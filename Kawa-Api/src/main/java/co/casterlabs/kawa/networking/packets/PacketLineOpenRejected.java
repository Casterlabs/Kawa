package co.casterlabs.kawa.networking.packets;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PacketLineOpenRejected implements Packet {
    public String nonce;

    @Override
    public Type getType() {
        return Type.LINE_OPEN_REJECTED;
    }

}
