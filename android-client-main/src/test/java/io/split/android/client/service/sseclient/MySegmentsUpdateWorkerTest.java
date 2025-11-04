package io.split.android.client.service.sseclient;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import io.split.android.client.service.mysegments.MySegmentUpdateParams;
import io.split.android.client.service.sseclient.reactor.MySegmentsUpdateWorker;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizer;

public class MySegmentsUpdateWorkerTest {

    MySegmentsUpdateWorker mWorker;

    @Mock
    MySegmentsSynchronizer mSynchronizer;

    BlockingQueue<MySegmentUpdateParams> mNotificationQueue;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mNotificationQueue = new ArrayBlockingQueue<>(50);
        mWorker = new MySegmentsUpdateWorker(mSynchronizer, mNotificationQueue);
        mWorker.start();
    }

    @Test
    public void mySegmentsUpdateReceived() throws InterruptedException {

        MySegmentUpdateParams params = mock(MySegmentUpdateParams.class);
        MySegmentUpdateParams params2 = mock(MySegmentUpdateParams.class);
        MySegmentUpdateParams params3 = mock(MySegmentUpdateParams.class);
        MySegmentUpdateParams params4 = mock(MySegmentUpdateParams.class);
        mNotificationQueue.offer(params);
        mNotificationQueue.offer(params2);
        mNotificationQueue.offer(params3);
        mNotificationQueue.offer(params4);

        Thread.sleep(200);

        verify(mSynchronizer, times(1)).forceMySegmentsSync(params);
        verify(mSynchronizer, times(1)).forceMySegmentsSync(params2);
        verify(mSynchronizer, times(1)).forceMySegmentsSync(params3);
        verify(mSynchronizer, times(1)).forceMySegmentsSync(params4);
    }

    @Test
    public void stopped() throws InterruptedException {
        mWorker.stop();
        mNotificationQueue.offer(mock(MySegmentUpdateParams.class));

        Thread.sleep(1000);

        verify(mSynchronizer, never()).synchronizeMySegments();
    }
}
