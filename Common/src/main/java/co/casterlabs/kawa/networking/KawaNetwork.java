package co.casterlabs.kawa.networking;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.reflections8.Reflections;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import lombok.AllArgsConstructor;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class KawaNetwork {
    private static final Set<Class<?>> classes = new HashSet<>();

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

    public static Listener listenKryo(String password) {
        return new Listener() {
            private Map<Connection, NetworkConnection> connMap = new HashMap<>();

            @Override
            public void received(Connection conn, Object message) {
                if (message instanceof AuthenticateHandshakeMessage) {
                    AuthenticateHandshakeMessage auth = (AuthenticateHandshakeMessage) message;
                    if (auth.password.equals(password)) {
                        // Connected.
                        this.connMap.put(conn, new NetworkConnection() {
                            @Override
                            void send(Object message) {
                                conn.sendTCP(message); // TODO UDP?
                            }
                        });
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

                if (nw.handleMessage(message)) {
                    return;// Already handled.
                }

                // TODO line/resource handshaking.
                FastLogger.logStatic("Unknown message: %s", message);
            }

            @Override
            public void disconnected(Connection conn) {
                NetworkConnection nw = this.connMap.remove(conn);
                if (nw == null) return;

                new ArrayList<>(nw.lines)
                    .forEach(nw::handleClose);
            }

        };
    }

    @Retention(RUNTIME)
    @Target(TYPE)
    public static @interface KryoSerializable {
    }

    @KryoSerializable
    @AllArgsConstructor
    public static class AuthenticateHandshakeMessage {
        public String password;
    }

}
