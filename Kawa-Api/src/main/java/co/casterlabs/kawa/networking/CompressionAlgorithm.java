package co.casterlabs.kawa.networking;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum CompressionAlgorithm implements Compressor {
    NONE(new Dummy()),
    GZIP(new GZip()),
    ;

    private final Compressor implementation;

    @Override
    public byte[] compress(String str) throws IOException {
        return this.implementation.compress(str);
    }

    @Override
    public String uncompress(byte[] bytes) throws IOException {
        return this.implementation.uncompress(bytes);
    }

}

interface Compressor {

    public byte[] compress(String str) throws IOException;

    public String uncompress(byte[] bytes) throws IOException;

}

class Dummy implements Compressor {
    private static final Charset CHARSET = StandardCharsets.UTF_8;

    @Override
    public byte[] compress(String str) throws IOException {
        return str.getBytes(CHARSET);
    }

    @Override
    public String uncompress(byte[] bytes) throws IOException {
        return new String(bytes, CHARSET);
    }

}

class GZip implements Compressor {
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final int BUFFER_SIZE = 10240; // 10kb

    @Override
    public byte[] compress(String str) throws IOException {
        try (
            ByteArrayOutputStream sink = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(sink)) {

            gzip.write(str.getBytes(CHARSET));
            gzip.close();

            return sink.toByteArray();
        }
    }

    @Override
    public String uncompress(byte[] bytes) throws IOException {
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

}
