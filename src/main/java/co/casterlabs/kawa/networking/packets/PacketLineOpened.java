package co.casterlabs.kawa.networking.packets;

import lombok.AllArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
public class PacketLineOpened implements Packet {
    public String nonce;
    public String lineId;

    @Override
    public Type getType() {
        return Type.LINE_OPENED;
    }

}
