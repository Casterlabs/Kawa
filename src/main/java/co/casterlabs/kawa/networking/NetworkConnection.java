package co.casterlabs.kawa.networking;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import co.casterlabs.commons.async.PromiseWithHandles;
import co.casterlabs.kawa.networking.packets.Packet;
import co.casterlabs.kawa.networking.packets.PacketLineClose;
import co.casterlabs.kawa.networking.packets.PacketLineMessageByte;
import co.casterlabs.kawa.networking.packets.PacketLineMessageObject;
import co.casterlabs.kawa.networking.packets.PacketLineOpenRejected;
import co.casterlabs.kawa.networking.packets.PacketLineOpened;
import co.casterlabs.kawa.networking.packets.PacketLineOpenedAck;

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
    void handleMessage(Packet rawPacket) throws Throwable {
        switch (rawPacket.getType()) {
            // ------------------------
            // Client side
            // ------------------------

            case AUTHENTICATE_SUCCESS: {
                return; // Handled in KawaNetwork.
            }

            case LINE_OPENED: {
                PacketLineOpened packet = (PacketLineOpened) rawPacket;
                PromiseWithHandles<String> promise = this.lineOpenPromises.remove(packet.nonce);
                if (promise != null) {
                    promise.resolve(packet.lineId);
                }
                return;
            }

            case LINE_OPEN_REJECTED: {
                PacketLineOpenRejected packet = (PacketLineOpenRejected) rawPacket;
                PromiseWithHandles<String> promise = this.lineOpenPromises.remove(packet.nonce);
                if (promise != null) {
                    promise.reject(new IOException("Rejected."));
                }
                return;
            }

            // ------------------------
            // Server side
            // ------------------------

            case AUTHENTICATE_HANDSHAKE: {
                return; // Handled in KawaNetwork.
            }

            case LINE_OPEN_REQUEST: {
                return; // Handled in KawaNetwork.
            }

            case LINE_OPENED_ACK: {
                PacketLineOpenedAck packet = (PacketLineOpenedAck) rawPacket;
                PromiseWithHandles<String> promise = this.lineOpenPromises.remove(packet.nonce);
                if (promise != null) {
                    promise.resolve(packet.lineId);
                }
                return;
            }

            // ------------------------
            // Both sides
            // ------------------------

            case LINE_CLOSE: {
                PacketLineClose packet = (PacketLineClose) rawPacket;

                WeakReference<Line> $ref = Line.instances.get(packet.lineId);
                if ($ref != null) {
                    Line line = $ref.get();
                    if (line != null) {
                        handleClose(line, false);
                    }
                }
                return;
            }

            case LINE_MESSAGE_BYTE: {
                PacketLineMessageByte packet = (PacketLineMessageByte) rawPacket;

                WeakReference<Line> $ref = Line.instances.get(packet.lineId);
                if ($ref != null) {
                    Line line = $ref.get();
                    if (line != null) {
                        line.listener.handleMessage(packet.type, packet.message);
                    }
                }
                return;
            }

            case LINE_MESSAGE_OBJECT: {
                PacketLineMessageObject packet = (PacketLineMessageObject) rawPacket;

                WeakReference<Line> $ref = Line.instances.get(packet.lineId);
                if ($ref != null) {
                    Line line = $ref.get();
                    if (line != null) {
                        line.listener.handleMessage(packet.getTrueMessageObject());
                    }
                }
                return;
            }
        }
    }

}
