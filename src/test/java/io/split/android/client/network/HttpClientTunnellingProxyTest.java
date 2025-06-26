package io.split.android.client.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.security.KeyStore;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.tls.HeldCertificate;

public class HttpClientTunnellingProxyTest {


    private UrlSanitizer mUrlSanitizerMock;

    @Before
    public void setUp() {
        mUrlSanitizerMock = mock(UrlSanitizer.class);
        when(mUrlSanitizerMock.getUrl(any())).thenAnswer(new Answer<URL>() {
            @Override
            public URL answer(InvocationOnMock invocation) throws Throwable {
                URI argument = invocation.getArgument(0);

                return new URL(argument.toString());
            }
        });
    }

    @Test
    public void proxyCacertProxyTunnelling() throws Exception {
        // 1. Create CA and server certs
        HeldCertificate proxyCa = new HeldCertificate.Builder()
                .commonName("Test Proxy CA")
                .certificateAuthority(0)
                .build();
        HeldCertificate originCa = new HeldCertificate.Builder()
                .commonName("Test Origin CA")
                .certificateAuthority(0)
                .build();
        HeldCertificate originCert = new HeldCertificate.Builder()
                .commonName("localhost")
                .signedBy(originCa)
                .build();

        // 2. Start HTTPS origin server
        MockWebServer originServer = new MockWebServer();
        CountDownLatch originLatch = new CountDownLatch(1);
        final String[] methodAndPath = new String[2];
        originServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                methodAndPath[0] = request.getMethod();
                methodAndPath[1] = request.getPath();
                originLatch.countDown();
                return new MockResponse().setBody("from origin!");
            }
        });
        originServer.useHttps(createSslSocketFactory(originCert), false);
        originServer.start();

        // 3. Start minimal tunnel proxy
        TunnelProxy tunnelProxy = new TunnelProxy(0);
        tunnelProxy.start();
        while (tunnelProxy.mServerSocket == null || tunnelProxy.mServerSocket.getLocalPort() == 0) {
            Thread.sleep(10);
        }
        int assignedProxyPort = tunnelProxy.mServerSocket.getLocalPort();

        // 4. Write BOTH proxy CA and origin CA certs to temp file
        File caCertFile = File.createTempFile("proxy-ca", ".pem");
        try (FileWriter writer = new FileWriter(caCertFile)) {
            writer.write(proxyCa.certificatePem());
            writer.write(originCa.certificatePem());
        }

        // 5. Configure HttpProxy with PROXY_CACERT
        HttpProxy proxy = HttpProxy.newBuilder("localhost", assignedProxyPort)
                .proxyCacert(caCertFile.getAbsolutePath())
                .build();

        // 6. Build client (let builder/factory handle trust)
        HttpClient client = new HttpClientImpl.Builder()
                .setProxy(proxy)
                .setUrlSanitizer(mUrlSanitizerMock)
                .build();

        // 7. Make a request to the origin server (should tunnel via proxy)
        URI uri = originServer.url("/test").uri();
        HttpRequest req = client.request(uri, HttpMethod.GET);
        HttpResponse resp = req.execute();
        assertNotNull(resp);
        assertEquals(200, resp.getHttpStatus());
        assertEquals("from origin!", resp.getData());

        // Assert that the tunnel was established and the origin received the request
        assertTrue("TunnelProxy did not tunnel the request in time", tunnelProxy.getTunnelLatch().await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertTrue("Origin server did not receive the request in time", originLatch.await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertEquals("GET", methodAndPath[0]);
        assertEquals("/test", methodAndPath[1]);

        tunnelProxy.stopProxy();
        tunnelProxy.join();
        originServer.shutdown();
        caCertFile.delete();
    }

    /**
     * Negative test: mTLS proxy requires client certificate, but client presents none.
     * The proxy should reject the connection, and the client should throw SSLHandshakeException.
     */
    @Test
    public void proxyMtlsProxyTunnelling_rejectsNoClientCert() throws Exception {
        // 1. Create CA, proxy/server, and origin certs
        HeldCertificate proxyCa = new HeldCertificate.Builder()
                .commonName("Test Proxy CA")
                .certificateAuthority(0)
                .build();
        HeldCertificate proxyServerCert = new HeldCertificate.Builder()
                .commonName("localhost")
                .signedBy(proxyCa)
                .build();
        HeldCertificate originCa = new HeldCertificate.Builder()
                .commonName("Test Origin CA")
                .certificateAuthority(0)
                .build();
        HeldCertificate originCert = new HeldCertificate.Builder()
                .commonName("localhost")
                .signedBy(originCa)
                .build();

        // Write proxy server cert and key to temp files
        File proxyCertFile = File.createTempFile("proxy-server", ".crt");
        File proxyKeyFile = File.createTempFile("proxy-server", ".key");
        try (FileWriter writer = new FileWriter(proxyCertFile)) {
            writer.write(proxyServerCert.certificatePem());
        }
        try (FileWriter writer = new FileWriter(proxyKeyFile)) {
            writer.write(proxyServerCert.privateKeyPkcs8Pem());
        }
        // Write proxy CA cert (for client auth) to temp file
        File proxyCaFile = File.createTempFile("proxy-ca", ".crt");
        try (FileWriter writer = new FileWriter(proxyCaFile)) {
            writer.write(proxyCa.certificatePem());
        }

        // 2. Start HTTPS origin server
        MockWebServer originServer = new MockWebServer();
        originServer.useHttps(createSslSocketFactory(originCert), false);
        originServer.start();

        // 3. Start mTLS tunnel proxy
        TunnelProxySsl tunnelProxy = new TunnelProxySsl(0, proxyServerCert, proxyCa);
        tunnelProxy.start();
        while (tunnelProxy.mServerSocket == null || tunnelProxy.mServerSocket.getLocalPort() == 0) {
            Thread.sleep(10);
        }
        int assignedProxyPort = tunnelProxy.mServerSocket.getLocalPort();

        // 4. Configure HttpProxy WITHOUT client cert (should be rejected)
        HttpProxy proxy = HttpProxy.newBuilder("localhost", assignedProxyPort)
                .proxyCacert(proxyCaFile.getAbsolutePath()) // only trust, no client auth
                .build();

        // 5. Build client (let builder/factory handle trust)
        HttpClient client = new HttpClientImpl.Builder()
                .setProxy(proxy)
                .setUrlSanitizer(mUrlSanitizerMock)
                .build();

        // 6. Make a request to the origin server (should fail at proxy handshake)
        URI uri = originServer.url("/test").uri();
        HttpRequest req = client.request(uri, HttpMethod.GET);
        boolean handshakeFailed = false;
        try {
            req.execute();
        } catch (Exception e) {
            handshakeFailed = true;
        }
        assertTrue("Expected SSL handshake to fail due to missing client certificate", handshakeFailed);

        tunnelProxy.stopProxy();
        tunnelProxy.join();
        originServer.shutdown();
        proxyCertFile.delete();
        proxyKeyFile.delete();
        proxyCaFile.delete();
    }

    /**
     * Positive test: mTLS proxy requires client certificate, and client presents a valid certificate.
     * The proxy should accept the connection, tunnel should be established, and the request should reach the origin.
     */
    @Test
    public void proxyMtlsProxyTunnelling() throws Exception {
        // 1. Create CA, proxy/server, client, and origin certs
        HeldCertificate proxyCa = new HeldCertificate.Builder()
                .commonName("Test Proxy CA")
                .certificateAuthority(0)
                .build();
        HeldCertificate proxyServerCert = new HeldCertificate.Builder()
                .commonName("localhost")
                .signedBy(proxyCa)
                .build();
        HeldCertificate clientCert = new HeldCertificate.Builder()
                .commonName("Test Client")
                .signedBy(proxyCa)
                .build();
        HeldCertificate originCa = new HeldCertificate.Builder()
                .commonName("Test Origin CA")
                .certificateAuthority(0)
                .build();
        HeldCertificate originCert = new HeldCertificate.Builder()
                .commonName("localhost")
                .signedBy(originCa)
                .build();

        // Write proxy server cert and key to temp files
        File proxyCertFile = File.createTempFile("proxy-server", ".crt");
        File proxyKeyFile = File.createTempFile("proxy-server", ".key");
        try (FileWriter writer = new FileWriter(proxyCertFile)) {
            writer.write(proxyServerCert.certificatePem());
        }
        try (FileWriter writer = new FileWriter(proxyKeyFile)) {
            writer.write(proxyServerCert.privateKeyPkcs8Pem());
        }
        // Write proxy CA cert (for client auth) to temp file
        File proxyCaFile = File.createTempFile("proxy-ca", ".crt");
        try (FileWriter writer = new FileWriter(proxyCaFile)) {
            writer.write(proxyCa.certificatePem());
        }
        // Write client cert and key to temp files
        File clientCertFile = File.createTempFile("client", ".crt");
        File clientKeyFile = File.createTempFile("client", ".key");
        try (FileWriter writer = new FileWriter(clientCertFile)) {
            writer.write(clientCert.certificatePem());
        }
        try (FileWriter writer = new FileWriter(clientKeyFile)) {
            writer.write(clientCert.privateKeyPkcs8Pem());
        }

        // 2. Start HTTPS origin server
        MockWebServer originServer = new MockWebServer();
        CountDownLatch originLatch = new CountDownLatch(1);
        final String[] methodAndPath = new String[2];
        originServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                methodAndPath[0] = request.getMethod();
                methodAndPath[1] = request.getPath();
                originLatch.countDown();
                return new MockResponse().setBody("from origin!");
            }
        });
        originServer.useHttps(createSslSocketFactory(originCert), false);
        originServer.start();

        // 3. Start mTLS tunnel proxy
        TunnelProxySsl tunnelProxy = new TunnelProxySsl(0, proxyServerCert, proxyCa);
        tunnelProxy.start();
        while (tunnelProxy.mServerSocket == null || tunnelProxy.mServerSocket.getLocalPort() == 0) {
            Thread.sleep(10);
        }
        int assignedProxyPort = tunnelProxy.mServerSocket.getLocalPort();

        // 4. Configure HttpProxy with mTLS (client cert, key, and CA)
        HttpProxy proxy = HttpProxy.newBuilder("localhost", assignedProxyPort)
                .mtlsAuth(clientCertFile.getAbsolutePath(), clientKeyFile.getAbsolutePath(), proxyCaFile.getAbsolutePath())
                .build();

        // 5. Build client (let builder/factory handle trust)
        HttpClient client = new HttpClientImpl.Builder()
                .setProxy(proxy)
                .setUrlSanitizer(mUrlSanitizerMock)
                .build();

        // 6. Make a request to the origin server (should tunnel via proxy)
        URI uri = originServer.url("/test").uri();
        HttpRequest req = client.request(uri, HttpMethod.GET);
        HttpResponse resp = req.execute();
        assertNotNull(resp);
        assertEquals(200, resp.getHttpStatus());
        assertEquals("from origin!", resp.getData());

        // Assert that the tunnel was established and the origin received the request
        assertTrue("TunnelProxy did not tunnel the request in time", tunnelProxy.getTunnelLatch().await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertTrue("Origin server did not receive the request in time", originLatch.await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertEquals("GET", methodAndPath[0]);
        assertEquals("/test", methodAndPath[1]);

        tunnelProxy.stopProxy();
        tunnelProxy.join();
        originServer.shutdown();
        proxyCertFile.delete();
        proxyKeyFile.delete();
        proxyCaFile.delete();
        clientCertFile.delete();
        clientKeyFile.delete();
    }

    // Helper to create SSLSocketFactory from HeldCertificate
    private static SSLSocketFactory createSslSocketFactory(HeldCertificate cert) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry("key", cert.keyPair().getPrivate(), "password".toCharArray(), new java.security.cert.Certificate[]{cert.certificate()});

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, "password".toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);

        return sslContext.getSocketFactory();
    }

    /**
     * TunnelProxySsl is a minimal SSL/TLS proxy supporting mTLS (client authentication).
     * It uses an SSLServerSocket and requires client certificates signed by the provided CA.
     * Only for use in mTLS proxy integration tests.
     */
    private static class TunnelProxySsl extends TunnelProxy {
        private final HeldCertificate mServerCert;
        private final HeldCertificate mClientCa;
        private final AtomicBoolean mRunning = new AtomicBoolean(true);

        public TunnelProxySsl(int port, HeldCertificate serverCert, HeldCertificate clientCa) {
            super(port);
            this.mServerCert = serverCert;
            this.mClientCa = clientCa;
        }
        @Override
        public void run() {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                KeyStore ks = KeyStore.getInstance("PKCS12");
                ks.load(null, null);
                ks.setKeyEntry("key", mServerCert.keyPair().getPrivate(), "password".toCharArray(), new java.security.cert.Certificate[]{mServerCert.certificate()});
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(ks, "password".toCharArray());
                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null, null);
                trustStore.setCertificateEntry("ca", mClientCa.certificate());
                javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory.getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(trustStore);
                sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
                javax.net.ssl.SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
                mServerSocket = factory.createServerSocket(mPort);
                ((javax.net.ssl.SSLServerSocket) mServerSocket).setNeedClientAuth(true);
                System.out.println("[TunnelProxySsl] Listening on port: " + mServerSocket.getLocalPort());
                while (mRunning.get()) {
                    Socket client = mServerSocket.accept();
                    System.out.println("[TunnelProxySsl] Accepted connection from: " + client.getRemoteSocketAddress());
                    new Thread(() -> handle(client)).start();
                }
            } catch (IOException e) {
                System.out.println("[TunnelProxySsl] Server socket closed or error: " + e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Minimal CONNECT-capable proxy for HTTPS tunneling in tests.
     * Listens on a port, accepts CONNECT requests, and pipes bytes between the client and the requested target.
     * Used to simulate a real HTTPS proxy for end-to-end CA trust validation.
     */
    private static class TunnelProxy extends Thread {
        // Latch to signal that a CONNECT tunnel was established
        private final CountDownLatch mTunnelLatch = new CountDownLatch(1);
        // Port to listen on (0 = auto-assign)
        protected final int mPort;
        // The server socket for accepting connections
        public ServerSocket mServerSocket;
        // Flag to control proxy shutdown
        private final AtomicBoolean mRunning = new AtomicBoolean(true);

        /**
         * Create a new TunnelProxy listening on the given port.
         * @param port Port to listen on (0 = auto-assign)
         */
        TunnelProxy(int port) { mPort = port; }

        /**
         * Main accept loop. For each incoming client, start a handler thread.
         */
        public void run() {
            try {
                mServerSocket = new ServerSocket(mPort);
                System.out.println("[TunnelProxy] Listening on port: " + mServerSocket.getLocalPort());
                while (mRunning.get()) {
                    Socket client = mServerSocket.accept();
                    System.out.println("[TunnelProxy] Accepted connection from: " + client.getRemoteSocketAddress());
                    // Each client handled in its own thread
                    new Thread(() -> handle(client)).start();
                }
            } catch (IOException ignored) {
                System.out.println("[TunnelProxy] Server socket closed or error: " + ignored);
            }
        }

        /**
         * Handles a single client connection. Waits for CONNECT, then establishes a tunnel.
         */
        void handle(Socket client) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                 OutputStream out = client.getOutputStream()) {
                String line = in.readLine();
                // Only handle CONNECT requests (as sent by HTTPS clients to a proxy)
                if (line != null && line.startsWith("CONNECT")) {
                    mTunnelLatch.countDown();
                    System.out.println("[TunnelProxy] Received CONNECT: " + line);
                    out.write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes());
                    out.flush();
                    String[] parts = line.split(" ");
                    String[] hostPort = parts[1].split(":");
                    // Open a socket to the requested target (origin server)
                    Socket target = new Socket(hostPort[0], Integer.parseInt(hostPort[1]));
                    System.out.println("[TunnelProxy] Established tunnel to: " + hostPort[0] + ":" + hostPort[1]);
                    // Pipe bytes in both directions (client <-> target) until closed
                    Thread t1 = new Thread(() -> {
                        try {
                            pipe(client, target);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    Thread t2 = new Thread(() -> {
                        try {
                            pipe(target, client);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    t1.start(); t2.start();
                    try { t1.join(); t2.join(); } catch (InterruptedException ignored) {}
                    System.out.println("[TunnelProxy] Tunnel closed for: " + hostPort[0] + ":" + hostPort[1]);
                    target.close();
                }
            } catch (Exception ignored) { }
        }

        /**
         * Copies bytes from inSocket to outSocket until EOF.
         * Used to relay data in both directions for the tunnel.
         */
        private void pipe(Socket inSocket, Socket outSocket) throws IOException {
            try (InputStream in = inSocket.getInputStream(); OutputStream out = outSocket.getOutputStream()) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                    out.flush();
                }
            } catch (IOException ignored) { }
        }

        /**
         * Stops the proxy by closing the server socket and setting the running flag to false.
         */
        public void stopProxy() throws IOException {
            mRunning.set(false);
            if (mServerSocket != null && !mServerSocket.isClosed()) {
                mServerSocket.close();
                System.out.println("[TunnelProxy] Proxy stopped.");
            }
        }

        public CountDownLatch getTunnelLatch() {
            return mTunnelLatch;
        }
    }
}
