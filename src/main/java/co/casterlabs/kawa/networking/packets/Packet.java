package co.casterlabs.kawa.networking.packets;

import lombok.AllArgsConstructor;

public interface Packet {

    public Type getType();

    @AllArgsConstructor
    public static enum Type {
        // @formatter:off
        AUTHENTICATE_HANDSHAKE(PacketAuthenticateHandshake.class),
        AUTHENTICATE_SUCCESS  (PacketAuthenticateSuccess.class),
        
        LINE_OPEN_REQUEST     (PacketLineOpenRequest.class),
        LINE_OPEN_REJECTED    (PacketLineOpenRejected.class),
        LINE_OPENED           (PacketLineOpened.class),
        LINE_OPENED_ACK       (PacketLineOpenedAck.class),
        LINE_CLOSE            (PacketLineClose.class),
        
        LINE_BYTE_MESSAGE     (PacketLineByteMessage.class),
        LINE_OBJECT_MESSAGE   (PacketLineObjectMessage.class),
        
        // @formatter:on
        ;

        public final Class<?> clazz;

    }

}
