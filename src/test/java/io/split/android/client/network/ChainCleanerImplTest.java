package io.split.android.client.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.net.http.X509TrustManagerExtensions;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChainCleanerImplTest {

    private ChainCleanerImpl mChainCleanerImpl;
    private X509TrustManagerExtensions X509TrustManagerExtensions;

    @Before
    public void setUp() {
        X509TrustManagerExtensions = mock(X509TrustManagerExtensions.class);
        mChainCleanerImpl = new ChainCleanerImpl(X509TrustManagerExtensions);
    }

    @Test
    public void cleanCallsCheckServerTrusted() throws CertificateException {
        X509Certificate firstX509Cert = mock(X509Certificate.class);
        X509Certificate secondX509Cert = mock(X509Certificate.class);
        Certificate[] certificates = {
                mock(Certificate.class),
                firstX509Cert,
                mock(Certificate.class),
                secondX509Cert,
        };
        doAnswer(invocation -> Arrays.asList(invocation.getArgument(0))).when(X509TrustManagerExtensions).checkServerTrusted(eq(new X509Certificate[]{firstX509Cert, secondX509Cert}), eq("RSA"), eq("my-host.com"));

        mChainCleanerImpl.clean("my-host.com", certificates);

        verify(X509TrustManagerExtensions).checkServerTrusted(argThat(new ArgumentMatcher<X509Certificate[]>() {
            @Override
            public boolean matches(X509Certificate[] argument) {
                return argument.length == 2 && argument[0] == firstX509Cert && argument[1] == secondX509Cert;
            }
        }), eq("RSA"), eq("my-host.com"));
    }

    @Test
    public void cleanCallsReturnsGetServerTrustedResult() throws CertificateException {
        X509Certificate firstX509Cert = mock(X509Certificate.class);
        X509Certificate secondX509Cert = mock(X509Certificate.class);
        Certificate[] certificates = {
                mock(Certificate.class),
                firstX509Cert,
                mock(Certificate.class),
                secondX509Cert,
        };
        final List<X509Certificate> mockResult = new ArrayList<>();
        doAnswer(invocation -> {
            mockResult.addAll(Arrays.asList(invocation.getArgument(0)));
            return mockResult;
        }).when(X509TrustManagerExtensions).checkServerTrusted(eq(new X509Certificate[]{firstX509Cert, secondX509Cert}), eq("RSA"), eq("my-host.com"));

        List<X509Certificate> result = mChainCleanerImpl.clean("my-host.com", certificates);

        assertEquals(mockResult, result);
    }

    @Test
    public void failureInCheckServerTrustedCallReturnsEmptyList() throws CertificateException {
        X509Certificate[] certificates = {
                mock(X509Certificate.class),
                mock(X509Certificate.class),
        };
        doAnswer(invocation -> {
            throw new CertificateException();
        }).when(X509TrustManagerExtensions).checkServerTrusted(eq(new X509Certificate[]{(X509Certificate) certificates[0], (X509Certificate) certificates[1]}), eq("RSA"), eq("my-host.com"));

        List<X509Certificate> result = mChainCleanerImpl.clean("my-host.com", certificates);

        assertTrue(result.isEmpty());
    }

    @Test
    public void nullOriginalChainReturnsEmptyList() {
        List<X509Certificate> result = mChainCleanerImpl.clean("my-host.com", null);

        assertTrue(result.isEmpty());
    }

    @Test
    public void emptyOriginalChainReturnsEmptyList() {
        List<X509Certificate> result = mChainCleanerImpl.clean("my-host.com", new Certificate[0]);

        assertTrue(result.isEmpty());
    }
}
