package io.split.android.client.network;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;

public class HttpRequestHelperTest {

    @Mock
    private HttpURLConnection mockConnection;
    @Mock
    private HttpsURLConnection mockHttpsConnection;
    @Mock
    private URL mockUrl;
    @Mock
    private SplitUrlConnectionAuthenticator mockAuthenticator;
    @Mock
    private SSLSocketFactory mockSslSocketFactory;
    @Mock
    private DevelopmentSslConfig mockDevelopmentSslConfig;
    @Mock
    private CertificateChecker mockCertificateChecker;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(mockUrl.openConnection()).thenReturn(mockConnection);
        when(mockUrl.openConnection(any(Proxy.class))).thenReturn(mockConnection);
        when(mockAuthenticator.authenticate(any(HttpURLConnection.class))).thenReturn(mockConnection);
    }

    @Test
    public void addHeaders() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer token123");
        headers.put(null, "This should be ignored");
        
        Method addHeadersMethod = HttpRequestHelper.class.getDeclaredMethod(
                "addHeaders", HttpURLConnection.class, Map.class);
        addHeadersMethod.setAccessible(true);
        addHeadersMethod.invoke(null, mockConnection, headers);

        verify(mockConnection).addRequestProperty("Content-Type", "application/json");
        verify(mockConnection).addRequestProperty("Authorization", "Bearer token123");
        verify(mockConnection, never()).addRequestProperty(null, "This should be ignored");
    }
    
    @Test
    public void applyTimeouts() {
        HttpRequestHelper.applyTimeouts(5000, 3000, mockConnection);
        verify(mockConnection).setReadTimeout(5000);
        verify(mockConnection).setConnectTimeout(3000);
        
        HttpRequestHelper.applyTimeouts(0, 0, mockConnection);
        verify(mockConnection, times(1)).setReadTimeout(any(Integer.class));
        verify(mockConnection, times(1)).setConnectTimeout(any(Integer.class));
        
        HttpRequestHelper.applyTimeouts(-1000, -500, mockConnection);
        verify(mockConnection, times(1)).setReadTimeout(any(Integer.class));
        verify(mockConnection, times(1)).setConnectTimeout(any(Integer.class));
    }
    
    @Test
    public void applySslConfigWithDevelopmentSslConfig() {
        when(mockDevelopmentSslConfig.getSslSocketFactory()).thenReturn(mockSslSocketFactory);

        HttpRequestHelper.applySslConfig(null, mockDevelopmentSslConfig, mockHttpsConnection);

        verify(mockHttpsConnection).setSSLSocketFactory(mockSslSocketFactory);
        verify(mockHttpsConnection).setHostnameVerifier(any());
    }
    
    @Test
    public void pinsAreCheckedWithCertificateChecker() throws SSLPeerUnverifiedException {
        HttpRequestHelper.checkPins(mockHttpsConnection, mockCertificateChecker);

        verify(mockCertificateChecker).checkPins(mockHttpsConnection);
    }
    
    @Test
    public void pinsAreNotCheckedWithoutCertificateChecker() throws SSLPeerUnverifiedException {
        HttpRequestHelper.checkPins(mockHttpsConnection, null);
        
        verify(mockCertificateChecker, never()).checkPins(any());
    }
    
    @Test
    public void pinsAreNotCheckedForNonHttpsConnections() throws SSLPeerUnverifiedException {
        HttpRequestHelper.checkPins(mockConnection, mockCertificateChecker);

        verify(mockCertificateChecker, never()).checkPins(any());
    }
}
