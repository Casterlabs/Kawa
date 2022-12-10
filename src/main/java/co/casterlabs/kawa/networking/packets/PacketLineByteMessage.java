package co.casterlabs.kawa.networking.packets;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PacketLineByteMessage implements Packet {
    public String lineId;
    public int type;
    public byte[] message;

    @Override
    public Type getType() {
        return Type.LINE_BYTE_MESSAGE;
    }

}
