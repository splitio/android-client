package io.split.android.client.network;

import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;

import javax.net.ssl.SSLSocketFactory;

import okhttp3.tls.HeldCertificate;

public class ProxySslContextFactoryImplTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void creatingWithValidCaCertCreatesSocketFactory() throws Exception {
        // OkHttp's HeldCertificate is available in test scope
        HeldCertificate ca = new HeldCertificate.Builder()
                .commonName("Test CA")
                .certificateAuthority(0)
                .build();
        File caCertFile = tempFolder.newFile("held-ca.pem");
        try (FileWriter writer = new FileWriter(caCertFile)) {
            writer.write(ca.certificatePem());
        }
        ProxySslContextFactoryImpl factory = new ProxySslContextFactoryImpl();
        try (FileInputStream fis = new FileInputStream(caCertFile)) {
            SSLSocketFactory socketFactory = factory.create(fis);
            assertNotNull(socketFactory);
        }
    }

    @Test(expected = Exception.class)
    public void creatingWithInvalidCaCertThrows() throws Exception {
        File caCertFile = tempFolder.newFile("invalid-ca.pem");
        try (FileWriter writer = new FileWriter(caCertFile)) {
            writer.write("not a cert");
        }
        ProxySslContextFactoryImpl factory = new ProxySslContextFactoryImpl();
        try (FileInputStream fis = new FileInputStream(caCertFile)) {
            factory.create(fis);
        }
    }
}
