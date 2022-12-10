package co.casterlabs.kawa.networking.packets;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PacketLineMessageByte implements Packet {
    public String lineId;
    public int type;
    public byte[] message;

    @Override
    public Type getType() {
        return Type.LINE_MESSAGE_BYTE;
    }

}
