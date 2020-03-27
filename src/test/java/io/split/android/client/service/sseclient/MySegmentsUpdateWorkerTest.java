package io.split.android.client.service.sseclient;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import io.split.android.client.service.Synchronizer;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackChannel;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackChannelImpl;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackMessage;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackMessageType;
import io.split.android.client.service.sseclient.notifications.MySegmentChangeNotification;
import io.split.android.client.service.sseclient.reactor.MySegmentsUpdateWorker;

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
    }

    @Test
    public void mySegmentsUpdateReceived() throws InterruptedException {

        mNotificationQueue.offer(new MySegmentChangeNotification());
        mNotificationQueue.offer(new MySegmentChangeNotification());
        mNotificationQueue.offer(new MySegmentChangeNotification());
        mNotificationQueue.offer(new MySegmentChangeNotification());

        Thread.sleep(1000);

        verify(mSynchronizer, times(4)).syncronizeMySegments();


    }
}
