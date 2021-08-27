package io.split.android.client.service.sseclient;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import io.split.android.client.service.sseclient.notifications.IncomingNotification;
import io.split.android.client.service.sseclient.notifications.MySegmentChangeNotification;
import io.split.android.client.service.sseclient.reactor.MySegmentsUpdateWorker;
import io.split.android.client.service.synchronizer.Synchronizer;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class MySegmentsUpdateWorkerTest {

    MySegmentsUpdateWorker mWorker;

    @Mock
    Synchronizer mSynchronizer;

    BlockingQueue<MySegmentChangeNotification> mNotificationQueue;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mNotificationQueue = new ArrayBlockingQueue<>(50);
        mWorker = new MySegmentsUpdateWorker(mSynchronizer, mNotificationQueue);
        mWorker.start();
    }

    @Test
    public void mySegmentsUpdateReceived() throws InterruptedException {

        mNotificationQueue.offer(new MySegmentChangeNotification());
        mNotificationQueue.offer(new MySegmentChangeNotification());
        mNotificationQueue.offer(new MySegmentChangeNotification());
        mNotificationQueue.offer(new MySegmentChangeNotification());

        Thread.sleep(1000);

        verify(mSynchronizer, times(4)).forceMySegmentsSync();
    }

    @Test
    public void stopped() throws InterruptedException {
        mWorker.stop();
        mNotificationQueue.offer(new MySegmentChangeNotification());

        Thread.sleep(1000);

        verify(mSynchronizer, never()).synchronizeMySegments();
    }
}
