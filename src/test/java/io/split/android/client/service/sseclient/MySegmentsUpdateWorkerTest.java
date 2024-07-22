package io.split.android.client.service.sseclient;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import io.split.android.client.service.sseclient.notifications.MySegmentChangeNotification;
import io.split.android.client.service.sseclient.reactor.MySegmentsUpdateWorker;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizer;

public class MySegmentsUpdateWorkerTest {

    MySegmentsUpdateWorker mWorker;

    @Mock
    MySegmentsSynchronizer mSynchronizer;

    BlockingQueue<Long> mNotificationQueue;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mNotificationQueue = new ArrayBlockingQueue<>(50);
        mWorker = new MySegmentsUpdateWorker(mSynchronizer, mNotificationQueue);
        mWorker.start();
    }

    @Test
    public void mySegmentsUpdateReceived() throws InterruptedException {

        mNotificationQueue.offer(0L);
        mNotificationQueue.offer(1L);
        mNotificationQueue.offer(2L);
        mNotificationQueue.offer(3L);

        Thread.sleep(1000);

        verify(mSynchronizer, times(4)).forceMySegmentsSync();
    }

    @Test
    public void stopped() throws InterruptedException {
        mWorker.stop();
        mNotificationQueue.offer(0L);

        Thread.sleep(1000);

        verify(mSynchronizer, never()).synchronizeMySegments();
    }
}
