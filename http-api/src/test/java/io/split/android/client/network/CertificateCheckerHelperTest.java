package io.split.android.client.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import org.junit.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import okhttp3.tls.HeldCertificate;

public class CertificateCheckerHelperTest {

    @Test
    public void getPinsForHost() {
        Map<String, Set<CertificatePin>> pins = new HashMap<>();
        Set<CertificatePin> pins1 = Collections.singleton(new CertificatePin(new byte[]{1, 2}, "sha256"));
        Set<CertificatePin> pins2 = Collections.singleton(new CertificatePin(new byte[]{3, 4}, "sha256"));
        Set<CertificatePin> pins4 = Collections.singleton(new CertificatePin(new byte[]{7, 8}, "sha256"));
        pins.put("*.example.com", pins1);
        pins.put("**.example.com", pins2);
        pins.put("www.sub.example.com", pins4);

        Set<CertificatePin> result1 = CertificateCheckerHelper.getPinsForHost("sub.example.com", pins);
        Set<CertificatePin> result2 = CertificateCheckerHelper.getPinsForHost("www.sub.example.com", pins);
        Set<CertificatePin> result4 = CertificateCheckerHelper.getPinsForHost("*.", pins);
        Set<CertificatePin> result5 = CertificateCheckerHelper.getPinsForHost("**.", pins);

        // sub.example.com matches with *.example.com & **.example.com
        assertEquals(2, result1.size());
        assertTrue(result1.contains(new CertificatePin(new byte[]{1, 2}, "sha256")));
        assertTrue(result1.contains(new CertificatePin(new byte[]{3, 4}, "sha256")));

        // www.sub.example.com matchers with **.example.com & www.sub.example.com
        assertEquals(2, result2.size());
        assertTrue(result2.contains(new CertificatePin(new byte[]{3, 4}, "sha256")));
        assertTrue(result2.contains(new CertificatePin(new byte[]{7, 8}, "sha256")));

        assertNull(result4);
        assertNull(result5);
    }

    @Test
    public void getPinsFromInputStream() throws IOException {
        X509Certificate[] certificateChain = generateCertificateChain();
        Path certFilePath = Paths.get("generated_certificate.cer");
        writeCertificateToFile(certFilePath, certificateChain);

        try (InputStream certInputStream = Files.newInputStream(certFilePath)) {
            Set<CertificatePin> pinsFromInputStream = CertificateCheckerHelper
                    .getPinsFromInputStream(certInputStream, new PinEncoderImpl());
            assertEquals(3, pinsFromInputStream.size());
            assertTrue(pinsFromInputStream.stream().allMatch(
                    certificatePin -> certificatePin.getAlgorithm().equals("sha256")));
        } finally {
            // delete file
            Files.delete(certFilePath);
        }
    }

    @NonNull
    private static X509Certificate[] generateCertificateChain() {
        HeldCertificate rootCertificate = new HeldCertificate.Builder()
                .certificateAuthority(1)
                .commonName("Root CA")
                .build();

        HeldCertificate intermediateCertificate = new HeldCertificate.Builder()
                .certificateAuthority(0)
                .commonName("Intermediate CA")
                .signedBy(rootCertificate)
                .build();

        HeldCertificate endEntityCertificate = new HeldCertificate.Builder()
                .commonName("example.com")
                .signedBy(intermediateCertificate)
                .build();

        return new X509Certificate[]{
                endEntityCertificate.certificate(),
                intermediateCertificate.certificate(),
                rootCertificate.certificate()
        };
    }

    private static void writeCertificateToFile(Path certFilePath, X509Certificate[] certificateChain) {
        try (FileOutputStream fos = new FileOutputStream(certFilePath.toFile())) {
            for (X509Certificate cert : certificateChain) {
                fos.write(cert.getEncoded());
            }
        } catch (IOException | CertificateEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
