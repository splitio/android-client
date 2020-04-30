package io.split.android.client.service.sseclient;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import io.split.android.client.service.sseclient.notifications.SplitsChangeNotification;
import io.split.android.client.service.sseclient.reactor.SplitUpdatesWorker;
import io.split.android.client.service.synchronizer.Synchronizer;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SplitUpdateWorkerTest {

    BlockingQueue<SplitsChangeNotification> mNotificationsQueue;

    SplitUpdatesWorker mWorker;

    @Mock
    Synchronizer mSynchronizer;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mNotificationsQueue = new ArrayBlockingQueue<>(50);
        mWorker = new SplitUpdatesWorker(mSynchronizer, mNotificationsQueue);
        mWorker.start();
    }

    @Test
    public void splitsUpdateReceived() throws InterruptedException {
        Long changeNumber = 1000L;
        SplitsChangeNotification notification = Mockito.mock(SplitsChangeNotification.class);
        when(notification.getChangeNumber()).thenReturn(changeNumber);
        mNotificationsQueue.offer(notification);

        Thread.sleep(2000);

        ArgumentCaptor<Long> changeNumberCaptor = ArgumentCaptor.forClass(Long.class);
        verify(mSynchronizer, times(1))
                .synchronizeSplits(changeNumberCaptor.capture());
        Assert.assertEquals(changeNumber, changeNumberCaptor.getValue());
    }

    @Test
    public void severalSplitsUpdateReceived() throws InterruptedException {
        Long changeNumber = 1000L;
        SplitsChangeNotification notification = Mockito.mock(SplitsChangeNotification.class);
        when(notification.getChangeNumber()).thenReturn(changeNumber);
        mNotificationsQueue.offer(notification);
        mNotificationsQueue.offer(notification);
        mNotificationsQueue.offer(notification);
        mNotificationsQueue.offer(notification);

        Thread.sleep(2000);

        verify(mSynchronizer, times(4))
                .synchronizeSplits(anyLong());
    }

    @Test
    public void stopped() throws InterruptedException {
        mWorker.stop();
        Long changeNumber = 1000L;
        SplitsChangeNotification notification = Mockito.mock(SplitsChangeNotification.class);
        when(notification.getChangeNumber()).thenReturn(changeNumber);
        mNotificationsQueue.offer(notification);

        Thread.sleep(2000);

        verify(mSynchronizer, never())
                .synchronizeSplits(anyLong());
    }
}
