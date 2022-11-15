package tests.integration.telemetry;

import static org.junit.Assert.assertTrue;
import static java.lang.Thread.sleep;

import org.junit.Test;

import java.io.IOException;
import java.util.List;

import helper.TestableSplitConfigBuilder;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.storage.db.StorageFactory;
import io.split.android.client.telemetry.model.streaming.OccupancyPriStreamingEvent;
import io.split.android.client.telemetry.model.streaming.OccupancySecStreamingEvent;
import io.split.android.client.telemetry.model.streaming.StreamingEvent;
import io.split.android.client.telemetry.model.streaming.TokenRefreshStreamingEvent;
import io.split.android.client.telemetry.storage.TelemetryStorage;
import tests.integration.streaming.OccupancyBaseTest;

public class TelemetryOccupancyTest extends OccupancyBaseTest {

    private final SplitClientConfig mTelemetryEnabledConfig = new TestableSplitConfigBuilder()
            .ready(30000)
            .featuresRefreshRate(3)
            .segmentsRefreshRate(3)
            .impressionsRefreshRate(3)
            .impressionsChunkSize(999999)
            .streamingEnabled(true)
            .shouldRecordTelemetry(true)
            .enableDebug()
            .trafficType("account")
            .build();


    @Test
    public void telemetryOccupancyPriStreamingEvent() throws InterruptedException, IOException {
        getSplitFactory(mTelemetryEnabledConfig);

        pushOccupancy(PRIMARY_CHANNEL, 1);
        sleep(2000);

        List<StreamingEvent> streamingEvents = mTelemetryStorage.popStreamingEvents();
        sleep(500);
        assertTrue(streamingEvents.stream().anyMatch(event -> event instanceof OccupancyPriStreamingEvent));
    }

    @Test
    public void telemetryOccupancySecStreamingEvent() throws InterruptedException, IOException {
        getSplitFactory(mTelemetryEnabledConfig);

        pushOccupancy(SECONDARY_CHANNEL, 1);
        sleep(2000);

        List<StreamingEvent> streamingEvents = mTelemetryStorage.popStreamingEvents();
        assertTrue(streamingEvents.stream().anyMatch(event -> event instanceof OccupancySecStreamingEvent));
    }

    @Test
    public void telemetryTokenRefreshStreamingEvent() throws InterruptedException, IOException {
        getSplitFactory(mTelemetryEnabledConfig);

        sleep(1000);
        List<StreamingEvent> streamingEvents = mTelemetryStorage.popStreamingEvents();
        assertTrue(streamingEvents.stream().anyMatch(event -> event instanceof TokenRefreshStreamingEvent));
    }
}
