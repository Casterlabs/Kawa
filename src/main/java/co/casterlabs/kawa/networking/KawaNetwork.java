package co.casterlabs.kawa.networking;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.reflections8.Reflections;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import co.casterlabs.commons.async.PromiseWithHandles;
import co.casterlabs.kawa.KawaResource;
import co.casterlabs.kawa.networking.packets.PacketAuthenticateHandshake;
import co.casterlabs.kawa.networking.packets.PacketAuthenticateSuccess;
import lombok.Getter;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class KawaNetwork {
    public static final int KAWA_PORT = 32977;

    private static final Map<String, NetworkConnection> clientConnections = new HashMap<>();
    private static final Set<Class<?>> classes = new HashSet<>();

    @Getter
    private static volatile int numberOfClients = 0;

    static {
        classes.addAll(
            new Reflections()
                .getTypesAnnotatedWith(KryoSerializable.class)
        );
        System.gc(); // Free memory, Reflections is HEAVY.
    }

    public static void setupKryo(Kryo kryo) {
        // We use the hashCode as the unique ID. Kryo internally uses 0-8 for other
        // things so we must also make sure we don't accidentally override those.
        for (Class<?> clazz : classes) {
            int id = (clazz.hashCode() & 0x7fffffff /*abs*/) + 10;
            kryo.register(clazz, id);
        }
    }

    public static synchronized NetworkConnection connectToServer(String address, String password) throws IOException {
        NetworkConnection nw_cached = clientConnections.get(address);

        if (nw_cached == null) {
            Client client = new Client();
            KawaNetwork.setupKryo(client.getKryo());

            PromiseWithHandles<Void> handshakePromise = new PromiseWithHandles<>();
            NetworkConnection nw = new NetworkConnection() {
                @Override
                void send(Object message) {
                    client.sendTCP(message);
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
                    nw.send(new PacketAuthenticateHandshake(password));
                }

                @Override
                public void received(Connection conn, Object message) {
                    if (message instanceof PacketAuthenticateSuccess) {
                        // Connection is ready to be used!
                        this.hasCompletedHandshake = true;
                        handshakePromise.resolve(null);
                        return;
                    }

                    nw.handleMessage(message);
                }

                @Override
                public void disconnected(Connection conn) {
                    if (this.hasCompletedHandshake) {
                        clientConnections.remove(address);
                    } else {
                        handshakePromise.reject(new IOException("Disconnected during handshake."));
                    }
                }
            });

            client.start();
            client.connect((int) TimeUnit.SECONDS.toMillis(2), address, KAWA_PORT);

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
                numberOfClients++;
            }

            @Override
            public void received(Connection conn, Object message) {
                if (message instanceof PacketAuthenticateHandshake) {
                    PacketAuthenticateHandshake auth = (PacketAuthenticateHandshake) message;
                    if (auth.password.equals(password)) {
                        // Connected.
                        this.connMap.put(conn, new NetworkConnection() {
                            @Override
                            void send(Object message) {
                                conn.sendTCP(message); // TODO UDP?
                            }
                        });
                        conn.sendTCP(new PacketAuthenticateSuccess());
                        return;
                    }
                    // Fallthrough.
                }

                NetworkConnection nw = this.connMap.get(conn);
                if (nw == null) {
                    FastLogger.logStatic("Client (%s) failed auth, disconnecting.", conn.getRemoteAddressTCP());
                    conn.close();
                    return;
                }

                nw.handleMessage(message);
            }

            @Override
            public void disconnected(Connection conn) {
                numberOfClients--;

                NetworkConnection nw = this.connMap.remove(conn);
                if (nw == null) return;

                new ArrayList<>(nw.activeLines)
                    .forEach((l) -> nw.handleClose(l, true));
            }

        });
        server.start();
        server.bind(KAWA_PORT);
    }

}
