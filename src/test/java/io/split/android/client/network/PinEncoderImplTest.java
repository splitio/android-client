package io.split.android.client.network;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.MockedStatic;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PinEncoderImplTest {

    @Test
    public void encodingSha1GetsMessageDigestInstance() throws NoSuchAlgorithmException {
        PinEncoderImpl pinEncoder = new PinEncoderImpl();
        try (MockedStatic<MessageDigest> messageDigest = mockStatic(MessageDigest.class)) {
            MessageDigest mockInstance = mock(MessageDigest.class);
            when(MessageDigest.getInstance("SHA-1")).thenReturn(mockInstance);
            pinEncoder.encodeCertPin("sha1", new byte[]{1, 2, 3});

            messageDigest.verify(() -> MessageDigest.getInstance("SHA-1"));
            verify(mockInstance).digest(eq(new byte[]{1, 2, 3}));
        }
    }

    @Test
    public void encodingSha256GetsMessageDigestInstance() throws NoSuchAlgorithmException {
        PinEncoderImpl pinEncoder = new PinEncoderImpl();
        try (MockedStatic<MessageDigest> messageDigest = mockStatic(MessageDigest.class)) {
            MessageDigest mockInstance = mock(MessageDigest.class);
            when(MessageDigest.getInstance("SHA-256")).thenReturn(mockInstance);
            pinEncoder.encodeCertPin("sha256", new byte[]{1, 2, 3});

            messageDigest.verify(() -> MessageDigest.getInstance("SHA-256"));
            verify(mockInstance).digest(eq(new byte[]{1, 2, 3}));
        }
    }

    @Test
    public void encodingUnknownAlgorithmReturnsEmptyArray() {
        PinEncoderImpl pinEncoder = new PinEncoderImpl();
        byte[] result = pinEncoder.encodeCertPin("unknown", new byte[]{1, 2, 3});
        assert result.length == 0;
    }
}
