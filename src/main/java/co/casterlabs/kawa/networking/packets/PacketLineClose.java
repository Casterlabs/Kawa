package co.casterlabs.kawa.networking.packets;

import lombok.AllArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
public class PacketLineClose implements Packet {
    public String lineId;

    @Override
    public Type getType() {
        return Type.LINE_CLOSE;
    }

}
