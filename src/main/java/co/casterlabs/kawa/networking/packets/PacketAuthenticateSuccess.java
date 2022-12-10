package co.casterlabs.kawa.networking.packets;

public class PacketAuthenticateSuccess implements Packet {

    @Override
    public Type getType() {
        return Type.AUTHENTICATE_SUCCESS;
    }

}
