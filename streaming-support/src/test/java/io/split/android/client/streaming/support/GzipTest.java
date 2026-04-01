package io.split.android.client.streaming.support;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class GzipTest {

    private Gzip gzip;

    @Before
    public void setUp() {
        gzip = new Gzip();
    }

    @Test
    public void decompress_validGzipData_returnsDecompressedBytes() throws IOException {
        // Arrange
        byte[] original = "Hello, World! This is a test message for gzip compression.".getBytes();
        byte[] compressed = compressWithGzip(original);

        // Act
        byte[] decompressed = gzip.decompress(compressed);

        // Assert
        assertNotNull(decompressed);
        assertArrayEquals(original, decompressed);
    }

    @Test
    public void decompress_emptyArray_returnsNull() {
        // Act
        byte[] result = gzip.decompress(new byte[0]);

        // Assert
        assertNull(result);
    }

    @Test
    public void decompress_nullInput_returnsNull() {
        // Act
        byte[] result = gzip.decompress(null);

        // Assert
        assertNull(result);
    }

    @Test
    public void decompress_invalidGzipData_returnsNull() {
        // Arrange
        byte[] invalidData = "This is not gzip compressed data".getBytes();

        // Act
        byte[] result = gzip.decompress(invalidData);

        // Assert
        assertNull(result);
    }

    @Test
    public void decompress_largeData_decompressesSuccessfully() throws IOException {
        // Arrange
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append("Line ").append(i).append(": Some test data\n");
        }
        byte[] original = sb.toString().getBytes();
        byte[] compressed = compressWithGzip(original);

        // Act
        byte[] decompressed = gzip.decompress(compressed);

        // Assert
        assertNotNull(decompressed);
        assertArrayEquals(original, decompressed);
    }

    private byte[] compressWithGzip(byte[] data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzipOut = new GZIPOutputStream(out);
        gzipOut.write(data);
        gzipOut.close();
        return out.toByteArray();
    }
}
