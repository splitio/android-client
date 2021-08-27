package io.split.android.client.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import io.split.android.client.service.ServiceConstants;

public class Gzip implements CompressionUtil {

    @Override
    public byte[] decompress(byte[] input) {
        if (input == null || input.length == 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(input);
        GZIPInputStream gzipIn = null;
        try {
            gzipIn = new GZIPInputStream(in);
            byte[] buffer = new byte[ServiceConstants.MY_SEGMENT_V2_DATA_SIZE];
            int byteCount;
            while ((byteCount = gzipIn.read(buffer)) >= 0) {
                out.write(buffer, 0, byteCount);
            }
            return out.toByteArray();
        } catch (IOException e) {
            Logger.e("Gzip format error: " + e.getLocalizedMessage());
        } catch (Exception e) {
            Logger.e("Error decompressing gzip: " + e.getLocalizedMessage());
        } finally {
            close(out);
            close(gzipIn);
            close(in);
        }
        return null;
    }

    void close(Closeable component) {
        try {
            component.close();
        } catch (Exception e) {
            Logger.e("Gzip error closing component: " + e.getLocalizedMessage());
        }
    }
}
