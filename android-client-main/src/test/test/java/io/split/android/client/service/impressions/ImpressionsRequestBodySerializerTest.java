package io.split.android.client.service.impressions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.split.android.client.dtos.KeyImpression;

public class ImpressionsRequestBodySerializerTest {

    private ImpressionsRequestBodySerializer mSerializer;

    @Before
    public void setUp() {
        mSerializer = new ImpressionsRequestBodySerializer();
    }

    @Test
    public void impressionWithoutPropertiesDoesNotIncludePropertiesField() {
        KeyImpression impression = createBasicImpression("user123", "test_feature", "on");
        impression.changeNumber = 1234567L;
        impression.label = "default rule";
        impression.bucketingKey = "bucketKey";

        String serialized = serialize(impression);

        String expected =
            "[{" +
                "\"f\":\"test_feature\"," +
                "\"i\":[{" +
                    "\"k\":\"user123\"," +
                    "\"b\":\"bucketKey\"," +
                    "\"t\":\"on\"," +
                    "\"r\":\"default rule\"," +
                    "\"m\":1650000000," +
                    "\"c\":1234567," +
                    "\"pt\":null" +
                "}]" +
            "}]";

        assertEquals(expected, serialized);
    }

    @Test
    public void serializeImpressionWithProperties() {
        KeyImpression impression = createBasicImpression("user123", "test_feature", "on");
        impression.properties = "{\"string_prop\":\"value\",\"number_prop\":42,\"bool_prop\":true}";

        String serialized = serialize(impression);

        String expected =
            "[{" +
                "\"f\":\"test_feature\"," +
                "\"i\":[{" +
                    "\"k\":\"user123\"," +
                    "\"b\":null," +
                    "\"t\":\"on\"," +
                    "\"r\":null," +
                    "\"m\":1650000000," +
                    "\"c\":null," +
                    "\"pt\":null," +
                    "\"properties\":\"{\\\"string_prop\\\":\\\"value\\\",\\\"number_prop\\\":42,\\\"bool_prop\\\":true}\"" +
                "}]" +
            "}]";

        assertEquals(expected, serialized);
    }

    @Test
    public void serializeMultipleImpressionsGroupedByFeature() {
        KeyImpression impression1 = createBasicImpression("user1", "feature1", "on");
        KeyImpression impression2 = createBasicImpression("user2", "feature1", "off");
        KeyImpression impression3 = createBasicImpression("user1", "feature2", "control");
        
        impression1.time = 1000L;
        impression2.time = 2000L;
        impression3.time = 3000L;

        String serialized = serialize(impression1, impression2, impression3);

        assertTrue(serialized.contains("\"f\":\"feature1\""));
        assertTrue(serialized.contains("\"f\":\"feature2\""));
        assertTrue(serialized.contains("\"k\":\"user1\""));
        assertTrue(serialized.contains("\"k\":\"user2\""));
        assertTrue(serialized.contains("\"t\":\"on\""));
        assertTrue(serialized.contains("\"t\":\"off\""));
        assertTrue(serialized.contains("\"t\":\"control\""));
        assertTrue(serialized.contains("\"m\":1000"));
        assertTrue(serialized.contains("\"m\":2000"));
        assertTrue(serialized.contains("\"m\":3000"));
    }

    @Test
    public void serializeEmptyImpressionsList() {
        String serialized = serialize();
        assertEquals("[]", serialized);
    }

    /**
     * Helper method to create a basic KeyImpression with common fields
     */
    private KeyImpression createBasicImpression(String keyName, String feature, String treatment) {
        KeyImpression impression = new KeyImpression();
        impression.keyName = keyName;
        impression.feature = feature;
        impression.treatment = treatment;
        impression.time = 1650000000L;
        return impression;
    }

    /**
     * Helper method to serialize impressions
     */
    private String serialize(KeyImpression... impressions) {
        List<KeyImpression> impressionsList = new ArrayList<>(Arrays.asList(impressions));

        return mSerializer.serialize(impressionsList);
    }
}
