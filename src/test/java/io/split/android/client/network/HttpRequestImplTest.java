package io.split.android.client.network;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;

public class HttpRequestImplTest {

    @Test
    public void certificateCheckerIsUsed() throws URISyntaxException, HttpException, SSLPeerUnverifiedException, MalformedURLException {
        UrlSanitizer urlSanitizer = mock(UrlSanitizer.class);
        CertificateChecker certificateChecker = mock(CertificateChecker.class);
        when(urlSanitizer.getUrl(any())).thenReturn(new URL("https://split.io"));

        HttpRequestImpl httpRequest = new HttpRequestImpl(new URI("https://split.io"), HttpMethod.GET,
                null, new HashMap<>(), null, null, 5, 5,
                null, null, urlSanitizer, certificateChecker);

        httpRequest.execute();

        verify(certificateChecker).checkPins(argThat(new ArgumentMatcher<HttpsURLConnection>() {
            @Override
            public boolean matches(HttpsURLConnection argument) {
                return argument.getURL().getHost().equals("split.io");
            }
        }));
    }
}
