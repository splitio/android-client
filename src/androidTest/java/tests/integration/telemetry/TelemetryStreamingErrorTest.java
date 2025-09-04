package tests.integration.telemetry;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.util.List;

import helper.TestableSplitConfigBuilder;
import io.split.android.client.storage.db.StorageFactory;
import io.split.android.client.telemetry.model.streaming.SseConnectionErrorStreamingEvent;
import io.split.android.client.telemetry.model.streaming.StreamingEvent;
import io.split.android.client.telemetry.model.streaming.SyncModeUpdateStreamingEvent;
import io.split.android.client.telemetry.storage.TelemetryStorage;
import tests.integration.streaming.AblyErrorBaseTest;

public class TelemetryStreamingErrorTest extends AblyErrorBaseTest {

    @Test
    public void errorStreamingEventTest() throws IOException, InterruptedException {
        initializeForTelemetry();
        Thread.sleep(1000);
        TelemetryStorage telemetryStorage = StorageFactory.getTelemetryStorage(true);
        List<StreamingEvent> streamingEvents = mTelemetryStorage.popStreamingEvents();
        assertTrue(streamingEvents.stream().anyMatch(event -> event instanceof SseConnectionErrorStreamingEvent));
    }

    @Test
    public void syncModeChangeStreamingEventTest() throws IOException, InterruptedException {
        initializeForTelemetry();

        List<StreamingEvent> streamingEvents = mTelemetryStorage.popStreamingEvents();
        assertTrue(streamingEvents.stream().anyMatch(event -> {
            if (event instanceof SyncModeUpdateStreamingEvent) {
                return event.getEventData().intValue() == 1;
            }

            return false;
        }));
    }

    private void initializeForTelemetry() throws IOException, InterruptedException {
        initializeFactory(new TestableSplitConfigBuilder()
                .ready(30000)
                .featuresRefreshRate(3)
                .segmentsRefreshRate(3)
                .impressionsRefreshRate(3)
                .impressionsChunkSize(999999)
                .streamingEnabled(true)
                .enableDebug()
                .shouldRecordTelemetry(true)
                .trafficType("account")
                .build());

        pushErrorMessage(40012);
    }
}
