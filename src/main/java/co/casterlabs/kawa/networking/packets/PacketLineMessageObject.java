package co.casterlabs.kawa.networking.packets;

import lombok.AllArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
public class PacketLineMessageObject implements Packet {
    public String lineId;
    public Object message;

    @Override
    public Type getType() {
        return Type.LINE_MESSAGE_OBJECT;
    }

}
