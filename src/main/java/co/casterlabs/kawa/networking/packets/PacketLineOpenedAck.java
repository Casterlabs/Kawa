package co.casterlabs.kawa.networking.packets;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PacketLineOpenedAck implements Packet {
    public String nonce;
    public String lineId;

    @Override
    public Type getType() {
        return Type.LINE_OPENED_ACK;
    }

}
