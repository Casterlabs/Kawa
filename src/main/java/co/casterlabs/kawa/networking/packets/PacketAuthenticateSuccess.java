package co.casterlabs.kawa.networking.packets;

import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@NoArgsConstructor
public class PacketAuthenticateSuccess implements Packet {

    @Override
    public Type getType() {
        return Type.AUTHENTICATE_SUCCESS;
    }

}
