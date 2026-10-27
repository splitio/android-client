package io.split.android.client.perf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

public class PerfReportTest {

    private static JsonObject device() {
        JsonObject device = new JsonObject();
        device.addProperty("model", "Pixel");
        return device;
    }

    private static JsonObject manifest() {
        JsonObject manifest = new JsonObject();
        manifest.addProperty("ciphertextBytes", 42);
        return manifest;
    }

    private static String report(Long firstFlagsSince) {
        return PerfReport.toJson("abc1234", 3, "enc1-n1000-sets0", 1000, 50, true, false, 500,
                device(), 4321, 2_500_000L, 12_000_000L, 800_000_000L, "cache", firstFlagsSince, 1000,
                manifest());
    }

    @Test
    public void writesSchemaAndIdentityFields() {
        JsonObject json = JsonParser.parseString(report(1L)).getAsJsonObject();
        assertEquals("split-perf/1", json.get("schema").getAsString());
        assertEquals("abc1234", json.get("git").getAsString());
        assertEquals(3, json.get("run").getAsInt());
        assertEquals(4321, json.get("pid").getAsInt());
        assertEquals("Pixel", json.getAsJsonObject("device").get("model").getAsString());
        assertEquals(42, json.getAsJsonObject("seedManifest").get("ciphertextBytes").getAsInt());
    }

    @Test
    public void writesScenarioFields() {
        JsonObject scenario = JsonParser.parseString(report(1L)).getAsJsonObject().getAsJsonObject("scenario");
        assertEquals("enc1-n1000-sets0", scenario.get("seed").getAsString());
        assertTrue(scenario.get("enc").getAsBoolean());
        assertEquals(1000, scenario.get("flags").getAsInt());
        assertEquals(50, scenario.get("rbs").getAsInt());
        assertFalse(scenario.get("sets").getAsBoolean());
        assertEquals(500, scenario.get("latencyMs").getAsInt());
    }

    @Test
    public void convertsNanosecondsToMilliseconds() {
        JsonObject json = JsonParser.parseString(report(1L)).getAsJsonObject();
        assertEquals(2.5, json.get("buildMs").getAsDouble(), 0.0);
        assertEquals(12.0, json.get("readyFromCacheMs").getAsDouble(), 0.0);
        assertEquals(800.0, json.get("readyMs").getAsDouble(), 0.0);
    }

    @Test
    public void writesPathAndSplitNames() {
        JsonObject json = JsonParser.parseString(report(1700000000000L)).getAsJsonObject();
        assertEquals("cache", json.get("readyFromCachePath").getAsString());
        assertEquals(1700000000000L, json.get("firstFlagsSince").getAsLong());
        assertEquals(1000, json.get("splitNames").getAsInt());
    }

    @Test
    public void nullFirstSinceIsWrittenAsJsonNull() {
        String text = report(null);
        assertTrue(text, text.contains("\"firstFlagsSince\":null"));
        assertTrue(JsonParser.parseString(text).getAsJsonObject().get("firstFlagsSince").isJsonNull());
    }

    @Test
    public void reportIsOneLine() {
        String text = report(1L);
        assertFalse(text.contains("\n"));
        assertFalse(text.contains("\r"));
    }
}
