package io.split.android.client.network;

import static org.mockito.Mockito.mockStatic;

import android.util.Base64;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.nio.charset.StandardCharsets;

public class DefaultBase64EncoderTest {

    private DefaultBase64Encoder encoder;
    private MockedStatic<Base64> mockedBase64;

    @Before
    public void setUp() {
        encoder = new DefaultBase64Encoder();
        mockedBase64 = mockStatic(Base64.class);
    }

    @After
    public void tearDown() {
        mockedBase64.close();
    }

    @Test
    public void encodeStringUsesAndroidBase64() {
        String input = "test string";

        encoder.encode(input);

        mockedBase64.verify(() -> Base64.encodeToString(input.getBytes(), Base64.NO_WRAP));
    }

    @Test
    public void encodeByteArrayUsesAndroidBase64() {
        byte[] input = "test bytes".getBytes(StandardCharsets.UTF_8);

        encoder.encode(input);

        mockedBase64.verify(() -> Base64.encodeToString(input, Base64.NO_WRAP));
    }
}
