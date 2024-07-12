package io.split.android.client.service.mysegments;

import static org.junit.Assert.*;

import org.junit.Test;

import io.split.android.client.service.executor.SplitTaskType;

public class LoadMySegmentsTaskConfigTest {

    @Test
    public void getForMySegments() {
        LoadMySegmentsTaskConfig config = LoadMySegmentsTaskConfig.getForMySegments();

        assertEquals(config.getTaskType(), SplitTaskType.LOAD_LOCAL_MY_SYGMENTS);
    }

    @Test
    public void getForMyLargeSegments() {
        LoadMySegmentsTaskConfig config = LoadMySegmentsTaskConfig.getForMyLargeSegments();

        assertEquals(config.getTaskType(), SplitTaskType.LOAD_LOCAL_MY_LARGE_SEGMENTS);
    }
}
