package io.split.android.client.telemetry;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import io.split.android.client.telemetry.model.Config;
import io.split.android.client.telemetry.model.ImpressionsMode;
import io.split.android.client.telemetry.model.RefreshRates;
import io.split.android.client.telemetry.model.UrlOverrides;

public class TelemetryConfigBodySerializerTest {

    private TelemetryConfigBodySerializer telemetryConfigBodySerializer;

    @Before
    public void setUp() {
        telemetryConfigBodySerializer = new TelemetryConfigBodySerializer();
    }

    @Test
    public void jsonIsBuiltAsExpected() {

        final String expectedJson = "{\"oM\":0,\"st\":\"memory\",\"sE\":true,\"rR\":{\"sp\":4000,\"se\":5000,\"im\":3000,\"ev\":2000,\"te\":1000},\"uO\":{\"s\":true,\"e\":true,\"a\":true,\"st\":true,\"t\":true},\"iQ\":4000,\"eQ\":3000,\"iM\":\"1\",\"iL\":true,\"hP\":true,\"aF\":1,\"rF\":0,\"tR\":300,\"nR\":3,\"t\":[\"tag1\",\"tag2\"],\"i\":[\"integration1\",\"integration2\"]}";
        final String serializedConfig = telemetryConfigBodySerializer.serialize(buildMockConfig());

        assertEquals(expectedJson, serializedConfig);
    }

    @Test
    public void nullValuesAreIgnoredForJson() {

        final String expectedJson = "{\"oM\":0,\"st\":\"memory\",\"sE\":true,\"iQ\":4000,\"eQ\":3000,\"iM\":\"1\",\"iL\":true,\"hP\":true,\"aF\":1,\"rF\":0,\"tR\":300,\"nR\":3,\"t\":[\"tag1\",\"tag2\"],\"i\":[\"integration1\",\"integration2\"]}";
        final String serializedConfig = telemetryConfigBodySerializer.serialize(buildMockConfigWithNulls());

        assertEquals(expectedJson, serializedConfig);
    }

    private Config buildMockConfig() {
        Config config = new Config();

        RefreshRates refreshRates = new RefreshRates();
        refreshRates.setTelemetry(1000);
        refreshRates.setEvents(2000);
        refreshRates.setImpressions(3000);
        refreshRates.setSplits(4000);
        refreshRates.setMySegments(5000);

        UrlOverrides urlOverrides = new UrlOverrides();
        urlOverrides.setTelemetry(true);
        urlOverrides.setSdkUrl(true);
        urlOverrides.setEvents(true);
        urlOverrides.setStream(true);
        urlOverrides.setAuth(true);

        config.setStreamingEnabled(true);
        config.setRefreshRates(refreshRates);
        config.setUrlOverrides(urlOverrides);
        config.setImpressionsQueueSize(4000);
        config.setEventsQueueSize(3000);
        config.setImpressionsMode(ImpressionsMode.DEBUG);
        config.setImpressionsListenerEnabled(true);
        config.setHttpProxyDetected(true);
        config.setActiveFactories(1);
        config.setRedundantActiveFactories(0);
        config.setTimeUntilSDKReady(300);
        config.setSDKNotReadyUsage(3);
        config.setTags(Arrays.asList("tag1", "tag2"));
        config.setIntegrations(Arrays.asList("integration1", "integration2"));

        return config;
    }

    private Config buildMockConfigWithNulls() {
        Config config = new Config();

        config.setStreamingEnabled(true);
        config.setImpressionsQueueSize(4000);
        config.setEventsQueueSize(3000);
        config.setImpressionsMode(ImpressionsMode.DEBUG);
        config.setImpressionsListenerEnabled(true);
        config.setHttpProxyDetected(true);
        config.setActiveFactories(1);
        config.setRedundantActiveFactories(0);
        config.setTimeUntilSDKReady(300);
        config.setSDKNotReadyUsage(3);
        config.setTags(Arrays.asList("tag1", "tag2"));
        config.setIntegrations(Arrays.asList("integration1", "integration2"));

        return config;
    }
}
