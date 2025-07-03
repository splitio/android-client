package io.split.android.client.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

        ProxySslContextFactory factory = new ProxySslContextFactoryImpl();
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
    public void establishTunnel_withValidSslProxy_succeeds() throws Exception {
        // Arrange
        SslProxyTunnelEstablisher establisher = new SslProxyTunnelEstablisher();
        String targetHost = "example.com";
        int targetPort = 443;

        // Act
        Socket tunnelSocket = establisher.establishTunnel(
            "localhost", 
            testProxy.getPort(),
            targetHost,
            targetPort,
            clientSslSocketFactory,
                proxyAuthenticator);

        // Assert
        assertNotNull("Tunnel socket should not be null", tunnelSocket);
        assertTrue("Tunnel socket should be connected", tunnelSocket.isConnected());
//        assertTrue("SSL handshake should be completed", tunnelSocket.getSession().isValid());
        
        // Verify CONNECT request was sent and successful
        assertTrue("Proxy should have received CONNECT request", 
                   testProxy.getConnectRequestReceived().await(5, TimeUnit.SECONDS));
        assertEquals("CONNECT example.com:443 HTTP/1.1", testProxy.getReceivedConnectLine());
        
        tunnelSocket.close();
    }

    @Test
    public void establishTunnel_withInvalidSslCertificate_throwsException() throws Exception {
        // Arrange - create SSL socket factory that doesn't trust the proxy
        SSLContext untrustedContext = SSLContext.getInstance("TLS");
        untrustedContext.init(null, null, null); // Use default trust manager (won't trust our proxy)
        SSLSocketFactory untrustedSocketFactory = untrustedContext.getSocketFactory();
        
        SslProxyTunnelEstablisher establisher = new SslProxyTunnelEstablisher();

        // Act & Assert
        try {
            establisher.establishTunnel(
                "localhost", 
                testProxy.getPort(),
                "example.com",
                443,
                untrustedSocketFactory,
                    proxyAuthenticator);
            fail("Should have thrown exception for untrusted certificate");
        } catch (IOException e) {
            assertTrue("Exception should be SSL-related", e.getMessage().contains("certification"));
        }
    }

    @Test
    public void establishTunnel_withProxyConnectionFailure_throwsException() throws Exception {
        // Arrange
        SslProxyTunnelEstablisher establisher = new SslProxyTunnelEstablisher();
        int nonExistentPort = 9999; // Assuming this port is not in use

        // Act & Assert
        try {
            establisher.establishTunnel(
                "localhost", 
                nonExistentPort,
                "example.com",
                443,
                clientSslSocketFactory,
                    proxyAuthenticator);
            fail("Should have thrown exception for connection failure");
        } catch (IOException e) {
            // The implementation wraps the original exception with a descriptive message
            assertTrue(e.getMessage().contains("Connection"));
        }
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
        private final AtomicReference<String> mReceivedConnectLine = new AtomicReference<>();

        public TestSslProxy(int port, HeldCertificate serverCert) {
            this.mPort = port;
            this.mServerCert = serverCert;
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
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(client.getInputStream()));
                java.io.PrintWriter writer = new java.io.PrintWriter(client.getOutputStream(), true);

                // Read CONNECT request
                String connectLine = reader.readLine();
                if (connectLine != null && connectLine.startsWith("CONNECT")) {
                    mReceivedConnectLine.set(connectLine);
                    mConnectRequestReceived.countDown();
                    
                    // Send successful CONNECT response
                    writer.println("HTTP/1.1 200 Connection established");
                    writer.println();
                    writer.flush();
                    
                    // Keep connection open for tunnel
                    Thread.sleep(100); // Brief pause to simulate tunnel establishment
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

        public void stopRun() throws IOException {
            mRunning.set(false);
            if (mServerSocket != null) {
                mServerSocket.close();
            }
        }

        public CountDownLatch getConnectRequestReceived() {
            return mConnectRequestReceived;
        }

        public String getReceivedConnectLine() {
            return mReceivedConnectLine.get();
        }
    }
}
