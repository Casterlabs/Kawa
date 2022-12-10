package co.casterlabs.kawa.networking.packets;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PacketLineObjectMessage implements Packet {
    public String lineId;
    public Object message;

    @Override
    public Type getType() {
        return Type.LINE_OBJECT_MESSAGE;
    }

}
