package io.split.android.client.network;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * Custom Gson {@link TypeAdapter} for {@link CertificatePin} that uses
 * {@code "algo"} and {@code "pin"} as JSON keys instead of the raw field names.
 */
public class CertificatePinSerializer extends TypeAdapter<CertificatePin> {

    @Override
    public void write(JsonWriter out, CertificatePin src) throws IOException {
        out.beginObject();
        out.name("algo").value(src.getAlgorithm());
        out.name("pin");
        out.beginArray();
        for (byte b : src.getPin()) {
            out.value(b);
        }
        out.endArray();
        out.endObject();
    }

    @Override
    public CertificatePin read(JsonReader in) throws IOException {
        String algorithm = null;
        byte[] pin = null;

        in.beginObject();
        while (in.hasNext()) {
            String name = in.nextName();
            switch (name) {
                case "algo":
                    algorithm = in.nextString();
                    break;
                case "pin":
                    pin = readByteArray(in);
                    break;
                default:
                    in.skipValue();
                    break;
            }
        }
        in.endObject();

        return new CertificatePin(pin, algorithm);
    }

    private static byte[] readByteArray(JsonReader in) throws IOException {
        java.util.List<Byte> bytes = new java.util.ArrayList<>();
        in.beginArray();
        while (in.hasNext()) {
            bytes.add((byte) in.nextInt());
        }
        in.endArray();

        byte[] result = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
            result[i] = bytes.get(i);
        }
        return result;
    }
}
