package io.split.android.client.service.sseclient.notifications.mysegments;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;

import io.split.android.client.common.CompressionType;
import io.split.android.client.service.sseclient.notifications.HashingAlgorithm;
import io.split.android.client.service.sseclient.notifications.MyLargeSegmentChangeNotification;
import io.split.android.client.service.sseclient.notifications.MySegmentUpdateStrategy;

public class MyLargeSegmentsNotificationProcessorImplTest {

    private MySegmentsNotificationProcessorHelper mHelper;
    private MySegmentsNotificationProcessorConfiguration mConfiguration;
    private SyncDelayCalculator mSyncDelayCalculator;
    private BlockingQueue mBlockingQueue;
    private MyLargeSegmentsNotificationProcessorImpl mNotificationProcessor;

    @Before
    public void setup() {
        mConfiguration = mock(MySegmentsNotificationProcessorConfiguration.class);
        mBlockingQueue = mock(BlockingQueue.class);
        when(mConfiguration.getNotificationsQueue()).thenReturn(mBlockingQueue);
        when(mConfiguration.getUserKey()).thenReturn("key");
        mHelper = mock(MySegmentsNotificationProcessorHelper.class);
        mSyncDelayCalculator = mock(SyncDelayCalculator.class);
        mNotificationProcessor = new MyLargeSegmentsNotificationProcessorImpl(mHelper, mConfiguration, mSyncDelayCalculator);
    }

    @Test
    public void processDelegatesToHelper() {
        MyLargeSegmentChangeNotification notification = mock(MyLargeSegmentChangeNotification.class);
        when(notification.getCompression()).thenReturn(CompressionType.GZIP);
        when(notification.getData()).thenReturn("dummy");
        when(notification.getHashingAlgorithm()).thenReturn(HashingAlgorithm.MURMUR3_32);
        when(notification.getLargeSegments()).thenReturn(new HashSet<>(Arrays.asList("segment1", "segment2")));
        when(notification.getUpdateIntervalMs()).thenReturn(1000L);
        when(notification.getAlgorithmSeed()).thenReturn(1234);
        when(notification.getUpdateStrategy()).thenReturn(MySegmentUpdateStrategy.BOUNDED_FETCH_REQUEST);
        when(mSyncDelayCalculator.calculateSyncDelay("key", 1000L, 1234, MySegmentUpdateStrategy.BOUNDED_FETCH_REQUEST, HashingAlgorithm.MURMUR3_32)).thenReturn(25L);

        mNotificationProcessor.process(notification);

        verify(mHelper).processMyLargeSegmentsUpdate(MySegmentUpdateStrategy.BOUNDED_FETCH_REQUEST,
                "dummy",
                CompressionType.GZIP,
                new HashSet<>(Arrays.asList("segment1", "segment2")),
                notification.getChangeNumber(), mBlockingQueue,
                25L);
        verify(mHelper, times(0)).processMySegmentsUpdate(any(), any(), any(), any(), any(), any(), anyLong());
    }

    @Test
    public void delayIsCalculatedWithCalculator() {
        MyLargeSegmentChangeNotification notification = mock(MyLargeSegmentChangeNotification.class);
        when(notification.getUpdateIntervalMs()).thenReturn(1000L);
        when(notification.getAlgorithmSeed()).thenReturn(1234);
        when(notification.getUpdateStrategy()).thenReturn(MySegmentUpdateStrategy.UNBOUNDED_FETCH_REQUEST);
        when(notification.getHashingAlgorithm()).thenReturn(HashingAlgorithm.MURMUR3_32);
        when(mSyncDelayCalculator.calculateSyncDelay("key", 1000L, 1234, MySegmentUpdateStrategy.UNBOUNDED_FETCH_REQUEST, HashingAlgorithm.MURMUR3_32)).thenReturn(25L);

        mNotificationProcessor.process(notification);

        verify(mSyncDelayCalculator).calculateSyncDelay("key", 1000L, 1234, MySegmentUpdateStrategy.UNBOUNDED_FETCH_REQUEST, HashingAlgorithm.MURMUR3_32);
    }
}
