package io.split.android.client.network;

import static org.mockito.Mockito.mockStatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.nio.charset.StandardCharsets;

import io.split.android.client.utils.Base64Util;

public class DefaultBase64EncoderTest {
    
    private DefaultBase64Encoder encoder;
    private MockedStatic<Base64Util> mockedBase64Util;
    
    @Before
    public void setUp() {
        encoder = new DefaultBase64Encoder();
        mockedBase64Util = mockStatic(Base64Util.class);
    }
    
    @After
    public void tearDown() {
        mockedBase64Util.close();
    }
    
    @Test
    public void encodeStringUsesBase64Util() {
        String input = "test string";
        
        encoder.encode(input);
        
        mockedBase64Util.verify(() -> Base64Util.encode(input));
    }
    
    @Test
    public void encodeByteArrayUsesBase64Util() {
        byte[] input = "test bytes".getBytes(StandardCharsets.UTF_8);
        
        encoder.encode(input);
        
        mockedBase64Util.verify(() -> Base64Util.encode(input));
    }
}
