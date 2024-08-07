package io.split.android.client.service.mysegments;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

import io.split.android.client.dtos.SegmentResponse;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.storage.mysegments.MySegmentsStorage;

public class MySegmentsTaskFactoryConfigurationTest {

    private HttpFetcher<? extends SegmentResponse> mHttpFetcher;
    private MySegmentsStorage mMySegmentsStorage;
    private SplitEventsManager mEventsManager;

    @Before
    public void setUp() {
        mHttpFetcher = mock(HttpFetcher.class);
        mMySegmentsStorage = mock(MySegmentsStorage.class);
        mEventsManager = mock(SplitEventsManager.class);
    }

    @Test
    public void getForMySegments() {
        MySegmentsTaskFactoryConfiguration config = MySegmentsTaskFactoryConfiguration.getForMySegments(
                mHttpFetcher,
                mMySegmentsStorage,
                mEventsManager);

        assertSame(mHttpFetcher, config.getHttpFetcher());
        assertSame(mMySegmentsStorage, config.getStorage());
        assertSame(mEventsManager, config.getEventsManager());
        assertEquals(MySegmentsSyncTaskConfig.getForMySegments(), config.getMySegmentsSyncTaskConfig());
        assertEquals(MySegmentsUpdateTaskConfig.getForMySegments(), config.getMySegmentsUpdateTaskConfig());
        assertEquals(MySegmentsOverwriteTaskConfig.getForMySegments(), config.getMySegmentsOverwriteTaskConfig());
        assertEquals(LoadMySegmentsTaskConfig.getForMySegments(), config.getLoadMySegmentsTaskConfig());
    }

    @Test
    public void getForMyLargeSegments() {
        MySegmentsTaskFactoryConfiguration config = MySegmentsTaskFactoryConfiguration.getForMyLargeSegments(
                mHttpFetcher,
                mMySegmentsStorage,
                mEventsManager);

        assertSame(mHttpFetcher, config.getHttpFetcher());
        assertSame(mMySegmentsStorage, config.getStorage());
        assertSame(mEventsManager, config.getEventsManager());
        assertEquals(MySegmentsSyncTaskConfig.getForMyLargeSegments(), config.getMySegmentsSyncTaskConfig());
        assertEquals(MySegmentsUpdateTaskConfig.getForMyLargeSegments(), config.getMySegmentsUpdateTaskConfig());
        assertEquals(MySegmentsOverwriteTaskConfig.getForMyLargeSegments(), config.getMySegmentsOverwriteTaskConfig());
        assertEquals(LoadMySegmentsTaskConfig.getForMyLargeSegments(), config.getLoadMySegmentsTaskConfig());
    }
}
