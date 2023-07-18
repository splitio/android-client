package io.split.android.client.telemetry.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import io.split.android.client.ServiceEndpoints;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.impressions.Impression;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.service.impressions.ImpressionsMode;
import io.split.android.client.shared.UserConsent;
import io.split.android.client.telemetry.model.Config;

public class TelemetryConfigProviderImplTest {

    private TelemetryConfigProvider mTelemetryConfigProvider;
    private TelemetryStorageConsumer mTelemetryStorageConsumer = mock(TelemetryStorageConsumer.class);

    @Test
    public void test() {

        int splitRefreshRate = 101;
        int impRefreshRate = 102;
        int segmentsRate = 103;
        int telemetryRefreshRate = 104;
        int eventsRate = 105;
        SplitClientConfig mSplitClientConfig = new SplitClientConfig.Builder()
                .streamingEnabled(true)
                .featuresRefreshRate(splitRefreshRate)
                .impressionsRefreshRate(impRefreshRate)
                .segmentsRefreshRate(segmentsRate)
                .telemetryRefreshRate(telemetryRefreshRate)
                .eventFlushInterval(eventsRate)
                .proxyHost("proxyHost")
                .serviceEndpoints(ServiceEndpoints.builder()
                        .apiEndpoint("asdas")
                        .eventsEndpoint("asdas")
                        .sseAuthServiceEndpoint("asdas")
                        .streamingServiceEndpoint("asdas")
                        .telemetryServiceEndpoint("asdas")
                        .build())
                .impressionsQueueSize(200)
                .eventsQueueSize(300)
                .userConsent(UserConsent.DECLINED)
                .impressionsMode(ImpressionsMode.OPTIMIZED)
                .impressionListener(new ImpressionListener() {
                    @Override
                    public void log(Impression impression) {

                    }

                    @Override
                    public void close() {

                    }
                })
                .build();
        mTelemetryConfigProvider = new TelemetryConfigProviderImpl(mTelemetryStorageConsumer, mSplitClientConfig);

        Config configTelemetry = mTelemetryConfigProvider.getConfigTelemetry();

        assertTrue(mSplitClientConfig.streamingEnabled());
        assertEquals(splitRefreshRate, configTelemetry.getRefreshRates().getSplits());
        assertEquals(impRefreshRate, configTelemetry.getRefreshRates().getImpressions());
        assertEquals(segmentsRate, configTelemetry.getRefreshRates().getMySegments());
        assertEquals(telemetryRefreshRate, configTelemetry.getRefreshRates().getTelemetry());
        assertEquals(eventsRate, configTelemetry.getRefreshRates().getEvents());
        assertTrue(configTelemetry.isHttpProxyDetected());
        assertEquals(200, configTelemetry.getImpressionsQueueSize());
        assertEquals(300, configTelemetry.getEventsQueueSize());
        assertEquals(3, configTelemetry.getUserConsent());
        assertEquals(io.split.android.client.telemetry.model.ImpressionsMode.OPTIMIZED.intValue(), configTelemetry.getImpressionsMode());
        assertTrue(configTelemetry.isImpressionsListenerEnabled());
        assertTrue(configTelemetry.getUrlOverrides().isTelemetry());
        assertTrue(configTelemetry.getUrlOverrides().isSdkUrl());
        assertTrue(configTelemetry.getUrlOverrides().isEvents());
        assertTrue(configTelemetry.getUrlOverrides().isAuth());
        assertTrue(configTelemetry.getUrlOverrides().isStream());
    }
}
