package io.split.android.client.network;

import static org.junit.Assert.assertNotNull;

import androidx.annotation.NonNull;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Base64;

import javax.net.ssl.SSLSocketFactory;

import okhttp3.tls.HeldCertificate;

public class ProxySslSocketFactoryProviderImplTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private final Base64Decoder mBase64Decoder = new Base64Decoder() {
        @Override
        public byte[] decode(String base64) {
            return Base64.getDecoder().decode(base64);
        }
    };

    @Test
    public void creatingWithValidCaCertCreatesSocketFactory() throws Exception {
        HeldCertificate ca = getCaCert();
        File caCertFile = tempFolder.newFile("held-ca.pem");
        try (FileWriter writer = new FileWriter(caCertFile)) {
            writer.write(ca.certificatePem());
        }
        ProxySslSocketFactoryProviderImpl provider = getProvider();
        try (FileInputStream fis = new FileInputStream(caCertFile)) {
            SSLSocketFactory socketFactory = provider.create(fis);
            assertNotNull(socketFactory);
        }
    }

    @Ignore("Robolectric conflic in CI")
    @Test(expected = Exception.class)
    public void creatingWithInvalidCaCertThrows() throws Exception {
        File caCertFile = tempFolder.newFile("invalid-ca.pem");
        try (FileWriter writer = new FileWriter(caCertFile)) {
            writer.write("not a cert");
        }
        ProxySslSocketFactoryProviderImpl provider = getProvider();
        try (FileInputStream fis = new FileInputStream(caCertFile)) {
            provider.create(fis);
        }
    }

    @Test
    public void creatingWithValidMtlsParamsCreatesSocketFactory() throws Exception {
        // Create CA cert and client cert & key
        HeldCertificate ca = getCaCert();
        HeldCertificate clientCert = getClientCert(ca);
        File caCertFile = createCaCertFile(ca);
        File clientCertFile = tempFolder.newFile("client.crt");
        File clientKeyFile = tempFolder.newFile("client.key");

        // Write client certificate and key to separate files
        try (FileWriter writer = new FileWriter(clientCertFile)) {
            writer.write(clientCert.certificatePem());
        }
        try (FileWriter writer = new FileWriter(clientKeyFile)) {
            writer.write(clientCert.privateKeyPkcs8Pem());
        }

        // Create socket factory
        ProxySslSocketFactoryProviderImpl factory = new ProxySslSocketFactoryProviderImpl(mBase64Decoder);
        SSLSocketFactory sslSocketFactory;
        try (FileInputStream caCertStream = new FileInputStream(caCertFile);
             FileInputStream clientCertStream = new FileInputStream(clientCertFile);
             FileInputStream clientKeyStream = new FileInputStream(clientKeyFile)) {
            sslSocketFactory = factory.create(caCertStream, clientCertStream, clientKeyStream);
        }

        assertNotNull(sslSocketFactory);
    }

    @Test(expected = Exception.class)
    public void creatingWithInvalidMtlsParamsThrows() throws Exception {
        // Create valid CA cert but invalid client cert/key files
        HeldCertificate ca = getCaCert();
        File caCertFile = createCaCertFile(ca);
        File invalidClientCertFile = tempFolder.newFile("invalid-client.crt");
        File invalidClientKeyFile = tempFolder.newFile("invalid-client.key");

        // Write invalid data to cert and key files
        try (FileWriter writer = new FileWriter(invalidClientCertFile)) {
            writer.write("invalid certificate");
        }
        try (FileWriter writer = new FileWriter(invalidClientKeyFile)) {
            writer.write("invalid key");
        }

        ProxySslSocketFactoryProviderImpl provider = getProvider();
        try (FileInputStream caCertStream = new FileInputStream(caCertFile);
             FileInputStream invalidClientCertStream = new FileInputStream(invalidClientCertFile);
             FileInputStream invalidClientKeyStream = new FileInputStream(invalidClientKeyFile)) {
            provider.create(caCertStream, invalidClientCertStream, invalidClientKeyStream);
        }
    }

    private File createCaCertFile(HeldCertificate ca) throws Exception {
        File caCertFile = tempFolder.newFile("mtls-ca.pem");
        try (FileWriter writer = new FileWriter(caCertFile)) {
            writer.write(ca.certificatePem());
        }
        return caCertFile;
    }

    @NonNull
    private static HeldCertificate getCaCert() {
        return new HeldCertificate.Builder()
                .commonName("Test CA")
                .certificateAuthority(0)
                .build();
    }

    @NonNull
    private static HeldCertificate getClientCert(HeldCertificate ca) {
        return new HeldCertificate.Builder()
                .commonName("Test Client")
                .signedBy(ca)
                .build();
    }

    @NonNull
    private ProxySslSocketFactoryProviderImpl getProvider() {
        return new ProxySslSocketFactoryProviderImpl(mBase64Decoder);
    }
}
