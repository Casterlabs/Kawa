package co.casterlabs.kawa.networking;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

import co.casterlabs.kawa.networking.packets.PacketLineByteMessage;
import co.casterlabs.kawa.networking.packets.PacketLineClose;
import co.casterlabs.kawa.networking.packets.PacketLineObjectMessage;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

abstract class NetworkConnection {
    final List<Line> activeLines = new LinkedList<>();

    abstract void send(Object message);

    void onEmptyLines() {
        // Override as needed.
    }

    protected void handleClose(Line line, boolean isNetworkDisconnect) {
        this.activeLines.remove(line);
        if (this.activeLines.isEmpty()) {
            this.onEmptyLines();
        }

        line.isOpen = false;
        line.listener.onClose(isNetworkDisconnect);
    }

    /**
     * @return true if the message was handled. false if you should handle it.
     */
    public void handleMessage(Object message) {
        if (message instanceof PacketLineClose) {
            PacketLineClose packet = (PacketLineClose) message;

            WeakReference<Line> $ref = Line.instances.get(packet.lineId);
            if ($ref != null) {
                Line line = $ref.get();
                if (line != null) {
                    handleClose(line, false);
                }
            }
            return;
        }

        if (message instanceof PacketLineByteMessage) {
            PacketLineByteMessage packet = (PacketLineByteMessage) message;

            WeakReference<Line> $ref = Line.instances.get(packet.lineId);
            if ($ref != null) {
                Line line = $ref.get();
                if (line != null) {
                    line.listener.handleMessage(packet.type, packet.message);
                }
            }
            return;
        }

        if (message instanceof PacketLineObjectMessage) {
            PacketLineObjectMessage packet = (PacketLineObjectMessage) message;

            WeakReference<Line> $ref = Line.instances.get(packet.lineId);
            if ($ref != null) {
                Line line = $ref.get();
                if (line != null) {
                    line.listener.handleMessage(packet.message);
                }
            }
            return;
        }

        // TODO line/resource handshaking.
        FastLogger.logStatic("Unknown message: %s", message);
    }

}
