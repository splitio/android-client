package io.split.android.client.streaming.support;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ZlibTest {

    private Zlib zlib;

    @Before
    public void setUp() {
        zlib = new Zlib();
    }

    @Test
    public void decompress_validZlibData_returnsDecompressedBytes() {
        // Arrange
        byte[] original = "Hello, World! This is a test message for zlib compression.".getBytes();
        byte[] compressed = compressWithZlib(original);

        // Act
        byte[] decompressed = zlib.decompress(compressed);

        // Assert
        assertNotNull(decompressed);
        assertArrayEquals(original, decompressed);
    }

    @Test
    public void decompress_emptyArray_returnsNull() {
        // Act
        byte[] result = zlib.decompress(new byte[0]);

        // Assert
        assertNull(result);
    }

    @Test
    public void decompress_nullInput_returnsNull() {
        // Act
        byte[] result = zlib.decompress(null);

        // Assert
        assertNull(result);
    }

    @Test
    public void decompress_invalidZlibData_returnsNull() {
        // Arrange
        byte[] invalidData = "This is not zlib compressed data".getBytes();

        // Act
        byte[] result = zlib.decompress(invalidData);

        // Assert
        assertNull(result);
    }

    @Test
    public void decompress_largeData_decompressesSuccessfully() {
        // Arrange
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append("Line ").append(i).append(": Some test data\n");
        }
        byte[] original = sb.toString().getBytes();
        byte[] compressed = compressWithZlib(original);

        // Act
        byte[] decompressed = zlib.decompress(compressed);

        // Assert
        assertNotNull(decompressed);
        assertArrayEquals(original, decompressed);
    }

    private byte[] compressWithZlib(byte[] data) {
        Deflater deflater = new Deflater();
        deflater.setInput(data);
        deflater.finish();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        deflater.end();

        return outputStream.toByteArray();
    }
}
