package co.casterlabs.kawa.networking.packets;

import lombok.AllArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
public class PacketLineOpenRequest implements Packet {
    public String nonce;
    public String resourceId;

    @Override
    public Type getType() {
        return Type.LINE_OPEN_REQUEST;
    }

}
