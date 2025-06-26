package io.split.android.client.network;

import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.security.KeyStore;

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

    @Test
    public void creatingWithValidMtlsParamsThrowsUntilImplemented() throws Exception {
        // Generate CA and client cert/key
        HeldCertificate ca = new HeldCertificate.Builder()
                .commonName("Test CA")
                .certificateAuthority(0)
                .build();
        HeldCertificate client = new HeldCertificate.Builder()
                .commonName("Test Client")
                .signedBy(ca)
                .build();
        File caCertFile = tempFolder.newFile("mtls-ca.pem");
        File clientP12File = tempFolder.newFile("mtls-client.p12");
        
        try (FileWriter writer = new FileWriter(caCertFile)) {
            writer.write(ca.certificatePem());
        }
        
        // Create PKCS#12 keystore with client cert and key
        KeyStore p12KeyStore = KeyStore.getInstance("PKCS12");
        p12KeyStore.load(null, null);
        p12KeyStore.setKeyEntry("client", client.keyPair().getPrivate(), "password".toCharArray(), 
                new java.security.cert.Certificate[]{client.certificate()});
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(clientP12File)) {
            p12KeyStore.store(fos, "password".toCharArray());
        }
        
        ProxySslContextFactoryImpl factory = new ProxySslContextFactoryImpl();
        try (FileInputStream caFis = new FileInputStream(caCertFile);
             FileInputStream clientP12Fis = new FileInputStream(clientP12File)) {
            assertNotNull(factory.create(caFis, clientP12Fis, "password"));
            factory.create(caFis, clientP12Fis, "password");
        }
    }
}
