package io.split.android.client.network;

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
        doAnswer(invocation -> invocation.getArgument(0)).when(X509TrustManagerExtensions).checkServerTrusted(new X509Certificate[]{firstX509Cert, secondX509Cert}, "RSA", "my-host.com");

        mChainCleanerImpl.clean("my-host.com", certificates);

        verify(X509TrustManagerExtensions).checkServerTrusted(argThat(new ArgumentMatcher<X509Certificate[]>() {
            @Override
            public boolean matches(X509Certificate[] argument) {
                return argument.length == 2 && argument[0] == firstX509Cert && argument[1] == secondX509Cert;
            }
        }), eq("RSA"), eq("my-host.com"));
    }
}
