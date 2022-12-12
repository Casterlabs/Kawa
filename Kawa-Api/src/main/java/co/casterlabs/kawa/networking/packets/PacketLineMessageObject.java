package co.casterlabs.kawa.networking.packets;

import java.io.IOException;

import co.casterlabs.kawa.networking.CompressionAlgorithm;
import co.casterlabs.rakurai.json.Rson;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@NoArgsConstructor
public class PacketLineMessageObject implements Packet {
    public String lineId;

    private byte[] value;
    private String valueClass;
    private CompressionAlgorithm valueCompression;

    public PacketLineMessageObject(String lineId, Object message) throws IOException {
        String rawJson = Rson.DEFAULT
            .toJson(message)
            .toString(false);

        this.lineId = lineId;
        this.valueClass = message.getClass().getTypeName();

        // "Probe" the content to figure out the best compression algorithm. Json is
        // pretty repetitive in it's syntax and has a limited range of characters,
        // making it perfect for this.
        // TODO There's probably a fast & analytical way to do this.
        this.valueCompression = rawJson.length() > 150 ? //
            CompressionAlgorithm.GZIP : CompressionAlgorithm.NONE;

        this.value = this.valueCompression.compress(rawJson);
    }

    public Object getTrueMessageObject() throws ClassNotFoundException, IOException {
        Class<?> valueClass = Class.forName(this.valueClass);
        String rawJson = this.valueCompression.uncompress(this.value);

        return Rson.DEFAULT.fromJson(
            rawJson,
            valueClass
        );
    }

    @Override
    public Type getType() {
        return Type.LINE_MESSAGE_OBJECT;
    }

}
