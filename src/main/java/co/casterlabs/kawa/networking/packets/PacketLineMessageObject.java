package co.casterlabs.kawa.networking.packets;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import co.casterlabs.rakurai.json.validation.JsonValidationException;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;

@ToString
@NoArgsConstructor
public class PacketLineMessageObject implements Packet {
    public String lineId;
    private String valueClass;
    private byte[] value;

    public PacketLineMessageObject(String lineId, Object message) {
        this.lineId = lineId;
        this.valueClass = message.getClass().getTypeName();
        this.value = GZipUtil.gzip(
            Rson.DEFAULT
                .toJson(message)
                .toString(false)
        );
    }

    public Object getTrueMessageObject() throws ClassNotFoundException, JsonValidationException, JsonParseException {
        Class<?> valueClass = Class.forName(this.valueClass);
        return Rson.DEFAULT.fromJson(
            GZipUtil.ungzip(this.value),
            valueClass
        );
    }

    @Override
    public Type getType() {
        return Type.LINE_MESSAGE_OBJECT;
    }

}

class GZipUtil {
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final int BUFFER_SIZE = 10240; // 10kb

    @SneakyThrows
    static String ungzip(byte[] bytes) {
        try (
            ByteArrayOutputStream sink = new ByteArrayOutputStream();
            GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(bytes))) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int read = 0;
            while ((read = gzip.read(buffer)) != -1) {
                sink.write(buffer, 0, read);
            }

            return new String(sink.toByteArray(), CHARSET);
        }
    }

    @SneakyThrows
    static byte[] gzip(String string) {
        try (
            ByteArrayOutputStream sink = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(sink)) {

            gzip.write(string.getBytes(CHARSET));
            gzip.close();

            return sink.toByteArray();
        }
    }

}
