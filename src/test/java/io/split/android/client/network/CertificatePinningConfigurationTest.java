package io.split.android.client.network;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;

import io.split.android.client.utils.logger.Logger;

public class CertificatePinningConfigurationTest {

    private Base64Decoder mBase64Decoder;

    @Before
    public void setUp() {
        mBase64Decoder = mock(Base64Decoder.class);
        doAnswer(invocation -> {
            String base64 = invocation.getArgument(0);
            return base64.getBytes();
        }).when(mBase64Decoder).decode(anyString());
    }

    @Test
    public void listenerFromBuilderIsUsed() {
        CertificatePinningFailureListener failureListener = new CertificatePinningFailureListener() {
            @Override
            public void onCertificatePinningFailure(String host, List<X509Certificate> certificateChain) {
                // Do nothing
            }
        };

        CertificatePinningConfiguration config = CertificatePinningConfiguration.builder()
                .failureListener(failureListener)
                .build();

        assertSame(failureListener, config.getFailureListener());
    }

    @Test
    public void nullFailureListenerIsIgnored() {
        try (MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            CertificatePinningConfiguration config = CertificatePinningConfiguration.builder()
                    .failureListener(null)
                    .build();

            assertNull(config.getFailureListener());
            logger.verify(() -> Logger.w("Failure listener cannot be null"));
        }
    }

    @Test
    public void addValidSha256Pin() {
        CertificatePinningConfiguration config = CertificatePinningConfiguration.builder(mBase64Decoder)
                .addPin("host", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
                .build();

        Set<CertificatePin> pins = config.getPins().get("host");
        CertificatePin firstPin = pins.iterator().next();
        assertEquals(1, pins.size());
        assertEquals("sha256", firstPin.getAlgorithm());
        assertArrayEquals("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=".getBytes(), firstPin.getPin());
    }

    @Test
    public void addValidSha1Pin() {
        CertificatePinningConfiguration config = CertificatePinningConfiguration.builder(mBase64Decoder)
                .addPin("host", "sha1/AAAAAAAAAAAAAAAAAAAAAAAAAAA=")
                .build();

        Set<CertificatePin> pins = config.getPins().get("host");
        CertificatePin firstPin = pins.iterator().next();
        assertEquals(1, pins.size());
        assertEquals("sha1", firstPin.getAlgorithm());
        assertArrayEquals("AAAAAAAAAAAAAAAAAAAAAAAAAAA=".getBytes(), firstPin.getPin());
    }

    @Test
    public void addInvalidAlgorithmPin() {
        try (MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            CertificatePinningConfiguration config = CertificatePinningConfiguration.builder(mBase64Decoder)
                    .addPin("host", "md5/AAAAAAAAAAAAAAAAAAAAAAAAAAA=")
                    .build();

            Set<CertificatePin> pins = config.getPins().get("host");
            assertNull(pins);
            logger.verify(() -> Logger.e("Invalid algorithm. Must be sha256 or sha1. Ignoring entry for host host"));
        }
    }

    @Test
    public void addInvalidFormatPin() {
        try (MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            CertificatePinningConfiguration config = CertificatePinningConfiguration.builder(mBase64Decoder)
                    .addPin("my-host", "/AAAAAAAAAAAAAAAAAAAAAAAAAAA/")
                    .addPin("my-host", "AAAAAAAAAAAAAAAAAAAAAAAAAAA")
                    .build();

            Set<CertificatePin> pins = config.getPins().get("host");
            assertNull(pins);
            logger.verify(() -> Logger.e("Invalid algorithm. Must be sha256 or sha1. Ignoring entry for host my-host"));
            logger.verify(() -> Logger.e("Pin must be in the form \"[algorithm]/[hash]\". Ignoring entry for host my-host"));
        }
    }

    @Test
    public void addNullHost() {
        try (MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            CertificatePinningConfiguration config = CertificatePinningConfiguration.builder(mBase64Decoder)
                    .addPin(null, "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
                    .build();

            Set<CertificatePin> pins = config.getPins().get("host");
            assertNull(pins);
            logger.verify(() -> Logger.e("Host cannot be null or empty. Ignoring entry"));
        }
    }

    @Test
    public void addEmptyHost() {
        try (MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            CertificatePinningConfiguration config = CertificatePinningConfiguration.builder(mBase64Decoder)
                    .addPin("", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
                    .addPin(" ", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
                    .build();

            Set<CertificatePin> pins = config.getPins().get("host");
            assertNull(pins);
            logger.verify(() -> Logger.e("Host cannot be null or empty. Ignoring entry"), times(2));
        }
    }

    @Test
    public void addNullAndEmptyPin() {
        try (MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            CertificatePinningConfiguration config = CertificatePinningConfiguration.builder(mBase64Decoder)
                    .addPin("host", "")
                    .addPin("host", " ")
                    .addPin("host", null)
                    .build();

            Set<CertificatePin> pins = config.getPins().get("host");
            assertNull(pins);
            logger.verify(() -> Logger.e("Pin cannot be null or empty. Ignoring entry for host host"), times(3));
        }
    }

    @Test
    public void defaultPinsMapIsEmpty() {
        CertificatePinningConfiguration config = CertificatePinningConfiguration.builder(mBase64Decoder)
                .build();

        assertEquals(0, config.getPins().size());
    }
}
