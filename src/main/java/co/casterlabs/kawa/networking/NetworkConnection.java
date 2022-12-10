package co.casterlabs.kawa.networking;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import co.casterlabs.commons.async.PromiseWithHandles;
import co.casterlabs.kawa.networking.packets.Packet;
import co.casterlabs.kawa.networking.packets.PacketLineByteMessage;
import co.casterlabs.kawa.networking.packets.PacketLineClose;
import co.casterlabs.kawa.networking.packets.PacketLineObjectMessage;
import co.casterlabs.kawa.networking.packets.PacketLineOpenRejected;
import co.casterlabs.kawa.networking.packets.PacketLineOpened;
import co.casterlabs.kawa.networking.packets.PacketLineOpenedAck;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

abstract class NetworkConnection {
    final List<Line> activeLines = new LinkedList<>();

    // The promise's resolved value is the lineId.
    // All rejects are IOExceptions.
    final Map<String, PromiseWithHandles<String>> lineOpenPromises = new HashMap<>();

    abstract void send(Packet packet);

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
    public void handleMessage(Packet message) {
        // ------------------------
        // Client side
        // ------------------------

        if (message instanceof PacketLineOpened) {
            PacketLineOpened packet = (PacketLineOpened) message;
            PromiseWithHandles<String> promise = this.lineOpenPromises.remove(packet.nonce);
            if (promise != null) {
                promise.resolve(packet.lineId);
            }
            return;
        }

        if (message instanceof PacketLineOpenRejected) {
            PacketLineOpenRejected packet = (PacketLineOpenRejected) message;
            PromiseWithHandles<String> promise = this.lineOpenPromises.remove(packet.nonce);
            if (promise != null) {
                promise.reject(new IOException("Rejected."));
            }
            return;
        }

        // ------------------------
        // Server side
        // ------------------------

        // Handled in KawaNetwork.
//        if (message instanceof PacketLineOpenRequest) {
//        }

        if (message instanceof PacketLineOpenedAck) {
            PacketLineOpenedAck packet = (PacketLineOpenedAck) message;
            PromiseWithHandles<String> promise = this.lineOpenPromises.remove(packet.nonce);
            if (promise != null) {
                promise.resolve(packet.lineId);
            }
            return;
        }

        // ------------------------
        // Both sides
        // ------------------------

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

        FastLogger.logStatic("Unknown message: %s", message);
    }

}
