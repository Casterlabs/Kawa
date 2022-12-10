package co.casterlabs.kawa.networking;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

import co.casterlabs.kawa.networking.ActiveLine.ByteMessage;
import co.casterlabs.kawa.networking.ActiveLine.CloseMessage;
import co.casterlabs.kawa.networking.ActiveLine.ObjectMessage;

abstract class NetworkConnection {
    final List<ActiveLine> lines = new LinkedList<>();

    abstract void send(Object message);

    protected void handleClose(ActiveLine line) {
        line.listener.onClose(false);
        this.lines.remove(line);
    }

    /**
     * @return true if the message was handled. false if you should handle it.
     */
    public boolean handleMessage(Object message) {
        if (message instanceof CloseMessage) {
            CloseMessage packet = (CloseMessage) message;

            WeakReference<ActiveLine> $ref = ActiveLine.instances.get(packet.lineId);
            if ($ref != null) {
                ActiveLine line = $ref.get();
                if (line != null) {
                    handleClose(line);
                }
            }

            return true;
        }

        if (message instanceof ByteMessage) {
            ByteMessage packet = (ByteMessage) message;

            WeakReference<ActiveLine> $ref = ActiveLine.instances.get(packet.lineId);
            if ($ref != null) {
                ActiveLine line = $ref.get();
                if (line != null) {
                    line.listener.handleMessage(packet.type, packet.message);
                }
            }

            return true;
        }

        if (message instanceof ObjectMessage) {
            ObjectMessage packet = (ObjectMessage) message;

            WeakReference<ActiveLine> $ref = ActiveLine.instances.get(packet.lineId);
            if ($ref != null) {
                ActiveLine line = $ref.get();
                if (line != null) {
                    line.listener.handleMessage(packet.message);
                }
            }

            return true;
        }

        return false;
    }

}
