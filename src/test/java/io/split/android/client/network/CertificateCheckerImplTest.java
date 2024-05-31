package io.split.android.client.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.net.URL;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;

import io.split.android.client.utils.logger.Logger;

public class CertificateCheckerImplTest {

    private CertificatePinningFailureListener mFailureListener;
    private ChainCleaner mChainCleaner;
    private Base64Encoder mBase64Encoder;
    private PinEncoder mPinEncoder;
    private CertificateCheckerImpl mChecker;
    private URL mMockUrl;
    private HttpsURLConnection mMockConnection;

    @Before
    public void setUp() {
        mFailureListener = mock(CertificatePinningFailureListener.class);
        mChainCleaner = mock(ChainCleaner.class);
        mBase64Encoder = mock(Base64Encoder.class);
        mPinEncoder = mock(PinEncoder.class);

        mMockConnection = mock(HttpsURLConnection.class);
        mMockUrl = mock(URL.class);
        when(mMockUrl.getHost()).thenReturn("my-url.com");
        when(mMockConnection.getURL()).thenReturn(mMockUrl);
    }

    @Test
    public void nullPinsDoesNotInteractWithComponents() throws SSLPeerUnverifiedException {
        mChecker = getChecker(null);
        when(mMockUrl.getHost()).thenReturn("my-url.com");
        when(mMockConnection.getURL()).thenReturn(mMockUrl);

        mChecker.checkPins(mMockConnection);

        verify(mFailureListener, times(0)).onCertificatePinningFailure(any(), any());
        verify(mChainCleaner, times(0)).clean(any(), any());
        verify(mBase64Encoder, times(0)).encode((byte[]) any());
        verify(mPinEncoder, times(0)).encodeCertPin(any(), any());
    }

    @Test
    public void emptyPinsDoesNotInteractWithComponents() throws SSLPeerUnverifiedException {
        mChecker = getChecker(null);

        mChecker.checkPins(mMockConnection);

        verify(mFailureListener, times(0)).onCertificatePinningFailure(any(), any());
        verify(mChainCleaner, times(0)).clean(any(), any());
        verify(mBase64Encoder, times(0)).encode((byte[]) any());
        verify(mPinEncoder, times(0)).encodeCertPin(any(), any());
    }

    @Test
    public void noMatchingPinsDoesNotInteractWithComponents() throws SSLPeerUnverifiedException {
        mChecker = getChecker(Collections.singletonMap("not-my-host.com", Collections.singletonList(new CertificatePin(new byte[]{0, 1, 2, 3}, "sha256"))));

        mChecker.checkPins(mMockConnection);

        verify(mFailureListener, times(0)).onCertificatePinningFailure(any(), any());
        verify(mChainCleaner, times(0)).clean(any(), any());
        verify(mBase64Encoder, times(0)).encode((byte[]) any());
        verify(mPinEncoder, times(0)).encodeCertPin(any(), any());
    }

    @Test
    public void noMatchingPinsLogsDebugMessage() throws SSLPeerUnverifiedException {
        mChecker = getChecker(Collections.singletonMap("not-my-host.com", Collections.singletonList(new CertificatePin(new byte[]{0, 1, 2, 3}, "sha256"))));

        try (MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            mChecker.checkPins(mMockConnection);

            logger.verify(() -> Logger.d("No certificate pins configured for my-url.com. Skipping pinning verification."));
        }
    }

    @Test
    public void matchingPinDoesNotInteractWithFailureListener() throws SSLPeerUnverifiedException {
        PublicKey mockedPublicKey = mock(PublicKey.class);
        when(mockedPublicKey.getEncoded()).thenReturn(new byte[]{0, 1, 2, 3});

        X509Certificate mockedX509Cert = mock(X509Certificate.class);
        when(mockedX509Cert.getPublicKey()).thenReturn(mockedPublicKey);

        CertificatePin pin = new CertificatePin(new byte[]{0, 1, 2, 3}, "sha256");

        mChecker = getChecker(Collections.singletonMap("my-url.com", Collections.singletonList(pin)));
        when(mChainCleaner.clean(any(), any())).thenReturn(Collections.singletonList(mockedX509Cert));
        when(mPinEncoder.encodeCertPin(any(), any())).thenReturn(new byte[]{0, 1, 2, 3});

        mChecker.checkPins(mMockConnection);

        verify(mFailureListener, times(0)).onCertificatePinningFailure(any(), any());
    }

    @Test
    public void pinEncoderIsCalledForThePublicKeyOfEachCleanCertificateWhenMatchingHost() {
        PublicKey mockedPublicKey = mock(PublicKey.class);
        byte[] bytes1 = {0, 1, 2, 3};
        when(mockedPublicKey.getEncoded()).thenReturn(bytes1);
        PublicKey mockedPublicKey2 = mock(PublicKey.class);
        byte[] bytes2 = {4, 5, 6, 7};
        when(mockedPublicKey2.getEncoded()).thenReturn(bytes2);

        Principal mockedPrincipal = mock(Principal.class);
        when(mockedPrincipal.getName()).thenReturn("CN=cert1");
        Principal mockedPrincipal2 = mock(Principal.class);
        when(mockedPrincipal2.getName()).thenReturn("CN=cert2");

        X509Certificate mockedX509Cert = mock(X509Certificate.class);
        when(mockedX509Cert.getPublicKey()).thenReturn(mockedPublicKey);
        when(mockedX509Cert.getSubjectDN()).thenReturn(mockedPrincipal);
        X509Certificate mockedX509Cert2 = mock(X509Certificate.class);
        when(mockedX509Cert2.getPublicKey()).thenReturn(mockedPublicKey2);
        when(mockedX509Cert2.getSubjectDN()).thenReturn(mockedPrincipal2);

        CertificatePin pin = new CertificatePin(bytes2, "sha1");
        when(mChainCleaner.clean(any(), any())).thenReturn(Arrays.asList(mockedX509Cert, mockedX509Cert2));
        when(mPinEncoder.encodeCertPin("sha1", bytes1)).thenReturn(bytes1);
        when(mPinEncoder.encodeCertPin("sha1", bytes2)).thenReturn(bytes2);

        mChecker = getChecker(Collections.singletonMap("my-url.com", Collections.singletonList(pin)));

        try {
            mChecker.checkPins(mMockConnection);
        } catch (SSLPeerUnverifiedException e) {
            // ignore
        }

        verify(mPinEncoder).encodeCertPin("sha1", bytes1);
        verify(mPinEncoder).encodeCertPin("sha1", bytes2);
    }

    @Test
    public void failureListenerIsCalledWhenThereAreHostsButNoMatchingCerts() {
        PublicKey mockedPublicKey = mock(PublicKey.class);
        byte[] bytes1 = {0, 1, 2, 3};
        when(mockedPublicKey.getEncoded()).thenReturn(bytes1);
        PublicKey mockedPublicKey2 = mock(PublicKey.class);
        byte[] bytes2 = {4, 5, 6, 7};
        when(mockedPublicKey2.getEncoded()).thenReturn(bytes2);

        Principal mockedPrincipal = mock(Principal.class);
        when(mockedPrincipal.getName()).thenReturn("CN=cert1");
        Principal mockedPrincipal2 = mock(Principal.class);
        when(mockedPrincipal2.getName()).thenReturn("CN=cert2");

        X509Certificate mockedX509Cert = mock(X509Certificate.class);
        when(mockedX509Cert.getPublicKey()).thenReturn(mockedPublicKey);
        when(mockedX509Cert.getSubjectDN()).thenReturn(mockedPrincipal);
        X509Certificate mockedX509Cert2 = mock(X509Certificate.class);
        when(mockedX509Cert2.getPublicKey()).thenReturn(mockedPublicKey2);
        when(mockedX509Cert2.getSubjectDN()).thenReturn(mockedPrincipal2);

        CertificatePin pin = new CertificatePin(new byte[]{1, 2, 2, 3}, "sha256");
        when(mChainCleaner.clean(any(), any())).thenReturn(Arrays.asList(mockedX509Cert, mockedX509Cert2));
        when(mPinEncoder.encodeCertPin(any(), any())).thenReturn(new byte[]{3, 2, 2, 3});

        mChecker = getChecker(Collections.singletonMap("my-url.com", Collections.singletonList(pin)));

        try {
            mChecker.checkPins(mMockConnection);
        } catch (SSLPeerUnverifiedException e) {
            // ignore
        }

        verify(mFailureListener).onCertificatePinningFailure(any(), any());
    }

    @Test
    public void sslPeerUnverifiedExceptionIsThrownWhenThereAreHostsButNoMatchingCerts() {
        PublicKey mockedPublicKey = mock(PublicKey.class);
        byte[] bytes1 = {0, 1, 2, 3};
        when(mockedPublicKey.getEncoded()).thenReturn(bytes1);

        Principal mockedPrincipal = mock(Principal.class);
        when(mockedPrincipal.getName()).thenReturn("CN=cert1");

        X509Certificate mockedX509Cert = mock(X509Certificate.class);
        when(mockedX509Cert.getPublicKey()).thenReturn(mockedPublicKey);
        when(mockedX509Cert.getSubjectDN()).thenReturn(mockedPrincipal);

        CertificatePin pin = new CertificatePin(new byte[]{1, 2, 2, 3}, "sha256");
        when(mChainCleaner.clean(any(), any())).thenReturn(Collections.singletonList(mockedX509Cert));
        when(mPinEncoder.encodeCertPin(any(), any())).thenReturn(new byte[]{3, 2, 2, 3});

        mChecker = getChecker(Collections.singletonMap("my-url.com", Collections.singletonList(pin)));

        try {
            mChecker.checkPins(mMockConnection);
        } catch (SSLPeerUnverifiedException e) {
            return;
        }
        fail();
    }

    @Test
    public void failingToCleanChainThrowsSSLPeerUnverifiedException() {
        when(mChainCleaner.clean(any(), any())).thenThrow(new RuntimeException("Error cleaning chain"));

        mChecker = getChecker(Collections.singletonMap("my-url.com", Collections.singletonList(new CertificatePin(new byte[]{0, 1, 2, 3}, "sha256"))));

        try {
            mChecker.checkPins(mMockConnection);
        } catch (SSLPeerUnverifiedException e) {
            return;
        }
        fail();
    }

    @Test
    public void fullChainHashesAreInExceptionMessageWhenThereAreHostsButNoMatchingCerts() {
        PublicKey mockedPublicKey = mock(PublicKey.class);
        byte[] bytes1 = {0, 1, 2, 3};
        when(mockedPublicKey.getEncoded()).thenReturn(bytes1);
        PublicKey mockedPublicKey2 = mock(PublicKey.class);
        byte[] bytes2 = {4, 5, 6, 7};
        when(mockedPublicKey2.getEncoded()).thenReturn(bytes2);

        Principal mockedPrincipal = mock(Principal.class);
        when(mockedPrincipal.getName()).thenReturn("CN=cert1");
        Principal mockedPrincipal2 = mock(Principal.class);
        when(mockedPrincipal2.getName()).thenReturn("CN=cert2");

        X509Certificate mockedX509Cert = mock(X509Certificate.class);
        when(mockedX509Cert.getPublicKey()).thenReturn(mockedPublicKey);
        when(mockedX509Cert.getSubjectDN()).thenReturn(mockedPrincipal);
        X509Certificate mockedX509Cert2 = mock(X509Certificate.class);
        when(mockedX509Cert2.getPublicKey()).thenReturn(mockedPublicKey2);
        when(mockedX509Cert2.getSubjectDN()).thenReturn(mockedPrincipal2);

        CertificatePin pin = new CertificatePin(new byte[]{1, 2, 2, 3}, "sha256");
        when(mChainCleaner.clean(any(), any())).thenReturn(Arrays.asList(mockedX509Cert, mockedX509Cert2));
        when(mPinEncoder.encodeCertPin(eq("sha256"), eq(new byte[]{1, 2, 2, 3}))).thenReturn(new byte[]{3, 2, 2, 3});
        when(mPinEncoder.encodeCertPin(eq("sha256"), eq(new byte[]{0, 1, 2, 3}))).thenReturn(new byte[]{0, 1, 2, 3});
        when(mPinEncoder.encodeCertPin(eq("sha256"), eq(new byte[]{4, 5, 6, 7}))).thenReturn(new byte[]{4, 5, 6, 7});
        when(mBase64Encoder.encode(eq(new byte[]{0, 1, 2, 3}))).thenReturn("base64-0-1-2-3");
        when(mBase64Encoder.encode(eq(new byte[]{4, 5, 6, 7}))).thenReturn("base64-4-5-6-7");

        mChecker = getChecker(Collections.singletonMap("my-url.com", Collections.singletonList(pin)));

        try {
            mChecker.checkPins(mMockConnection);
        } catch (SSLPeerUnverifiedException e) {
            verify(mBase64Encoder).encode(eq(new byte[]{0, 1, 2, 3}));
            verify(mBase64Encoder).encode(eq(new byte[]{4, 5, 6, 7}));
            assertEquals("Certificate pinning verification failed for host: my-url.com. Chain:\n" +
                    "CN=cert1 - sha256/base64-0-1-2-3\n" +
                    "CN=cert2 - sha256/base64-4-5-6-7\n", e.getMessage());
            return;
        }
        fail();
    }

    @NonNull
    private CertificateCheckerImpl getChecker(Map<String, List<CertificatePin>> pins) {
        return new CertificateCheckerImpl(pins, mFailureListener, mChainCleaner, mBase64Encoder, mPinEncoder);
    }
}
