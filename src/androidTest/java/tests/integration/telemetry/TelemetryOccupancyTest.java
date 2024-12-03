package tests.integration.telemetry;

import static org.junit.Assert.assertTrue;
import static java.lang.Thread.sleep;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import helper.TestableSplitConfigBuilder;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.telemetry.model.streaming.OccupancyPriStreamingEvent;
import io.split.android.client.telemetry.model.streaming.OccupancySecStreamingEvent;
import io.split.android.client.telemetry.model.streaming.StreamingEvent;
import io.split.android.client.telemetry.model.streaming.TokenRefreshStreamingEvent;
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
        new CountDownLatch(1);
        getSplitFactory(mTelemetryEnabledConfig);

        pushOccupancy(PRIMARY_CHANNEL, 1);

        long startTime = System.currentTimeMillis();
        List<StreamingEvent> streamingEvents = new ArrayList<>();
        streamingEvents = mTelemetryStorage.popStreamingEvents();
        while (System.currentTimeMillis() - startTime < 5000 &&
                !streamingEvents.stream().anyMatch(event -> event instanceof OccupancyPriStreamingEvent)) {
            Thread.sleep(100);
            streamingEvents = mTelemetryStorage.popStreamingEvents();
        }
        assertTrue(streamingEvents.stream().anyMatch(event -> event instanceof OccupancyPriStreamingEvent));
    }

    @Test
    public void telemetryOccupancySecStreamingEvent() throws InterruptedException, IOException {
        getSplitFactory(mTelemetryEnabledConfig);

        pushOccupancy(SECONDARY_CHANNEL, 1);
        sleep(2000);

        long startTime = System.currentTimeMillis();
        List<StreamingEvent> streamingEvents = new ArrayList<>();
        streamingEvents = mTelemetryStorage.popStreamingEvents();
        while (System.currentTimeMillis() - startTime < 5000 &&
                !streamingEvents.stream().anyMatch(event -> event instanceof OccupancySecStreamingEvent)) {
            Thread.sleep(100);
            streamingEvents = mTelemetryStorage.popStreamingEvents();
        }
        assertTrue(streamingEvents.stream().anyMatch(event -> event instanceof OccupancySecStreamingEvent));
    }

    @Test
    public void telemetryTokenRefreshStreamingEvent() throws InterruptedException, IOException {
        getSplitFactory(mTelemetryEnabledConfig);


        List<StreamingEvent> streamingEvents = mTelemetryStorage.popStreamingEvents();
        assertTrue(streamingEvents.stream().anyMatch(event -> event instanceof TokenRefreshStreamingEvent));
    }
}
