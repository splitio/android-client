package io.split.android.client.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
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
 * Test for HTTPS-over-SSL-proxy scenarios requiring two TLS handshakes.
 * 
 * This test validates the "onion layering" architecture:
 * 1. First TLS: Client ↔ SSL Proxy (with proxy CA cert)
 * 2. CONNECT: Through first TLS connection
 * 3. Second TLS: Client ↔ HTTPS Origin (through raw tunnel streams)
 */
public class HttpsOverSslProxyTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private MockWebServer originServer;
    private HttpClientTunnellingProxyTest.TunnelProxySslServerOnly sslProxy;
    private CountDownLatch originLatch;
    private String[] methodAndPath = new String[2];

    @Before
    public void setUp() throws Exception {
        System.out.println("=== SETUP METHOD STARTED ===");
        
        // Create certificates for proxy and origin
        System.out.println("Creating proxy CA certificate...");
        HeldCertificate proxyCa = new HeldCertificate.Builder()
                .certificateAuthority(0)
                .commonName("Proxy CA")
                .build();
        System.out.println("Proxy CA certificate created");

        System.out.println("Creating proxy server certificate...");
        HeldCertificate proxyServerCert = new HeldCertificate.Builder()
                .commonName("localhost")
                .signedBy(proxyCa)
                .build();
        System.out.println("Proxy server certificate created");

        System.out.println("Creating origin CA certificate...");
        HeldCertificate originCa = new HeldCertificate.Builder()
                .certificateAuthority(0)
                .commonName("Origin CA")
                .build();
        System.out.println("Origin CA certificate created");

        System.out.println("Creating origin server certificate...");
        HeldCertificate originCert = new HeldCertificate.Builder()
                .commonName("localhost")
                .signedBy(originCa)
                .build();
        System.out.println("Origin server certificate created");

        // Start HTTPS origin server
        System.out.println("Creating MockWebServer for origin...");
        originServer = new MockWebServer();
        System.out.println("MockWebServer created");
        
        System.out.println("Setting up origin server dispatcher...");
        originLatch = new CountDownLatch(1);
        originServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                methodAndPath[0] = request.getMethod();
                methodAndPath[1] = request.getPath();
                originLatch.countDown();
                return new MockResponse().setBody("Hello from HTTPS origin via SSL proxy!");
            }
        });
        System.out.println("Origin server dispatcher set up");
        
        System.out.println("Configuring origin server with HTTPS...");
        // Restore HTTPS origin server - this is the point of the test!
        originServer.useHttps(createSslSocketFactory(originCert), false);
        System.out.println("Origin server HTTPS configured");
        
        System.out.println("Starting origin server...");
        originServer.start();
        System.out.println("Origin server started on port: " + originServer.getPort());

        // Start SSL proxy
        System.out.println("Creating SSL proxy...");
        sslProxy = new HttpClientTunnellingProxyTest.TunnelProxySslServerOnly(0, proxyServerCert);
        System.out.println("SSL proxy created");
        
        System.out.println("Starting SSL proxy...");
        sslProxy.start();
        System.out.println("SSL proxy start() called");
        
        System.out.println("Waiting for SSL proxy port assignment...");
        while (sslProxy.mPort == 0) {
            Thread.sleep(10);
        }
        System.out.println("SSL proxy started on port: " + sslProxy.mPort);

        // Write combined CA cert file (proxy CA + origin CA)
        System.out.println("Creating combined CA certificate file...");
        File caCertFile = tempFolder.newFile("combined-ca.pem");
        System.out.println("CA cert file created: " + caCertFile.getAbsolutePath());
        
        System.out.println("Writing CA certificates to file...");
        try (FileWriter writer = new FileWriter(caCertFile)) {
            System.out.println("Writing proxy CA certificate...");
            writer.write(proxyCa.certificatePem());
            System.out.println("Writing origin CA certificate...");
            writer.write(originCa.certificatePem());
        }
        System.out.println("CA certificates written to file");
        
        System.out.println("=== SETUP METHOD COMPLETED ===");
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

    @Test
    public void httpOverSslProxy_basicTunnel_succeeds() throws Exception {
        System.out.println("=== HTTPS-over-SSL-proxy Basic Tunnel Test ===");
        System.out.println("Test method started");
        
        // Create SSL proxy tunnel establisher
        System.out.println("Creating SslProxyTunnelEstablisher...");
        SslProxyTunnelEstablisher tunnelEstablisher = new SslProxyTunnelEstablisher();
        System.out.println("SslProxyTunnelEstablisher created");
        
        // Create SSL socket factory with combined CA certificates
        System.out.println("Setting up CA certificate file...");
        File caCertFile = new File(tempFolder.getRoot(), "combined-ca.pem");
        System.out.println("CA cert file path: " + caCertFile.getAbsolutePath());
        System.out.println("CA cert file exists: " + caCertFile.exists());
        
        ProxySslContextFactoryImpl sslContextFactory = new ProxySslContextFactoryImpl();
        System.out.println("ProxySslContextFactoryImpl created");
        
        // Use InputStream-based API (the correct method)
        System.out.println("About to create SSL socket factory...");
        try (java.io.FileInputStream caCertStream = new java.io.FileInputStream(caCertFile)) {
            System.out.println("FileInputStream created, calling sslContextFactory.create()...");
            SSLSocketFactory sslSocketFactory = sslContextFactory.create(caCertStream);
            System.out.println("SSL socket factory created successfully");
            
            // Step 1: Establish SSL tunnel to proxy (First TLS handshake)
            System.out.println("Step 1: Establishing SSL tunnel to proxy...");
            System.out.println("Proxy port: " + sslProxy.mPort);
            System.out.println("Origin port: " + originServer.getPort());
            
            javax.net.ssl.SSLSocket tunnelSocket = tunnelEstablisher.establishTunnel(
                "localhost",
                sslProxy.mPort,
                "localhost", 
                originServer.getPort(),
                sslSocketFactory
            );
            System.out.println("SSL tunnel established successfully");
            
            // Set socket timeout to prevent hanging
            tunnelSocket.setSoTimeout(10000); // 10 second timeout
            
            // Step 2: Execute HTTPS request through tunnel (Second TLS handshake)
            System.out.println("Step 2: Executing HTTPS request through tunnel...");
            HttpOverTunnelExecutor tunnelExecutor = new HttpOverTunnelExecutor();
            
            // Use HTTPS instead of HTTP to test HTTPS-over-SSL-proxy
            URL httpsUrl = new URL("https://localhost:" + originServer.getPort() + "/test");
            System.out.println("Target HTTPS URL: " + httpsUrl);
            Map<String, String> headers = new HashMap<>();
            
            try {
                HttpResponse response = tunnelExecutor.executeRequest(
                    tunnelSocket,
                    httpsUrl,
                    HttpMethod.GET,
                    headers,
                    null,
                    sslSocketFactory  // Pass the combined SSL socket factory
                );
                
                // Validate response
                assertNotNull("Response should not be null", response);
                assertEquals("Response status should be 200", 200, response.getHttpStatus());
                assertTrue("Response should contain expected data", 
                           response.getData().contains("Hello from HTTPS origin via SSL proxy!"));
                
                // Validate that origin server received the request
                assertTrue("Origin server should have received request", 
                           originLatch.await(5, TimeUnit.SECONDS));
                assertEquals("GET", methodAndPath[0]);
                assertEquals("/test", methodAndPath[1]);
                
                System.out.println("HTTPS-over-SSL-proxy test completed successfully!");
                
            } finally {
                tunnelSocket.close();
            }
        }
    }

    private SSLSocketFactory createSslSocketFactory(HeldCertificate certificate) {
        HandshakeCertificates handshakeCertificates = new HandshakeCertificates.Builder()
                .heldCertificate(certificate)
                .build();
        return handshakeCertificates.sslSocketFactory();
    }
}
