package helper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

import io.split.android.client.service.ServiceConstants;
import io.split.android.client.utils.logger.Logger;

public class CompressionHelper {
    public byte[] compressZlib(byte[] input) {
        byte[] output = new byte[ServiceConstants.MY_SEGMENT_V2_DATA_SIZE];
        Deflater deflater = new Deflater();
        deflater.setInput(input);
        deflater.finish();
        int compressedDataLength = deflater.deflate(output);
        deflater.end();
        return output;
    }

    public byte[] compressGzip(byte[] input) {

        if (input == null || input.length == 0) {
            return null;
        }
        GZIPOutputStream gzipOut = null;
        ByteArrayOutputStream out = null;
        ByteArrayInputStream in = null;
        try {
            out = new ByteArrayOutputStream(input.length);
            gzipOut = new GZIPOutputStream(out);
            byte[] output = new byte[ServiceConstants.MY_SEGMENT_V2_DATA_SIZE];
            in = new ByteArrayInputStream(input);
            int byteCount;
            while ((byteCount = in.read(output)) >= 0) {
                gzipOut.write(output, 0, byteCount);
            }
            gzipOut.finish();
            return out.toByteArray();
        } catch (IOException e) {
            System.out.println("DataFormatException error: " + e.getLocalizedMessage());
        } catch (Exception e) {
            System.out.println("Error compressing: " + e.getLocalizedMessage());
        } finally {
            close(gzipOut);
            close(out);
            close(in);
        }
        return null;
    }

    void close(Closeable component) {
        try {
            component.close();
        } catch (Exception e) {
            Logger.e("Error closing component: " + e.getLocalizedMessage());
        }
    }
}
