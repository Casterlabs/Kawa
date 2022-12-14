package co.casterlabs.kawa.networking;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.UUID;

import co.casterlabs.kawa.networking.packets.PacketLineClose;
import co.casterlabs.kawa.networking.packets.PacketLineMessageByte;
import co.casterlabs.kawa.networking.packets.PacketLineMessageObject;
import lombok.Getter;
import lombok.NonNull;

public class Line {
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
        this.conn.lines.put(this.id, $ref);
    }

    Line(NetworkConnection conn, @NonNull Listener listener) {
        this(UUID.randomUUID().toString(), conn, listener);
    }

    @Override
    protected void finalize() {
        this.conn.lines.remove(this.id);
        $ref.clear();
    }

    public void sendMessage(Object message, boolean reliable) throws IOException {
        assert this.isOpen : "Line is closed.";
        this.conn.send(new PacketLineMessageObject(this.id, message), reliable);
    }

    public void sendMessage(byte type, byte[] message, boolean reliable) throws IOException {
        assert this.isOpen : "Line is closed.";
        this.conn.send(new PacketLineMessageByte(this.id, type, message), reliable);
    }

    public void close() {
        this.conn.send(new PacketLineClose(this.id), true);
        this.conn.handleClose(this, false);
    }

    public static interface Listener {

        default void onOpen(Line line) {}

        default void handleMessage(Line line, byte type, byte[] trueMessage) {}

        default void handleMessage(Line line, Object trueMessage) {}

        default void onClose(Line line, boolean isNetworkDisconnect) {}

    }

}
