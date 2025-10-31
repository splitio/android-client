package io.split.android.client.network;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.annotation.Nullable;

import org.junit.Test;

import java.util.Iterator;

public class CertificatePinningConfigurationProviderTest {

    @Test
    public void nullJsonReturnsNull() {
        CertificatePinningConfiguration config = getConfig(null);

        assertNull(config);
    }

    @Test
    public void emptyJsonReturnsNull() {
        CertificatePinningConfiguration config = getConfig("");

        assertNull(config);
    }

    @Test
    public void blankJsonReturnsNull() {
        CertificatePinningConfiguration config = getConfig(" ");

        assertNull(config);
    }

    @Test
    public void invalidJsonReturnsNull() {
        CertificatePinningConfiguration config = getConfig("invalid");

        assertNull(config);
    }

    @Test
    public void validJsonReturnsConfig() {
        CertificatePinningConfiguration config = getConfig(
                "{\"events.split.io\":[{\"algo\":\"sha256\",\"pin\":[-80,50,-99,-126,11]},{\"algo\":\"sha1\",\"pin\":[-116,-73,-94,-80,55]}],\"sdk.split.io\":[{\"algo\":\"sha256\",\"pin\":[-116,-123,30,-25]}]}");

        assertEquals(2, config.getPins().size());
        assertEquals(2, config.getPins().get("events.split.io").size());
        assertEquals(1, config.getPins().get("sdk.split.io").size());
        Iterator<CertificatePin> eventsIterator = config.getPins().get("events.split.io").iterator();
        CertificatePin firstEventsPin = eventsIterator.next();
        CertificatePin secondEventsPin = eventsIterator.next();
        CertificatePin sdkPin = config.getPins().get("sdk.split.io").iterator().next();
        assertArrayEquals(new byte[]{-80,50,-99,-126,11}, firstEventsPin.getPin());
        assertArrayEquals(new byte[]{-116,-123,30,-25}, sdkPin.getPin());
        assertArrayEquals(new byte[]{-116,-73,-94,-80,55}, secondEventsPin.getPin());
        assertEquals("sha256", firstEventsPin.getAlgorithm());
        assertEquals("sha1", secondEventsPin.getAlgorithm());
        assertEquals("sha256", sdkPin.getAlgorithm());
    }

    @Nullable
    private static CertificatePinningConfiguration getConfig(String pinsJson) {
        return CertificatePinningConfigurationProvider.getCertificatePinningConfiguration(pinsJson);
    }
}
