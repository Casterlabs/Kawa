package co.casterlabs.kawa.networking.packets;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PacketLineClose implements Packet {
    public String lineId;

    @Override
    public Type getType() {
        return Type.LINE_CLOSE;
    }

}
