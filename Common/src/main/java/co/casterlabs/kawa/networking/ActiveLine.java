package co.casterlabs.kawa.networking;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import co.casterlabs.kawa.networking.KawaNetwork.KryoSerializable;
import lombok.AllArgsConstructor;

public class ActiveLine {
    static final Map<String, WeakReference<ActiveLine>> instances = new HashMap<>();
    private final WeakReference<ActiveLine> $ref = new WeakReference<>(this);

    public final String id;
    public final NetworkConnection conn;
    public final ActiveLineListener listener;

    ActiveLine(String id, NetworkConnection conn, ActiveLineListener listener) {
        this.id = id;
        this.conn = conn;
        this.listener = listener;
    }

    ActiveLine(NetworkConnection conn, ActiveLineListener listener) {
        this(UUID.randomUUID().toString(), conn, listener);
    }

    @Override
    protected void finalize() {
        synchronized (instances) {
            instances.remove(this.id);
            $ref.clear();
        }
    }

    public void sendMessage(Object message) throws IOException {
        this.conn.send(new ObjectMessage(this.id, message));
    }

    public void sendMessage(byte type, byte[] message) throws IOException {
        this.conn.send(new ByteMessage(this.id, type, message));
    }

    public void close() {
        this.listener.onClose(false);
        this.conn.send(new CloseMessage(this.id));
        this.conn.lines.remove(this);
    }

    public interface ActiveLineListener {

        default void handleMessage(int type, byte[] trueMessage) {}

        default void handleMessage(Object trueMessage) {}

        default void onClose(boolean isNetworkDisconnect) {}

    }

    @KryoSerializable
    @AllArgsConstructor
    public static class CloseMessage {
        public String lineId;
    }

    @KryoSerializable
    @AllArgsConstructor
    public static class ObjectMessage {
        public String lineId;
        public Object message;
    }

    @KryoSerializable
    @AllArgsConstructor
    public static class ByteMessage {
        public String lineId;
        public int type;
        public byte[] message;
    }

}
