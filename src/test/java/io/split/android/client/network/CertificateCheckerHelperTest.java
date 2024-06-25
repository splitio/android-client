package io.split.android.client.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
}
