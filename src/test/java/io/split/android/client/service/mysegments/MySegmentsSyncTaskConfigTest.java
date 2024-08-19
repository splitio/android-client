package io.split.android.client.service.mysegments;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.telemetry.model.OperationType;

public class MySegmentsSyncTaskConfigTest {

    @Test
    public void getForMySegments() {
        MySegmentsSyncTaskConfig config = MySegmentsSyncTaskConfig.get();

        assertEquals(config.getTaskType(), SplitTaskType.MY_SEGMENTS_SYNC);
        assertEquals(config.getUpdateEvent(), SplitInternalEvent.MY_SEGMENTS_UPDATED);
        assertEquals(config.getFetchedEvent(), SplitInternalEvent.MY_SEGMENTS_FETCHED);
        assertEquals(config.getTelemetryOperationType(), OperationType.MY_SEGMENT);
    }
}
