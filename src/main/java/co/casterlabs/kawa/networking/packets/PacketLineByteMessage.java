package co.casterlabs.kawa.networking.packets;

import co.casterlabs.kawa.networking.KryoSerializable;
import lombok.AllArgsConstructor;

@KryoSerializable
@AllArgsConstructor
public class PacketLineByteMessage {
    public String lineId;
    public int type;
    public byte[] message;
}