package io.split.android.client.service.sseclient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
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

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import io.split.android.client.common.CompressionType;
import io.split.android.client.common.CompressionUtilProvider;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.rules.RuleBasedSegmentInPlaceUpdateTask;
import io.split.android.client.service.splits.SplitInPlaceUpdateTask;
import io.split.android.client.service.sseclient.notifications.InstantUpdateChangeNotification;
import io.split.android.client.service.sseclient.notifications.NotificationType;
import io.split.android.client.service.sseclient.notifications.RuleBasedSegmentChangeNotification;
import io.split.android.client.service.sseclient.notifications.SplitsChangeNotification;
import io.split.android.client.service.sseclient.reactor.SplitUpdatesWorker;
import io.split.android.client.service.synchronizer.Synchronizer;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.utils.CompressionUtil;
import io.split.android.fake.SplitTaskExecutorStub;

public class SplitUpdateWorkerTest {

    BlockingQueue<InstantUpdateChangeNotification> mNotificationsQueue;

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
    @Mock
    private RuleBasedSegmentStorage mRuleBasedSegmentStorage;

    private static final String TEST_SPLIT = "{\"trafficTypeName\":\"account\",\"name\":\"android_test_2\",\"trafficAllocation\":100,\"trafficAllocationSeed\":-1955610140,\"seed\":-633015570,\"status\":\"ACTIVE\",\"killed\":false,\"defaultTreatment\":\"off\",\"changeNumber\":1648733409158,\"algo\":2,\"configurations\":{},\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"account\",\"attribute\":null},\"matcherType\":\"IN_SPLIT_TREATMENT\",\"negate\":false,\"userDefinedSegmentMatcherData\":null,\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":{\"split\":\"android_test_3\",\"treatments\":[\"on\"]},\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":100},{\"treatment\":\"off\",\"size\":0}],\"label\":\"in split android_test_3 treatment [on]\"},{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"account\",\"attribute\":null},\"matcherType\":\"ALL_KEYS\",\"negate\":false,\"userDefinedSegmentMatcherData\":null,\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":100}],\"label\":\"default rule\"}]}";
    private static final String TEST_RULE_BASED_SEGMENT = "{\"changeNumber\":5,\"name\":\"mauro_rule_based_segment\",\"status\":\"ACTIVE\",\"trafficTypeName\":\"user\",\"excluded\":{\"keys\":[\"mauro@split.io\",\"gaston@split.io\"],\"segments\":[\"segment_test\"]},\"conditions\":[{\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\"},\"matcherType\":\"WHITELIST\",\"negate\":false,\"whitelistMatcherData\":{\"whitelist\":[\"mdp\",\"tandil\",\"bsas\"]}},{\"keySelector\":{\"trafficType\":\"user\",\"attribute\":\"email\"},\"matcherType\":\"ENDS_WITH\",\"negate\":false,\"whitelistMatcherData\":{\"whitelist\":[\"@split.io\"]}}]}},{\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\"},\"matcherType\":\"IN_SEGMENT\",\"negate\":false,\"userDefinedSegmentMatcherData\":{\"segmentName\":\"regular_segment\"}}]}}]}";

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mNotificationsQueue = new ArrayBlockingQueue<>(10);
        mWorker = new SplitUpdatesWorker(mSynchronizer,
                mNotificationsQueue,
                mSplitsStorage,
                mRuleBasedSegmentStorage,
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

        verify(mSplitTaskExecutor).submit(eq(updateTask), argThat(Objects::nonNull));
        verify(mSynchronizer, never())
                .synchronizeSplits(anyLong());
    }

    @Test
    public void newRuleBasedSegmentNotificationSubmitsTaskOnExecutor() throws InterruptedException {
        byte[] bytes = TEST_RULE_BASED_SEGMENT.getBytes();
        CompressionUtil mockCompressor = mock(CompressionUtil.class);

        RuleBasedSegmentInPlaceUpdateTask updateTask = mock(RuleBasedSegmentInPlaceUpdateTask.class);
        RuleBasedSegmentChangeNotification notification = getRuleBasedSegmentNotification();

        when(mSplitTaskFactory.createRuleBasedSegmentUpdateTask(any(), anyLong())).thenReturn(updateTask);
        when(mRuleBasedSegmentStorage.getChangeNumber()).thenReturn(1000L);
        when(notification.getCompressionType()).thenReturn(CompressionType.NONE);
        when(mockCompressor.decompress(any())).thenReturn(bytes);

        when(mCompressionUtilProvider.get(any())).thenReturn(mockCompressor);
        when(mBase64Decoder.decode(anyString())).thenReturn(bytes);

        mNotificationsQueue.offer(notification);
        Thread.sleep(100);

        verify(mSplitTaskExecutor).submit(eq(updateTask), argThat(Objects::nonNull));
        verify(mSynchronizer, never()).synchronizeSplits(anyLong());
        verify(mSynchronizer, never()).synchronizeRuleBasedSegments(anyLong());
    }

    @Test
    public void synchronizeSplitsIsCalledOnSynchronizerWhenTaskFails() throws InterruptedException {
        initWorkerWithStubExecutor();

        long changeNumber = 1000L;
        SplitInPlaceUpdateTask updateTask = mock(SplitInPlaceUpdateTask.class);
        SplitsChangeNotification notification = getNewNotification();
        CompressionUtil mockCompressor = mock(CompressionUtil.class);

        when(updateTask.execute()).thenAnswer(invocation -> SplitTaskExecutionInfo.error(SplitTaskType.SPLITS_SYNC));
        when(mSplitTaskFactory.createSplitsUpdateTask(any(), anyLong())).thenReturn(updateTask);
        when(mSplitsStorage.getTill()).thenReturn(changeNumber);
        when(notification.getChangeNumber()).thenReturn(changeNumber + 1);
        when(notification.getCompressionType()).thenReturn(CompressionType.NONE);
        when(mockCompressor.decompress(any())).thenReturn(TEST_SPLIT.getBytes());
        when(mCompressionUtilProvider.get(any())).thenReturn(mockCompressor);

        mNotificationsQueue.offer(notification);
        Thread.sleep(500);

        verify(mSynchronizer).synchronizeSplits(changeNumber + 1);
    }

    @Test
    public void synchronizeRuleBasedSegmentsIsCalledOnSynchronizerWhenTaskFails() throws InterruptedException {
        initWorkerWithStubExecutor();

        long changeNumber = 1000L;
        RuleBasedSegmentInPlaceUpdateTask updateTask = mock(RuleBasedSegmentInPlaceUpdateTask.class);
        RuleBasedSegmentChangeNotification notification = getRuleBasedSegmentNotification();
        CompressionUtil mockCompressor = mock(CompressionUtil.class);

        when(updateTask.execute()).thenAnswer(invocation -> SplitTaskExecutionInfo.error(SplitTaskType.RULE_BASED_SEGMENT_SYNC));
        when(mSplitTaskFactory.createRuleBasedSegmentUpdateTask(any(), anyLong())).thenReturn(updateTask);
        when(mRuleBasedSegmentStorage.getChangeNumber()).thenReturn(changeNumber);
        when(notification.getChangeNumber()).thenReturn(changeNumber + 1);
        when(notification.getCompressionType()).thenReturn(CompressionType.NONE);
        when(mockCompressor.decompress(any())).thenReturn(TEST_RULE_BASED_SEGMENT.getBytes());
        when(mCompressionUtilProvider.get(any())).thenReturn(mockCompressor);

        mNotificationsQueue.offer(notification);
        Thread.sleep(500);

        verify(mSynchronizer).synchronizeRuleBasedSegments(changeNumber + 1);
    }

    @Test
    public void synchronizeSplitsIsCalledOnSynchronizerWhenParsingFails() throws InterruptedException {
        initWorkerWithStubExecutor();

        long changeNumber = 1000L;
        SplitInPlaceUpdateTask updateTask = mock(SplitInPlaceUpdateTask.class);
        SplitsChangeNotification notification = getNewNotification();
        CompressionUtil mockCompressor = mock(CompressionUtil.class);

        when(mSplitTaskFactory.createSplitsUpdateTask(any(), anyLong())).thenReturn(updateTask);
        when(mSplitsStorage.getTill()).thenReturn(changeNumber);
        when(notification.getChangeNumber()).thenReturn(changeNumber + 1);
        when(notification.getCompressionType()).thenReturn(CompressionType.NONE);
        when(mockCompressor.decompress(any())).thenReturn("malformed_split".getBytes());
        when(mCompressionUtilProvider.get(any())).thenReturn(mockCompressor);

        mNotificationsQueue.offer(notification);
        Thread.sleep(800);

        verify(mSynchronizer).synchronizeSplits(changeNumber + 1);
    }

    @Test
    public void synchronizeSplitsIsCalledOnSynchronizerWhenDecompressingFailsDueToException() throws InterruptedException {
        long changeNumber = 1000L;
        SplitInPlaceUpdateTask updateTask = mock(SplitInPlaceUpdateTask.class);
        SplitsChangeNotification notification = getNewNotification();
        CompressionUtil mockCompressor = mock(CompressionUtil.class);

        when(mSplitTaskFactory.createSplitsUpdateTask(any(), anyLong())).thenReturn(updateTask);
        when(mSplitsStorage.getTill()).thenReturn(changeNumber);
        when(notification.getChangeNumber()).thenReturn(changeNumber + 1);
        when(notification.getCompressionType()).thenReturn(CompressionType.NONE);
        when(mockCompressor.decompress(any())).thenThrow(new RuntimeException("test"));
        when(mCompressionUtilProvider.get(any())).thenReturn(mockCompressor);

        mNotificationsQueue.offer(notification);
        Thread.sleep(500);

        verify(mSynchronizer).synchronizeSplits(changeNumber + 1);
    }

    @Test
    public void synchronizeSplitsIsCalledOnSynchronizerWhenDecompressingFailsDueToNullDecompressor() throws InterruptedException {
        long changeNumber = 1000L;
        SplitInPlaceUpdateTask updateTask = mock(SplitInPlaceUpdateTask.class);
        SplitsChangeNotification notification = getNewNotification();

        when(mSplitTaskFactory.createSplitsUpdateTask(any(), anyLong())).thenReturn(updateTask);
        when(mSplitsStorage.getTill()).thenReturn(changeNumber);
        when(notification.getChangeNumber()).thenReturn(changeNumber + 1);
        when(notification.getCompressionType()).thenReturn(CompressionType.NONE);
        when(mCompressionUtilProvider.get(any())).thenReturn(null);

        mNotificationsQueue.offer(notification);
        Thread.sleep(500);

        verify(mSynchronizer).synchronizeSplits(changeNumber + 1);
    }

    @Test
    public void synchronizeSplitsIsCalledOnSynchronizerWhenDecompressingFailsDueToNullDecompressedBytes() throws InterruptedException {
        long changeNumber = 1000L;
        SplitInPlaceUpdateTask updateTask = mock(SplitInPlaceUpdateTask.class);
        SplitsChangeNotification notification = getNewNotification();
        CompressionUtil mockCompressor = mock(CompressionUtil.class);

        when(mSplitTaskFactory.createSplitsUpdateTask(any(), anyLong())).thenReturn(updateTask);
        when(mSplitsStorage.getTill()).thenReturn(changeNumber);
        when(notification.getChangeNumber()).thenReturn(changeNumber + 1);
        when(notification.getCompressionType()).thenReturn(CompressionType.NONE);
        when(mockCompressor.decompress(any())).thenReturn(null);
        when(mCompressionUtilProvider.get(any())).thenReturn(mockCompressor);

        mNotificationsQueue.offer(notification);
        Thread.sleep(500);

        verify(mSynchronizer).synchronizeSplits(changeNumber + 1);
    }

    @Test
    public void synchronizeSplitsIsCalledOnSynchronizerWhenDecompressingFailsDueToFailedBase64Decoding() throws InterruptedException {
        long changeNumber = 1000L;
        SplitInPlaceUpdateTask updateTask = mock(SplitInPlaceUpdateTask.class);
        SplitsChangeNotification notification = getNewNotification();
        CompressionUtil mockCompressor = mock(CompressionUtil.class);

        when(mSplitTaskFactory.createSplitsUpdateTask(any(), anyLong())).thenReturn(updateTask);
        when(mSplitsStorage.getTill()).thenReturn(changeNumber);
        when(notification.getChangeNumber()).thenReturn(changeNumber + 1);
        when(notification.getCompressionType()).thenReturn(CompressionType.NONE);
        when(mockCompressor.decompress(any())).thenReturn("malformed_split".getBytes());
        when(mCompressionUtilProvider.get(any())).thenReturn(mockCompressor);
        when(mBase64Decoder.decode(any())).thenReturn(null);

        mNotificationsQueue.offer(notification);
        Thread.sleep(500);

        verify(mSynchronizer).synchronizeSplits(changeNumber + 1);
    }

    private void initWorkerWithStubExecutor() {
        mWorker.stop();
        mWorker = new SplitUpdatesWorker(mSynchronizer,
                mNotificationsQueue,
                mSplitsStorage,
                mRuleBasedSegmentStorage,
                mCompressionUtilProvider,
                new SplitTaskExecutorStub(),
                mSplitTaskFactory,
                mBase64Decoder);
        mWorker.start();
    }

    private static SplitsChangeNotification getLegacyNotification() {
        SplitsChangeNotification mock = mock(SplitsChangeNotification.class);
        when(mock.getType()).thenReturn(NotificationType.SPLIT_UPDATE);
        when(mock.getChangeNumber()).thenReturn(1000L);
        return mock;
    }

    private static SplitsChangeNotification getNewNotification() {
        SplitsChangeNotification mock = mock(SplitsChangeNotification.class);
        when(mock.getCompressionType()).thenReturn(CompressionType.ZLIB);
        when(mock.getType()).thenReturn(NotificationType.SPLIT_UPDATE);
        when(mock.getData()).thenReturn(TEST_SPLIT);
        when(mock.getPreviousChangeNumber()).thenReturn(1000L);
        when(mock.getChangeNumber()).thenReturn(2000L);
        return mock;
    }

    private RuleBasedSegmentChangeNotification getRuleBasedSegmentNotification() {
        RuleBasedSegmentChangeNotification notification = mock(RuleBasedSegmentChangeNotification.class);
        when(notification.getChangeNumber()).thenReturn(2000L);
        when(notification.getCompressionType()).thenReturn(CompressionType.NONE);
        when(notification.getData()).thenReturn(TEST_RULE_BASED_SEGMENT);
        when(notification.getType()).thenReturn(NotificationType.RULE_BASED_SEGMENT_UPDATE);
        when(notification.getPreviousChangeNumber()).thenReturn(1000L);
        return notification;
    }
}
