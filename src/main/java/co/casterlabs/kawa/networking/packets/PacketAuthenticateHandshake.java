package co.casterlabs.kawa.networking.packets;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PacketAuthenticateHandshake implements Packet {
    public String password;

    @Override
    public Type getType() {
        return Type.AUTHENTICATE_HANDSHAKE;
    }

}
