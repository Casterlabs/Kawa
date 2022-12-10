package co.casterlabs.kawa.networking;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

import co.casterlabs.kawa.networking.packets.PacketLineByteMessage;
import co.casterlabs.kawa.networking.packets.PacketLineClose;
import co.casterlabs.kawa.networking.packets.PacketLineObjectMessage;

abstract class NetworkConnection {
    final List<Line> lines = new LinkedList<>();

    abstract void send(Object message);

    protected void handleClose(Line line, boolean isNetworkDisconnect) {
        this.lines.remove(line);
        line.isOpen = false;
        line.listener.onClose(isNetworkDisconnect);
    }

    /**
     * @return true if the message was handled. false if you should handle it.
     */
    public boolean handleMessage(Object message) {
        if (message instanceof PacketLineClose) {
            PacketLineClose packet = (PacketLineClose) message;

            WeakReference<Line> $ref = Line.instances.get(packet.lineId);
            if ($ref != null) {
                Line line = $ref.get();
                if (line != null) {
                    handleClose(line, false);
                }
            }

            return true;
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

            return true;
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

            return true;
        }

        return false;
    }

}
