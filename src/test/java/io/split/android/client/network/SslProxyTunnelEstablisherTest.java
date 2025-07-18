package io.split.android.client.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpRetryException;
import java.net.Socket;
import java.security.KeyStore;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocketFactory;

import okhttp3.tls.HeldCertificate;

public class SslProxyTunnelEstablisherTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private TestSslProxy testProxy;
    private SSLSocketFactory clientSslSocketFactory;

    @Before
    public void setUp() throws Exception {
        // Create test certificates
        HeldCertificate proxyCa = new HeldCertificate.Builder()
                .commonName("Test Proxy CA")
                .certificateAuthority(0)
                .build();
        HeldCertificate proxyServerCert = new HeldCertificate.Builder()
                .commonName("localhost")
                .signedBy(proxyCa)
                .build();

        // Create SSL socket factory that trusts the proxy CA
        File proxyCaFile = tempFolder.newFile("proxy-ca.pem");
        try (FileWriter writer = new FileWriter(proxyCaFile)) {
            writer.write(proxyCa.certificatePem());
        }

        ProxySslSocketFactoryProvider factory = new ProxySslSocketFactoryProviderImpl();
        try (java.io.FileInputStream caInput = new java.io.FileInputStream(proxyCaFile)) {
            clientSslSocketFactory = factory.create(caInput);
        }

        // Start test SSL proxy
        testProxy = new TestSslProxy(0, proxyServerCert);
        testProxy.start();

        // Wait for proxy to start
        while (testProxy.getPort() == 0) {
            Thread.sleep(10);
        }
    }

    @After
    public void tearDown() throws Exception {
        if (testProxy != null) {
            testProxy.stop();
        }
    }

    @Test
    public void establishTunnelWithValidSslProxySucceeds() throws Exception {
        SslProxyTunnelEstablisher establisher = new SslProxyTunnelEstablisher();
        String targetHost = "example.com";
        int targetPort = 443;
        BearerCredentialsProvider proxyCredentialsProvider = mock(BearerCredentialsProvider.class);

        Socket tunnelSocket = establisher.establishTunnel(
                "localhost",
                testProxy.getPort(),
                targetHost,
                targetPort,
                clientSslSocketFactory,
                proxyCredentialsProvider,
                false);

        assertNotNull("Tunnel socket should not be null", tunnelSocket);
        assertTrue("Tunnel socket should be connected", tunnelSocket.isConnected());

        // Verify CONNECT request was sent and successful
        assertTrue("Proxy should have received CONNECT request",
                testProxy.getConnectRequestReceived().await(5, TimeUnit.SECONDS));
        assertEquals("CONNECT example.com:443 HTTP/1.1", testProxy.getReceivedConnectLine());

        tunnelSocket.close();
    }

    @Test
    public void establishTunnelWithNotTrustedCertificatedThrows() throws Exception {
        SSLContext untrustedContext = SSLContext.getInstance("TLS");
        untrustedContext.init(null, null, null);
        SSLSocketFactory untrustedSocketFactory = untrustedContext.getSocketFactory();
        BearerCredentialsProvider proxyCredentialsProvider = mock(BearerCredentialsProvider.class);

        SslProxyTunnelEstablisher establisher = new SslProxyTunnelEstablisher();

        try {
            establisher.establishTunnel(
                    "localhost",
                    testProxy.getPort(),
                    "example.com",
                    443,
                    untrustedSocketFactory,
                    proxyCredentialsProvider,
                    false);
            fail("Should have thrown exception for untrusted certificate");
        } catch (IOException e) {
            assertTrue("Exception should be SSL-related", e.getMessage().contains("certification"));
        }
    }

    @Test
    public void establishTunnelWithFailingProxyConnectionThrows() {
        SslProxyTunnelEstablisher establisher = new SslProxyTunnelEstablisher();
        BearerCredentialsProvider proxyCredentialsProvider = mock(BearerCredentialsProvider.class);

        try {
            establisher.establishTunnel(
                    "localhost",
                    -1234,
                    "example.com",
                    443,
                    clientSslSocketFactory,
                    proxyCredentialsProvider,
                    false);
            fail("Should have thrown exception for connection failure");
        } catch (IOException e) {
            // The implementation wraps the original exception with a descriptive message
            assertTrue(e.getMessage().contains("Failed to establish SSL tunnel"));
        }
    }

    @Test
    public void bearerTokenIsPassedWhenSet() throws IOException, InterruptedException {
        SslProxyTunnelEstablisher establisher = new SslProxyTunnelEstablisher();
        establisher.establishTunnel(
                "localhost",
                testProxy.getPort(),
                "example.com",
                443,
                clientSslSocketFactory,
                new BearerCredentialsProvider() {
                    @Override
                    public String getToken() {
                        return "token";
                    }
                },
                false);
        boolean await = testProxy.getAuthorizationHeaderReceived().await(5, TimeUnit.SECONDS);
        assertTrue("Proxy should have received authorization header", await);
    }

    @Test
    public void establishTunnelWithNullCredentialsProviderDoesNotAddAuthHeader() throws Exception {
        SslProxyTunnelEstablisher establisher = new SslProxyTunnelEstablisher();

        Socket tunnelSocket = establisher.establishTunnel(
                "localhost",
                testProxy.getPort(),
                "example.com",
                443,
                clientSslSocketFactory,
                null,
                false);

        assertNotNull(tunnelSocket);
        assertTrue(testProxy.getConnectRequestReceived().await(5, TimeUnit.SECONDS));

        assertEquals(1, testProxy.getAuthorizationHeaderReceived().getCount());

        tunnelSocket.close();
    }

    @Test
    public void establishTunnelWithNullBearerTokenDoesNotAddAuthHeader() throws Exception {
        SslProxyTunnelEstablisher establisher = new SslProxyTunnelEstablisher();

        Socket tunnelSocket = establisher.establishTunnel(
                "localhost",
                testProxy.getPort(),
                "example.com",
                443,
                clientSslSocketFactory,
                new BearerCredentialsProvider() {
                    @Override
                    public String getToken() {
                        return null;
                    }
                },
                false);

        assertNotNull(tunnelSocket);
        assertTrue(testProxy.getConnectRequestReceived().await(5, TimeUnit.SECONDS));

        assertEquals(1, testProxy.getAuthorizationHeaderReceived().getCount());

        tunnelSocket.close();
    }

    @Test
    public void establishTunnelWithEmptyBearerTokenDoesNotAddAuthHeader() throws Exception {
        SslProxyTunnelEstablisher establisher = new SslProxyTunnelEstablisher();

        Socket tunnelSocket = establisher.establishTunnel(
                "localhost",
                testProxy.getPort(),
                "example.com",
                443,
                clientSslSocketFactory,
                new BearerCredentialsProvider() {
                    @Override
                    public String getToken() {
                        return "";
                    }
                },
                false);

        assertNotNull(tunnelSocket);
        assertTrue(testProxy.getConnectRequestReceived().await(5, TimeUnit.SECONDS));

        assertEquals(1, testProxy.getAuthorizationHeaderReceived().getCount());

        tunnelSocket.close();
    }

    @Test
    public void establishTunnelWithNullStatusLineThrowsIOException() {
        testProxy.setConnectResponse(null);
        SslProxyTunnelEstablisher establisher = new SslProxyTunnelEstablisher();

        IOException exception = assertThrows(IOException.class, () -> establisher.establishTunnel(
                "localhost",
                testProxy.getPort(),
                "example.com",
                443,
                clientSslSocketFactory,
                null, false));

        assertNotNull(exception);
    }

    @Test
    public void establishTunnelWithMalformedStatusLineThrowsIOException() {
        testProxy.setConnectResponse("HTTP/1.1"); // Malformed, missing status code
        SslProxyTunnelEstablisher establisher = new SslProxyTunnelEstablisher();

        IOException exception = assertThrows(IOException.class, () -> establisher.establishTunnel(
                "localhost",
                testProxy.getPort(),
                "example.com",
                443,
                clientSslSocketFactory,
                null,
                false));

        assertNotNull(exception);
    }

    @Test
    public void establishTunnelWithProxyAuthRequiredThrowsHttpRetryException() {
        testProxy.setConnectResponse("HTTP/1.1 407 Proxy Authentication Required");
        SslProxyTunnelEstablisher establisher = new SslProxyTunnelEstablisher();

        HttpRetryException exception = assertThrows(HttpRetryException.class, () -> establisher.establishTunnel(
                "localhost",
                testProxy.getPort(),
                "example.com",
                443,
                clientSslSocketFactory,
                null,
                false));

        assertEquals(407, exception.responseCode());
    }

    /**
     * Test SSL proxy that accepts SSL connections and handles CONNECT requests.
     */
    private static class TestSslProxy extends Thread {
        private final int mPort;
        private final HeldCertificate mServerCert;
        private SSLServerSocket mServerSocket;
        private final AtomicBoolean mRunning = new AtomicBoolean(true);
        private final CountDownLatch mConnectRequestReceived = new CountDownLatch(1);
        private final CountDownLatch mAuthorizationHeaderReceived = new CountDownLatch(1);
        private final AtomicReference<String> mReceivedConnectLine = new AtomicReference<>();
        private final AtomicReference<String> mConnectResponse = new AtomicReference<>("HTTP/1.1 200 Connection established");

        public TestSslProxy(int port, HeldCertificate serverCert) {
            mPort = port;
            mServerCert = serverCert;
        }

        @Override
        public void run() {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                KeyStore ks = KeyStore.getInstance("PKCS12");
                ks.load(null, null);
                ks.setKeyEntry("key", mServerCert.keyPair().getPrivate(), "password".toCharArray(),
                        new java.security.cert.Certificate[]{mServerCert.certificate()});
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(ks, "password".toCharArray());
                sslContext.init(kmf.getKeyManagers(), null, null);

                mServerSocket = (SSLServerSocket) sslContext.getServerSocketFactory().createServerSocket(mPort);
                mServerSocket.setWantClientAuth(false);
                mServerSocket.setNeedClientAuth(false);

                while (mRunning.get()) {
                    try {
                        Socket client = mServerSocket.accept();
                        handleClient(client);
                    } catch (IOException e) {
                        if (mRunning.get()) {
                            System.err.println("Error accepting client: " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to start test SSL proxy", e);
            }
        }

        private void handleClient(Socket client) {
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(client.getInputStream()));
                PrintWriter writer = new PrintWriter(client.getOutputStream(), true);

                // Read CONNECT request
                String line = reader.readLine();
                if (line != null && line.startsWith("CONNECT")) {
                    mReceivedConnectLine.set(line);
                    mConnectRequestReceived.countDown();

                    while((line = reader.readLine()) != null && !line.isEmpty()) {
                        if (line.contains("Authorization") && line.contains("Bearer")) {
                            mAuthorizationHeaderReceived.countDown();
                        }
                    }

                    // Send configured CONNECT response
                    String response = mConnectResponse.get();
                    if (response != null) {
                        writer.println(response);
                        writer.println();
                        writer.flush();
                    }

                    // Keep connection open for tunnel
                    Thread.sleep(100);
                }
            } catch (Exception e) {
                System.err.println("Error handling client: " + e.getMessage());
            } finally {
                try {
                    client.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }

        public int getPort() {
            return mServerSocket != null ? mServerSocket.getLocalPort() : 0;
        }

        public CountDownLatch getConnectRequestReceived() {
            return mConnectRequestReceived;
        }

        public CountDownLatch getAuthorizationHeaderReceived() {
            return mAuthorizationHeaderReceived;
        }

        public String getReceivedConnectLine() {
            return mReceivedConnectLine.get();
        }

        public void setConnectResponse(String connectResponse) {
            mConnectResponse.set(connectResponse);
        }
    }
}
