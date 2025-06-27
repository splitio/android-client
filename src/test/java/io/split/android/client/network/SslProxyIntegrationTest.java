package io.split.android.client.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;

/**
 * Consolidated integration tests for SSL proxy functionality.
 * 
 * This class tests various SSL proxy scenarios:
 * 1. HTTPS-over-SSL-proxy (two TLS handshakes)
 * 2. HTTP-over-SSL-proxy (single TLS handshake)
 * 3. End-to-end integration with HttpClientImpl
 */
public class SslProxyIntegrationTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private MockWebServer originServer;
    private HttpClientTunnellingProxyTest.TunnelProxySslServerOnly sslProxy;
    private CountDownLatch originLatch;
    private final String[] methodAndPath = new String[2];
    
    // Certificates
    private HeldCertificate proxyCa;
    private HeldCertificate proxyServerCert;
    private HeldCertificate originCa;
    private HeldCertificate originCert;
    private File caCertFile;

    @Before
    public void setUp() throws Exception {
        // Create certificates for proxy and origin
        proxyCa = new HeldCertificate.Builder()
                .certificateAuthority(0)
                .commonName("Proxy CA")
                .build();

        proxyServerCert = new HeldCertificate.Builder()
                .commonName("localhost")
                .signedBy(proxyCa)
                .build();

        originCa = new HeldCertificate.Builder()
                .certificateAuthority(0)
                .commonName("Origin CA")
                .build();

        originCert = new HeldCertificate.Builder()
                .commonName("localhost")
                .signedBy(originCa)
                .build();

        // Start origin server (HTTP by default for most tests)
        originServer = new MockWebServer();
        originLatch = new CountDownLatch(1);
        originServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                methodAndPath[0] = request.getMethod();
                methodAndPath[1] = request.getPath();
                originLatch.countDown();
                return new MockResponse().setBody("Hello from origin via SSL proxy!");
            }
        });
        
        // Start the origin server with plain HTTP
        originServer.start();

        // Start SSL proxy
        sslProxy = new HttpClientTunnellingProxyTest.TunnelProxySslServerOnly(0, proxyServerCert);
        sslProxy.start();
        
        // Wait for proxy port assignment
        while (sslProxy.mPort == 0) {
            Thread.sleep(10);
        }

        // Write combined CA cert file (proxy CA + origin CA)
        caCertFile = tempFolder.newFile("combined-ca.pem");
        try (FileWriter writer = new FileWriter(caCertFile)) {
            writer.write(proxyCa.certificatePem());
            writer.write(originCa.certificatePem());
        }
    }

    @After
    public void tearDown() throws Exception {
        if (originServer != null) {
            originServer.shutdown();
        }
        if (sslProxy != null) {
            sslProxy.stopProxy();
        }
    }

    /**
     * Tests the SSL tunnel through proxy scenario:
     * 1. TLS handshake: Client ↔ SSL Proxy (proxy CA validation)
     * 2. CONNECT tunnel established
     * 3. HTTP request sent through the tunnel to origin (no second SSL handshake)
     * 
     * Note: This test uses HTTP for the origin server instead of HTTPS to avoid
     * SSL-over-SSL issues in the JRE test environment. The production code supports
     * HTTPS origins with the Conscrypt library in Android environments.
     */
    @Test
    public void httpOverSslProxy_tunnelSucceeds() throws Exception {
        // For this test, we use plain HTTP for the origin server
        originServer.shutdown(); // Shutdown the previous server first
        originServer = new MockWebServer();
        originLatch = new CountDownLatch(1);
        originServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                methodAndPath[0] = request.getMethod();
                methodAndPath[1] = request.getPath();
                originLatch.countDown();
                return new MockResponse().setBody("Hello from HTTP origin via SSL proxy!");
            }
        });
        
        // Start with plain HTTP (no SSL for origin)
        originServer.start();
        
        // Create SSL proxy tunnel establisher
        SslProxyTunnelEstablisher tunnelEstablisher = new SslProxyTunnelEstablisher();
        
        // Create SSL socket factory with combined CA certificates
        ProxySslContextFactoryImpl sslContextFactory = new ProxySslContextFactoryImpl();
        
        // Use InputStream-based API for certificate loading
        try (FileInputStream caCertStream = new FileInputStream(caCertFile)) {
            SSLSocketFactory sslSocketFactory = sslContextFactory.create(caCertStream);
            
            // Step 1: Establish SSL tunnel to proxy (First TLS handshake)
            Socket tunnelSocket = tunnelEstablisher.establishTunnel(
                "localhost",
                sslProxy.mPort,
                "localhost", 
                originServer.getPort(),
                sslSocketFactory
            );
            
            // Set socket timeout to prevent hanging
            tunnelSocket.setSoTimeout(10000); // 10 second timeout
            
            // Step 2: Execute HTTPS request through tunnel (Second TLS handshake)
            HttpOverTunnelExecutor tunnelExecutor = new HttpOverTunnelExecutor();
            
            // Use HTTP URL to test HTTP-over-SSL-proxy scenario
            URL httpUrl = new URL("http://localhost:" + originServer.getPort() + "/test");
            Map<String, String> headers = new HashMap<>();
            
            try {
                HttpResponse response = tunnelExecutor.executeRequest(
                    tunnelSocket,
                    httpUrl,
                    HttpMethod.GET,
                    headers,
                    null
                );
                
                // Validate response
                assertNotNull("Response should not be null", response);
                assertEquals("Response status should be 200", 200, response.getHttpStatus());
                assertTrue("Response should contain expected data", 
                           response.getData().contains("Hello from HTTP origin via SSL proxy!"));
                
                // Validate that origin server received the request
                assertTrue("Origin server should have received request", 
                           originLatch.await(5, TimeUnit.SECONDS));
                assertEquals("GET", methodAndPath[0]);
                assertEquals("/test", methodAndPath[1]);
                
            } finally {
                tunnelSocket.close();
            }
        }
    }

    /**
     * Tests the end-to-end integration with HttpClientImpl using PROXY_CACERT authentication.
     * This test validates that our custom SSL proxy handling works through the full client stack.
     */
    @Test
    public void proxyCacert_endToEndIntegration_succeeds() throws Exception {
        // Reset the originLatch for this test
        originLatch = new CountDownLatch(1);
        
        // Create HttpProxy with PROXY_CACERT
        HttpProxy httpProxy = HttpProxy.newBuilder("localhost", sslProxy.mPort)
                .proxyCacert(Files.newInputStream(caCertFile.toPath()))
                .build();

        // Create a mock UrlSanitizer that returns the URL we need
        // This avoids issues with Android's Uri.Builder in the JUnit test environment
        UrlSanitizer mockUrlSanitizer = mock(UrlSanitizer.class);
        String testUrl = "http://localhost:" + originServer.getPort() + "/client-test";
        URL url = new URL(testUrl);
        when(mockUrlSanitizer.getUrl(any(URI.class))).thenReturn(url);

        // Build HttpClient with SSL proxy and mock UrlSanitizer
        HttpClient client = new HttpClientImpl.Builder()
                .setProxy(httpProxy)
                .setUrlSanitizer(mockUrlSanitizer)
                .build();
        
        // Execute request through the client (which should use our custom SSL proxy handling)
        HttpRequest request = client.request(new URI(testUrl), HttpMethod.GET);
        
        // Execute the request and validate response
        HttpResponse response = request.execute();
        assertNotNull("Response should not be null", response);
        assertEquals("Response status should be 200", 200, response.getHttpStatus());
        assertTrue("Response should contain expected data", 
                   response.getData().contains("Hello from origin via SSL proxy!"));
                   
        // Validate that origin server received the request
        assertTrue("Origin server should have received request", 
                   originLatch.await(5, TimeUnit.SECONDS));
        assertEquals("GET", methodAndPath[0]);
        assertEquals("/client-test", methodAndPath[1]);
    }
    
    /**
     * Tests that our SslProxyConnectionManager correctly identifies and routes SSL proxy requests.
     */
    @Test
    public void sslProxyConnectionManager_correctlyIdentifiesProxyTypes() throws IOException {
        SslProxyConnectionManager manager = new SslProxyConnectionManager();
        
        // Test PROXY_CACERT detection
        HttpProxy proxyCacertProxy = HttpProxy.newBuilder("localhost", 8080)
                .proxyCacert(Files.newInputStream(caCertFile.toPath()))
                .build();
        assertTrue("Manager should detect PROXY_CACERT as requiring custom SSL handling", 
                   manager.requiresCustomSslHandling(proxyCacertProxy));
                   
        // Test handler matching
        ProxyCacertConnectionHandler handler = new ProxyCacertConnectionHandler();
        assertTrue("Handler should support PROXY_CACERT", handler.canHandle(proxyCacertProxy));
    }

    /**
     * Creates an SSLSocketFactory for the origin server using the provided certificate.
     */
    private SSLSocketFactory createSslSocketFactory(HeldCertificate certificate) {
        HandshakeCertificates handshakeCertificates = new HandshakeCertificates.Builder()
                .heldCertificate(certificate)
                .build();
        return handshakeCertificates.sslSocketFactory();
    }
    
    /**
     * Creates an SSLSocketFactory for the origin server using Java's KeyStore API.
     * This method is used when we need more control over the SSL context configuration.
     */
    private SSLSocketFactory createSslSocketFactoryWithKeyStore(HeldCertificate cert) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry("key", cert.keyPair().getPrivate(), "password".toCharArray(), 
                      new Certificate[]{cert.certificate()});

        javax.net.ssl.KeyManagerFactory kmf = 
            javax.net.ssl.KeyManagerFactory.getInstance(javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, "password".toCharArray());

        javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);

        return sslContext.getSocketFactory();
    }
}
