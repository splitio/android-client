package io.split.android.client.service.mysegments;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.storage.mysegments.MySegmentsStorage;

public class MySegmentsTaskFactoryConfigurationTest {

    private HttpFetcher<? extends MembershipsResponse> mHttpFetcher;
    private MySegmentsStorage mMySegmentsStorage;
    private MySegmentsStorage mMyLargeSegmentsStorage;
    private SplitEventsManager mEventsManager;

    @Before
    public void setUp() {
        mHttpFetcher = mock(HttpFetcher.class);
        mMySegmentsStorage = mock(MySegmentsStorage.class);
        mMyLargeSegmentsStorage = mock(MySegmentsStorage.class);
        mEventsManager = mock(SplitEventsManager.class);
    }

    @Test
    public void getForMySegments() {
        MySegmentsTaskFactoryConfiguration config = MySegmentsTaskFactoryConfiguration.get(
                mHttpFetcher,
                mMySegmentsStorage,
                mMyLargeSegmentsStorage,
                mEventsManager);

        assertSame(mHttpFetcher, config.getHttpFetcher());
        assertSame(mMySegmentsStorage, config.getMySegmentsStorage());
        assertSame(mEventsManager, config.getEventsManager());
        assertEquals(MySegmentsSyncTaskConfig.get(), config.getMySegmentsSyncTaskConfig());
        assertEquals(MySegmentsUpdateTaskConfig.getForMySegments(), config.getMySegmentsUpdateTaskConfig());
        assertEquals(MySegmentsOverwriteTaskConfig.getForMySegments(), config.getMySegmentsOverwriteTaskConfig());
        assertEquals(LoadMySegmentsTaskConfig.get(), config.getLoadMySegmentsTaskConfig());
    }
}
