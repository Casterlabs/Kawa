package co.casterlabs.kawa.networking.packets;

import co.casterlabs.kawa.networking.KryoSerializable;
import lombok.AllArgsConstructor;

@KryoSerializable
@AllArgsConstructor
public class PacketLineOpenRejected {
    public String nonce;
    public String lineId;
}
