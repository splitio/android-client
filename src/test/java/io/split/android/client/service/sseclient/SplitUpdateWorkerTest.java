package io.split.android.client.service.sseclient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import io.split.android.client.common.CompressionType;
import io.split.android.client.common.CompressionUtilProvider;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.splits.SplitInPlaceUpdateTask;
import io.split.android.client.service.sseclient.notifications.SplitsChangeNotification;
import io.split.android.client.service.sseclient.reactor.SplitUpdatesWorker;
import io.split.android.client.service.synchronizer.Synchronizer;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.utils.CompressionUtil;

public class SplitUpdateWorkerTest {

    BlockingQueue<SplitsChangeNotification> mNotificationsQueue;

    SplitUpdatesWorker mWorker;

    @Mock
    Synchronizer mSynchronizer;
    @Mock
    private SplitsStorage mSplitsStorage;
    @Mock
    private CompressionUtilProvider mCompressionUtilProvider;
    @Mock
    private SplitTaskExecutor mSplitTaskExecutor;
    @Mock
    private SplitTaskFactory mSplitTaskFactory;
    @Mock
    private SplitUpdatesWorker.Base64Decoder mBase64Decoder;

    private static final String TEST_SPLIT = "{\"trafficTypeName\":\"account\",\"name\":\"android_test_2\",\"trafficAllocation\":100,\"trafficAllocationSeed\":-1955610140,\"seed\":-633015570,\"status\":\"ACTIVE\",\"killed\":false,\"defaultTreatment\":\"off\",\"changeNumber\":1648733409158,\"algo\":2,\"configurations\":{},\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"account\",\"attribute\":null},\"matcherType\":\"IN_SPLIT_TREATMENT\",\"negate\":false,\"userDefinedSegmentMatcherData\":null,\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":{\"split\":\"android_test_3\",\"treatments\":[\"on\"]},\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":100},{\"treatment\":\"off\",\"size\":0}],\"label\":\"in split android_test_3 treatment [on]\"},{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"account\",\"attribute\":null},\"matcherType\":\"ALL_KEYS\",\"negate\":false,\"userDefinedSegmentMatcherData\":null,\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":100}],\"label\":\"default rule\"}]}";

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mNotificationsQueue = new ArrayBlockingQueue<>(50);
        mWorker = new SplitUpdatesWorker(mSynchronizer,
                mNotificationsQueue,
                mSplitsStorage,
                mCompressionUtilProvider,
                mSplitTaskExecutor,
                mSplitTaskFactory,
                mBase64Decoder);
        mWorker.start();
    }

    @Test
    public void splitsUpdateReceived() throws InterruptedException {
        Long changeNumber = 1000L;
        SplitsChangeNotification notification = getLegacyNotification();
        when(notification.getChangeNumber()).thenReturn(changeNumber);
        mNotificationsQueue.offer(notification);

        Thread.sleep(500);

        ArgumentCaptor<Long> changeNumberCaptor = ArgumentCaptor.forClass(Long.class);
        verify(mSynchronizer, times(1))
                .synchronizeSplits(changeNumberCaptor.capture());
        Assert.assertEquals(changeNumber, changeNumberCaptor.getValue());
    }

    @Test
    public void severalSplitsUpdateReceived() throws InterruptedException {
        Long changeNumber = 1000L;
        SplitsChangeNotification notification = getLegacyNotification();
        when(notification.getChangeNumber()).thenReturn(changeNumber);
        mNotificationsQueue.offer(notification);
        mNotificationsQueue.offer(notification);
        mNotificationsQueue.offer(notification);
        mNotificationsQueue.offer(notification);

        Thread.sleep(500);

        verify(mSynchronizer, times(4))
                .synchronizeSplits(anyLong());
    }

    @Test
    public void stopped() throws InterruptedException {
        mWorker.stop();
        Long changeNumber = 1000L;
        SplitsChangeNotification notification = getLegacyNotification();
        when(notification.getChangeNumber()).thenReturn(changeNumber);
        mNotificationsQueue.offer(notification);

        Thread.sleep(500);

        verify(mSynchronizer, never())
                .synchronizeSplits(anyLong());
    }

    @Test
    public void lowerChangeNumberThanStoredDoesNothing() {
        long changeNumber = 1000L;
        when(mSplitsStorage.getTill()).thenReturn(changeNumber + 1);
        SplitsChangeNotification notification = getLegacyNotification();
        when(notification.getChangeNumber()).thenReturn(changeNumber);
        mNotificationsQueue.offer(notification);

        verify(mSynchronizer, never())
                .synchronizeSplits(anyLong());
    }

    @Test
    public void nullPreviousChangeNumberDoesNothing() {
        when(mSplitsStorage.getTill()).thenReturn(1000L);
        SplitsChangeNotification notification = getNewNotification();
        when(notification.getPreviousChangeNumber()).thenReturn(null);
        mNotificationsQueue.offer(notification);

        verify(mSynchronizer, never())
                .synchronizeSplits(anyLong());
    }

    @Test
    public void zeroPreviousChangeNumberDoesNothing() {
        when(mSplitsStorage.getTill()).thenReturn(1000L);
        SplitsChangeNotification notification = getNewNotification();
        when(notification.getPreviousChangeNumber()).thenReturn(0L);
        mNotificationsQueue.offer(notification);

        verify(mSynchronizer, never())
                .synchronizeSplits(anyLong());
    }

    @Test
    public void legacyNotificationDoesNotSubmitTaskInExecutor() {
        long changeNumber = 1000L;
        when(mSplitsStorage.getTill()).thenReturn(changeNumber - 1);
        SplitsChangeNotification notification = getLegacyNotification();
        when(notification.getChangeNumber()).thenReturn(changeNumber);
        mNotificationsQueue.offer(notification);

        verify(mSplitTaskExecutor, never())
                .submit(any(), any());
    }

    @Test
    public void newNotificationSubmitsTaskInExecutor() throws InterruptedException {
        long changeNumber = 1000L;
        byte[] bytes = TEST_SPLIT.getBytes();
        SplitInPlaceUpdateTask updateTask = mock(SplitInPlaceUpdateTask.class);
        SplitsChangeNotification notification = getNewNotification();
        CompressionUtil mockCompressor = mock(CompressionUtil.class);

        when(mSplitTaskFactory.createSplitsUpdateTask(any(), anyLong())).thenReturn(updateTask);
        when(mSplitsStorage.getTill()).thenReturn(changeNumber);
        when(notification.getChangeNumber()).thenReturn(changeNumber + 1);
        when(notification.getCompressionType()).thenReturn(CompressionType.NONE);
        when(mockCompressor.decompress(any())).thenReturn(bytes);

        when(mCompressionUtilProvider.get(any())).thenReturn(mockCompressor);
        when(mBase64Decoder.decode(anyString())).thenReturn(bytes);

        mNotificationsQueue.offer(notification);
        Thread.sleep(500);

        verify(mSplitTaskExecutor).submit(eq(updateTask), eq(null));
    }

    private static SplitsChangeNotification getLegacyNotification() {
        SplitsChangeNotification mock = mock(SplitsChangeNotification.class);
        when(mock.getChangeNumber()).thenReturn(1000L);
        return mock;
    }

    private static SplitsChangeNotification getNewNotification() {
        SplitsChangeNotification mock = mock(SplitsChangeNotification.class);
        when(mock.getCompressionType()).thenReturn(CompressionType.ZLIB);
        when(mock.getData()).thenReturn(TEST_SPLIT);
        when(mock.getPreviousChangeNumber()).thenReturn(1000L);
        when(mock.getChangeNumber()).thenReturn(2000L);
        return mock;
    }
}
