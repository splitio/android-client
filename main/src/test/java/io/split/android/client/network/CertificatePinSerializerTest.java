package io.split.android.client.network;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

public class CertificatePinSerializerTest {

    private Gson mGson;

    @Before
    public void setUp() {
        mGson = new GsonBuilder()
                .registerTypeAdapter(CertificatePin.class, new CertificatePinSerializer())
                .create();
    }

    @Test
    public void serializeSinglePin() {
        CertificatePin pin = new CertificatePin(new byte[]{1, 2, 3}, "sha256");

        String json = mGson.toJson(pin);

        assertEquals("{\"algo\":\"sha256\",\"pin\":[1,2,3]}", json);
    }

    @Test
    public void serializeNegativeByteValues() {
        CertificatePin pin = new CertificatePin(new byte[]{-80, 50, -99, -126, 11}, "sha256");

        String json = mGson.toJson(pin);

        assertEquals("{\"algo\":\"sha256\",\"pin\":[-80,50,-99,-126,11]}", json);
    }

    @Test
    public void deserializeSinglePin() {
        String json = "{\"algo\":\"sha1\",\"pin\":[-116,-73,-94,-80,55]}";

        CertificatePin pin = mGson.fromJson(json, CertificatePin.class);

        assertNotNull(pin);
        assertEquals("sha1", pin.getAlgorithm());
        assertArrayEquals(new byte[]{-116, -73, -94, -80, 55}, pin.getPin());
    }

    @Test
    public void roundTripPreservesData() {
        CertificatePin original = new CertificatePin(new byte[]{-116, -123, 30, -25}, "sha256");

        String json = mGson.toJson(original);
        CertificatePin deserialized = mGson.fromJson(json, CertificatePin.class);

        assertNotNull(deserialized);
        assertEquals(original.getAlgorithm(), deserialized.getAlgorithm());
        assertArrayEquals(original.getPin(), deserialized.getPin());
    }

    @Test
    public void roundTripMapOfSets() {
        String expectedJson = "{\"events.split.io\":[{\"algo\":\"sha256\",\"pin\":[-80,50,-99,-126,11]},{\"algo\":\"sha1\",\"pin\":[-116,-73,-94,-80,55]}],\"sdk.split.io\":[{\"algo\":\"sha256\",\"pin\":[-116,-123,30,-25]}]}";

        Type type = new TypeToken<Map<String, Set<CertificatePin>>>() {
        }.getType();
        Map<String, Set<CertificatePin>> deserialized = mGson.fromJson(expectedJson, type);

        assertNotNull(deserialized);
        assertEquals(2, deserialized.size());
        assertEquals(2, deserialized.get("events.split.io").size());
        assertEquals(1, deserialized.get("sdk.split.io").size());

        // Re-serialize and deserialize 
        String reserialized = mGson.toJson(deserialized, type);
        Map<String, Set<CertificatePin>> roundTripped = mGson.fromJson(reserialized, type);

        assertNotNull(roundTripped);
        assertEquals(deserialized.size(), roundTripped.size());
        for (Map.Entry<String, Set<CertificatePin>> entry : deserialized.entrySet()) {
            Set<CertificatePin> originalPins = entry.getValue();
            Set<CertificatePin> roundTrippedPins = roundTripped.get(entry.getKey());
            assertNotNull(roundTrippedPins);
            assertEquals(originalPins.size(), roundTrippedPins.size());
            assertEquals(originalPins, roundTrippedPins);
        }
    }

    @Test
    public void deserializeWithUnknownFieldsIsIgnored() {
        String json = "{\"algo\":\"sha256\",\"pin\":[1,2],\"extra\":\"ignored\"}";

        CertificatePin pin = mGson.fromJson(json, CertificatePin.class);

        assertNotNull(pin);
        assertEquals("sha256", pin.getAlgorithm());
        assertArrayEquals(new byte[]{1, 2}, pin.getPin());
    }

    @Test
    public void deserializeMissingFieldsResultsInNulls() {
        String json = "{}";

        CertificatePin pin = mGson.fromJson(json, CertificatePin.class);

        assertNotNull(pin);
        assertNull(pin.getAlgorithm());
        assertNull(pin.getPin());
    }

    @Test
    public void serializeEmptyPinArray() {
        CertificatePin pin = new CertificatePin(new byte[]{}, "sha256");

        String json = mGson.toJson(pin);

        assertEquals("{\"algo\":\"sha256\",\"pin\":[]}", json);
    }
}
