package co.casterlabs.kawa.networking;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import co.casterlabs.commons.async.AsyncTask;
import co.casterlabs.commons.async.PromiseWithHandles;
import co.casterlabs.kawa.Kawa;
import co.casterlabs.kawa.KawaResource;
import co.casterlabs.kawa.networking.packets.Packet;
import co.casterlabs.kawa.networking.packets.PacketAuthenticateHandshake;
import co.casterlabs.kawa.networking.packets.PacketAuthenticateSuccess;
import co.casterlabs.kawa.networking.packets.PacketLineOpenRejected;
import co.casterlabs.kawa.networking.packets.PacketLineOpenRequest;
import co.casterlabs.kawa.networking.packets.PacketLineOpened;
import co.casterlabs.kawa.networking.packets.PacketLineOpenedAck;
import lombok.Getter;

public class KawaNetwork {
    public static final int KAWA_PORT_TCP = 32977;
    public static final int KAWA_PORT_UDP = 32978;

    @Getter
    private static volatile int numberOfClients = 0;

    private static final Map<String, NetworkConnection> clientConnections = new HashMap<>();
    private static final List<Class<?>> kryoTypes = new LinkedList<>();

    static {
        for (Packet.Type type : Packet.Type.values()) {
            kryoTypes.add(type.clazz);
        }
        kryoTypes.add(CompressionAlgorithm.class);
        kryoTypes.add(byte[].class);
    }

    public static void setupKryo(Kryo kryo) {
        // We use the hashCode as the unique ID. Kryo internally uses 0-8 for other
        // things so we must also make sure we don't accidentally override those.
        for (Class<?> clazz : kryoTypes) {
            int id = (clazz.hashCode() & 0x7fffffff /*abs*/) + 10;
            kryo.register(clazz, id);
        }
    }

    /**
     * @throws null if the open failed/was rejected.
     */
    public static synchronized Line openLine(String address, String password, String resourceId, Line.Listener listener) throws IOException {
        NetworkConnection nw = connectToServer(address, password);
        String nonce = UUID.randomUUID().toString();

        // Register a promise which we'll use to wait for the value.
        PromiseWithHandles<String> openPromise = new PromiseWithHandles<>();
        openPromise.except((t) -> {
        });
        nw.lineOpenPromises.put(nonce, openPromise);

        // Ask for a line to the resource.
        nw.send(new PacketLineOpenRequest(nonce, resourceId), true);

        // Wait for either the line or a rejection.
        String lineId;

        try {
            lineId = openPromise.await();
        } catch (Throwable t) {
            throw (IOException) t;
        }

        Line line = new Line(lineId, nw, listener);

        // Tell the server that we've allocated the resources and are now ready.
        nw.send(new PacketLineOpenedAck(nonce, resourceId), true);

        // We're open!
        line.listener.onOpen(line);
        return line;
    }

    private static synchronized NetworkConnection connectToServer(String address, String password) throws IOException {
        NetworkConnection nw_cached = clientConnections.get(address);

        if (nw_cached == null) {
            Client client = new Client();
            KawaNetwork.setupKryo(client.getKryo());

            PromiseWithHandles<Void> handshakePromise = new PromiseWithHandles<>();
            handshakePromise.except((t) -> {
            });
            NetworkConnection nw = new NetworkConnection() {
                @Override
                void send(Packet packet, boolean reliable) {
                    Kawa.LOGGER.trace("[Network Client] Send: %s", packet);
                    if (reliable) {
                        client.sendTCP(packet);
                    } else {
                        client.sendUDP(packet);
                    }
                }

                @Override
                void onEmptyLines() {
                    // We've gone idle, teardown the connection.
                    client.close();
                }
            };
            client.addListener(new Listener() {
                private boolean hasCompletedHandshake = false;

                @Override
                public void connected(Connection conn) {
                    Kawa.LOGGER.trace("[Network Client] Connected");
                    nw.send(new PacketAuthenticateHandshake(password), true);
                }

                @Override
                public void received(Connection conn, Object message) {
                    Kawa.LOGGER.trace("[Network Client] Recv: %s", message);

                    if (message instanceof PacketAuthenticateSuccess) {
                        // Connection is ready to be used!
                        this.hasCompletedHandshake = true;
                        handshakePromise.resolve(null);
                        return;
                    }

                    if (message instanceof FrameworkMessage.KeepAlive) return;

                    try {
                        nw.handleMessage((Packet) message);
                    } catch (Throwable t) {
                        Kawa.LOGGER.severe("[Network Client] Uncaught:\n%s", t);
                    }
                }

                @Override
                public void disconnected(Connection conn) {
                    Kawa.LOGGER.trace("[Network Client] Disconnected");
                    if (this.hasCompletedHandshake) {
                        NetworkConnection nw = clientConnections.remove(address);
                        if (nw == null) return;

                        nw.lineOpenPromises
                            .values()
                            .forEach((p) -> p.reject(new IOException("Connection was closed.")));

                        new ArrayList<>(nw.lines.values())
                            .forEach(($ref) -> {
                                Line line = $ref.get();
                                if (line != null) {
                                    nw.handleClose(line, true);
                                }
                            });
                    } else {
                        handshakePromise.reject(new IOException("Disconnected during handshake."));
                    }
                }
            });

            client.start();
            client.connect((int) TimeUnit.SECONDS.toMillis(2), address, KAWA_PORT_TCP, KAWA_PORT_UDP);

            try {
                handshakePromise.await();
            } catch (Throwable t) {
                throw (IOException) t;
            }

            // Success!
            clientConnections.put(address, nw);
            return nw;
        } else {
            return nw_cached;
        }
    }

    public static void startServer(String thisAddress, String password, Map<String, KawaResource> resourceProviders) throws IOException {
        Server server = new Server();
        KawaNetwork.setupKryo(server.getKryo());
        server.addListener(new Listener() {
            private Map<Connection, NetworkConnection> connMap = new HashMap<>();

            @Override
            public void connected(Connection conn) {
                Kawa.LOGGER.trace("[Network Server] Client connected");
                numberOfClients++;
            }

            @Override
            public void received(Connection conn, Object message) {
                Kawa.LOGGER.trace("[Network Server] Recv: %s", message);

                if (message instanceof PacketAuthenticateHandshake) {
                    PacketAuthenticateHandshake auth = (PacketAuthenticateHandshake) message;
                    if (auth.password.equals(password)) {
                        // Connected.
                        NetworkConnection nw = new NetworkConnection() {
                            @Override
                            void send(Packet packet, boolean reliable) {
                                Kawa.LOGGER.trace("[Network Server] Send: %s", packet);
                                if (reliable) {
                                    conn.sendTCP(packet);
                                } else {
                                    conn.sendUDP(packet);
                                }
                            }
                        };

                        this.connMap.put(conn, nw);
                        nw.send(new PacketAuthenticateSuccess(), true);
                        return;
                    }
                    // Fallthrough.
                }

                NetworkConnection nw = this.connMap.get(conn);
                if (nw == null) {
                    Kawa.LOGGER.warn("[Network Server] Client (%s) failed auth, disconnecting.", conn.getRemoteAddressTCP());
                    conn.close();
                    return;
                }

                if (message instanceof PacketLineOpenRequest) {
                    PacketLineOpenRequest packet = (PacketLineOpenRequest) message;

                    KawaResource resourceProvider = resourceProviders.get(packet.resourceId);
                    if (resourceProvider == null) {
                        nw.send(new PacketLineOpenRejected(packet.nonce), true);
                        return;
                    }

                    Line.Listener lineListener = resourceProvider.accept(packet.resourceId);
                    if (lineListener == null) {
                        nw.send(new PacketLineOpenRejected(packet.nonce), true);
                        return;
                    }

                    // Register our connect promise.
                    PromiseWithHandles<String> ackPromise = new PromiseWithHandles<>();
                    ackPromise.except((t) -> {
                    });
                    nw.lineOpenPromises.put(packet.nonce, ackPromise);

                    AsyncTask.create(() -> {
                        try {
                            TimeUnit.SECONDS.sleep(2);
                        } catch (InterruptedException e) {}

                        if (ackPromise.hasCompleted()) return;

                        // We timed out, reject.
                        nw.send(new PacketLineOpenRejected(packet.nonce), true);
                        nw.lineOpenPromises.remove(packet.nonce);
                    });

                    // Accept the request.
                    Line line = new Line(nw, lineListener);
                    ackPromise.then((_unused) -> {
                        line.listener.onOpen(line);
                    });
                    ackPromise.except((_unused) -> {
                        line.close();
                    });

                    nw.send(new PacketLineOpened(packet.nonce, line.id), true); // Notify client of acceptance.
                    return;
                }

                if (message instanceof FrameworkMessage.KeepAlive) return;

                try {
                    nw.handleMessage((Packet) message);
                } catch (Throwable t) {
                    Kawa.LOGGER.severe("[Network Server] Uncaught:\n%s", t);
                }
            }

            @Override
            public void disconnected(Connection conn) {
                Kawa.LOGGER.trace("[Network Server] Client disconnected");
                numberOfClients--;

                NetworkConnection nw = this.connMap.remove(conn);
                if (nw == null) return;

                new ArrayList<>(nw.lines.values())
                    .forEach(($ref) -> {
                        Line line = $ref.get();
                        if (line != null) {
                            nw.handleClose(line, true);
                        }
                    });
            }

        });
        server.start();
        server.bind(KAWA_PORT_TCP, KAWA_PORT_UDP);
    }

}
