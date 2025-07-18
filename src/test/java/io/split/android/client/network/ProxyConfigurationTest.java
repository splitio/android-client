package io.split.android.client.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class ProxyConfigurationTest {

    private static final String VALID_URL = "http://proxy.example.com:8080";
    private static final String INVALID_URL = "invalid://\\url";
    private static final String URL_WITH_PATH = "https://proxy.example.com:8080/path/to/proxy";
    private static final String URL_WITH_PATH_NORMALIZED = "https://proxy.example.com:8080/path/to/proxy";

    @Mock
    private BearerCredentialsProvider mockBearerCredentialsProvider;
    
    @Mock
    private BasicCredentialsProvider mockBasicCredentialsProvider;
    
    private InputStream clientCert;
    private InputStream clientPk;
    private InputStream caCert;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        clientCert = new ByteArrayInputStream("client-cert-content".getBytes());
        clientPk = new ByteArrayInputStream("client-pk-content".getBytes());
        caCert = new ByteArrayInputStream("ca-cert-content".getBytes());
    }

    @Test
    public void buildWithValidUrl() {
        ProxyConfiguration config = ProxyConfiguration.builder()
                .url(VALID_URL)
                .build();
        
        assertNotNull("Configuration should be created with valid URL", config);
        assertEquals("URL should match", VALID_URL, config.getUrl().toString());
    }
    
    @Test
    public void buildWithInvalidUrlHasNoNullConfig() {
        ProxyConfiguration config = ProxyConfiguration.builder()
                .url(INVALID_URL)
                .build();
        
        assertNotNull(config);
    }
    
    @Test
    public void urlIsNormalized() {
        ProxyConfiguration config = ProxyConfiguration.builder()
                .url(URL_WITH_PATH)
                .build();
        
        assertNotNull("Configuration should be created with URL containing path", config);
        assertEquals("URL should be normalized", URL_WITH_PATH_NORMALIZED, config.getUrl().toString());
    }
    
    @Test
    public void bearerCredentialsProvider() {
        ProxyConfiguration config = ProxyConfiguration.builder()
                .url(VALID_URL)
                .credentialsProvider(mockBearerCredentialsProvider)
                .build();
        
        assertNotNull("Configuration should be created with bearer credentials", config);
        assertSame("Credentials provider should match", mockBearerCredentialsProvider, config.getCredentialsProvider());
    }
    
    @Test
    public void basicCredentialsProvider() {
        BasicCredentialsProvider provider = mockBasicCredentialsProvider;
        
        ProxyConfiguration config = ProxyConfiguration.builder()
                .url(VALID_URL)
                .credentialsProvider(provider)
                .build();
        
        assertNotNull("Configuration should be created with basic credentials", config);
        assertSame("Credentials provider should match", provider, config.getCredentialsProvider());
    }
    
    @Test
    public void mtlsValues() {
        ProxyConfiguration config = ProxyConfiguration.builder()
                .url(VALID_URL)
                .mtls(clientCert, clientPk)
                .build();
        
        assertNotNull("Configuration should be created with mTLS", config);
        assertSame("Client certificate should match", clientCert, config.getClientCert());
        assertSame("Client private key should match", clientPk, config.getClientPk());
    }
    
    @Test
    public void cacert() {
        ProxyConfiguration config = ProxyConfiguration.builder()
                .url(VALID_URL)
                .caCert(caCert)
                .build();
        
        assertNotNull("Configuration should be created with CA certificate", config);
        assertSame("CA certificate should match", caCert, config.getCaCert());
    }
    
    @Test
    public void allOptions() {
        ProxyConfiguration config = ProxyConfiguration.builder()
                .url(VALID_URL)
                .credentialsProvider(mockBearerCredentialsProvider)
                .mtls(clientCert, clientPk)
                .caCert(caCert)
                .build();
        
        assertNotNull("Configuration should be created with all options", config);
        assertEquals("URL should match", VALID_URL, config.getUrl().toString());
        assertSame("Credentials provider should match", mockBearerCredentialsProvider, config.getCredentialsProvider());
        assertSame("Client certificate should match", clientCert, config.getClientCert());
        assertSame("Client private key should match", clientPk, config.getClientPk());
        assertSame("CA certificate should match", caCert, config.getCaCert());
    }
    
    @Test
    public void buildWithoutUrlReturnsNonNullConfig() {
        ProxyConfiguration config = ProxyConfiguration.builder()
                .credentialsProvider(mockBearerCredentialsProvider)
                .build();
        
        assertNotNull("Configuration should be created with null URL", config);
    }
}
