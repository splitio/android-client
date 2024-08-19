package io.split.android.client.service.mysegments;

import static org.junit.Assert.*;

import org.junit.Test;

import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.telemetry.model.streaming.UpdatesFromSSEEnum;

public class MySegmentsUpdateTaskConfigTest {

    @Test
    public void getForMySegments() {
        MySegmentsUpdateTaskConfig config = MySegmentsUpdateTaskConfig.getForMySegments();

        assertEquals(config.getTaskType(), SplitTaskType.MY_SEGMENTS_UPDATE);
        assertEquals(config.getUpdateEvent(), SplitInternalEvent.MY_SEGMENTS_UPDATED);
        assertEquals(config.getTelemetrySSEKey(), UpdatesFromSSEEnum.MY_SEGMENTS);
    }
}
