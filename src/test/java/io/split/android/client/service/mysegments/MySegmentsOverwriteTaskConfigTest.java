package io.split.android.client.service.mysegments;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTaskType;

public class MySegmentsOverwriteTaskConfigTest {

    @Test
    public void getForMySegments() {
        MySegmentsOverwriteTaskConfig config = MySegmentsOverwriteTaskConfig.getForMySegments();

        assertEquals(config.getTaskType(), SplitTaskType.MY_SEGMENTS_OVERWRITE);
        assertEquals(config.getInternalEvent(), SplitInternalEvent.MY_SEGMENTS_UPDATED);
    }
}
