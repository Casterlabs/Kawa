package co.casterlabs.kawa.networking;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import co.casterlabs.kawa.networking.packets.PacketLineMessageByte;
import co.casterlabs.kawa.networking.packets.PacketLineClose;
import co.casterlabs.kawa.networking.packets.PacketLineMessageObject;
import lombok.Getter;
import lombok.NonNull;

public class Line {
    static final Map<String, WeakReference<Line>> instances = new HashMap<>();
    private final WeakReference<Line> $ref = new WeakReference<>(this);

    public final String id;
    public final NetworkConnection conn;
    public final Listener listener;

    @Getter
    boolean isOpen = true;

    Line(String id, NetworkConnection conn, @NonNull Listener listener) {
        this.id = id;
        this.conn = conn;
        this.listener = listener;
    }

    Line(NetworkConnection conn, @NonNull Listener listener) {
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
        assert this.isOpen : "Line is closed.";
        this.conn.send(new PacketLineMessageObject(this.id, message));
    }

    public void sendMessage(byte type, byte[] message) throws IOException {
        assert this.isOpen : "Line is closed.";
        this.conn.send(new PacketLineMessageByte(this.id, type, message));
    }

    public void close() {
        this.conn.send(new PacketLineClose(this.id));
        this.conn.handleClose(this, false);
    }

    public static interface Listener {

        default void onOpen(Line line) {}

        default void handleMessage(int type, byte[] trueMessage) {}

        default void handleMessage(Object trueMessage) {}

        default void onClose(boolean isNetworkDisconnect) {}

    }

}
