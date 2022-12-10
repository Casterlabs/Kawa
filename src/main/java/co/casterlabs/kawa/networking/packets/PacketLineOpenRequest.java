package co.casterlabs.kawa.networking.packets;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PacketLineOpenRequest implements Packet {
    public String nonce;
    public String resourceId;

    @Override
    public Type getType() {
        return Type.LINE_OPEN_REQUEST;
    }

}
