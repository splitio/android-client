package io.split.android.client.service.sseclient.notifications.mysegments;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static io.split.android.client.service.sseclient.notifications.KeyList.Action.ADD;
import static io.split.android.client.service.sseclient.notifications.KeyList.Action.NONE;
import static io.split.android.client.service.sseclient.notifications.KeyList.Action.REMOVE;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import io.split.android.client.common.CompressionUtilProvider;
import io.split.android.client.exceptions.MySegmentsParsingException;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.mysegments.MySegmentUpdateParams;
import io.split.android.client.service.mysegments.MySegmentsOverwriteTask;
import io.split.android.client.service.mysegments.MySegmentsSyncTask;
import io.split.android.client.service.mysegments.MySegmentsTaskFactory;
import io.split.android.client.service.mysegments.MySegmentsUpdateTask;
import io.split.android.client.service.sseclient.notifications.HashingAlgorithm;
import io.split.android.client.service.sseclient.notifications.KeyList;
import io.split.android.client.service.sseclient.notifications.MembershipNotification;
import io.split.android.client.service.sseclient.notifications.MySegmentUpdateStrategy;
import io.split.android.client.service.sseclient.notifications.MySegmentsV2PayloadDecoder;
import io.split.android.client.service.sseclient.notifications.NotificationParser;
import io.split.android.client.service.sseclient.notifications.NotificationType;
import io.split.android.client.service.sseclient.notifications.memberships.MembershipsNotificationProcessorImpl;
import io.split.android.client.utils.CompressionUtil;

public class MySegmentsNotificationProcessorImplTest {

    @Mock
    private SplitTaskExecutor mSplitTaskExecutor;
    @Mock
    private MySegmentsTaskFactory mSplitTaskFactory;
    @Mock
    private NotificationParser mNotificationParser;
    @Mock
    private CompressionUtilProvider mCompressionUtilProvider;
    @Mock
    private MySegmentsV2PayloadDecoder mMySegmentsPayloadDecoder;
    @Mock
    private CompressionUtil mCompressionUtil;
    private final BigInteger mHashedUserKey = new BigInteger("11288179738259047283");
    @Mock
    private BlockingQueue<MySegmentUpdateParams> mMySegmentChangeQueue;
    @Mock
    private MembershipNotification mMsMembershipNotification;
    @Mock
    private MembershipNotification mLsMembershipNotification;
    @Mock
    private MySegmentsNotificationProcessorConfiguration mConfiguration;
    @Mock
    private SyncDelayCalculator mSyncCalculator;

    private MembershipsNotificationProcessorImpl mNotificationProcessor;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        when(mCompressionUtilProvider.get(any())).thenReturn(mCompressionUtil);
        when(mMySegmentsPayloadDecoder.hashKey(anyString())).thenReturn(mHashedUserKey);
        when(mMsMembershipNotification.getJsonData()).thenReturn("{}");
        when(mLsMembershipNotification.getJsonData()).thenReturn("{}");
        when(mSplitTaskFactory.createMySegmentsUpdateTask(anyBoolean(), anySet(), anyLong()))
                .thenReturn(mock(MySegmentsUpdateTask.class));
        when(mSplitTaskFactory.createMySegmentsOverwriteTask(any()))
                .thenReturn(mock(MySegmentsOverwriteTask.class));
        when(mSplitTaskFactory.createMySegmentsSyncTask(anyBoolean(), anyLong(), anyLong()))
                .thenReturn(mock(MySegmentsSyncTask.class));
        when(mConfiguration.getUserKey()).thenReturn("key");
        when(mConfiguration.getHashedUserKey()).thenReturn(mHashedUserKey);
        when(mConfiguration.getMySegmentsTaskFactory()).thenReturn(mSplitTaskFactory);
        when(mConfiguration.getNotificationsQueue()).thenReturn(mMySegmentChangeQueue);
        mNotificationProcessor = new MembershipsNotificationProcessorImpl(
                mNotificationParser,
                mSplitTaskExecutor,
                mMySegmentsPayloadDecoder,
                mCompressionUtilProvider,
                mConfiguration,
                mSyncCalculator
        );
    }

    @Test
    public void mySegmentsUpdateV2UnboundedNotification() {

        MembershipNotification mySegmentChangeNotification
                = mock(MembershipNotification.class);
        when(mySegmentChangeNotification.getUpdateStrategy()).thenReturn(MySegmentUpdateStrategy.UNBOUNDED_FETCH_REQUEST);
        when(mySegmentChangeNotification.getType()).thenReturn(NotificationType.MEMBERSHIP_MS_UPDATE);
        when(mNotificationParser.parseMembershipNotification(anyString())).thenReturn(mySegmentChangeNotification);

        mNotificationProcessor.process(mySegmentChangeNotification);

        verify(mMySegmentChangeQueue, times(1)).offer(any());
    }

    @Test
    public void mySegmentsUpdateV2RemovalNotification() {

        String segmentName = "ToRemove";
        when(mMsMembershipNotification.getUpdateStrategy()).thenReturn(MySegmentUpdateStrategy.SEGMENT_REMOVAL);
        when(mMsMembershipNotification.getNames()).thenReturn(Collections.singleton(segmentName));
        when(mMsMembershipNotification.getType()).thenReturn(NotificationType.MEMBERSHIP_MS_UPDATE);
        when(mMsMembershipNotification.getChangeNumber()).thenReturn(25L);
        when(mNotificationParser.parseMembershipNotification(anyString())).thenReturn(mMsMembershipNotification);

        mNotificationProcessor.process(mMsMembershipNotification);

        ArgumentCaptor<Set> messageCaptor =
                ArgumentCaptor.forClass(Set.class);
        verify(mSplitTaskFactory, times(1)).createMySegmentsUpdateTask(anyBoolean(), messageCaptor.capture(), eq(25L));

        Assert.assertEquals(Collections.singleton(segmentName), messageCaptor.getValue());
    }

    @Test
    public void mySegmentsUpdateV2BoundedNotificationFetch() {
        mySegmentsUpdateV2BoundedNotification(true);
        verify(mMySegmentChangeQueue, times(1)).offer(any());
    }

    @Test
    public void mySegmentsUpdateV2BoundedNotificationNoFetch() {
        mySegmentsUpdateV2BoundedNotification(false);
        verify(mSplitTaskFactory, never()).createMySegmentsSyncTask(anyBoolean(), anyLong(), anyLong());
    }

    public void mySegmentsUpdateV2BoundedNotification(boolean hasToFetch) {

        MembershipNotification mySegmentChangeNotification
                = mock(MembershipNotification.class);
        when(mySegmentChangeNotification.getUpdateStrategy()).thenReturn(MySegmentUpdateStrategy.BOUNDED_FETCH_REQUEST);
        when(mySegmentChangeNotification.getData()).thenReturn("dummy");
        when(mMsMembershipNotification.getType()).thenReturn(NotificationType.MEMBERSHIP_MS_UPDATE);
        when(mySegmentChangeNotification.getType()).thenReturn(NotificationType.MEMBERSHIP_MS_UPDATE);
        try {
            when(mMySegmentsPayloadDecoder.decodeAsBytes(anyString(), any())).thenReturn(new byte[]{});
        } catch (MySegmentsParsingException e) {
        }
        when(mMySegmentsPayloadDecoder.computeKeyIndex(any(), anyInt())).thenReturn(1);
        when(mMySegmentsPayloadDecoder.isKeyInBitmap(any(), anyInt())).thenReturn(hasToFetch);
        when(mNotificationParser.parseMembershipNotification(anyString())).thenReturn(mySegmentChangeNotification);

        mNotificationProcessor.process(mySegmentChangeNotification);

    }

    @Test
    public void mySegmentsUpdateV2KeyListNotificationAdd() {
        String segment = "TheSegment";
        mySegmentsUpdateV2KeyListNotification(segment, ADD);
        verify(mSplitTaskFactory, times(1)).createMySegmentsUpdateTask(true, Collections.singleton(segment), 123456L);
    }

    @Test
    public void mySegmentsUpdateV2KeyListNotificationRemove() {
        String segment = "TheSegment";
        mySegmentsUpdateV2KeyListNotification(segment, REMOVE);
        verify(mSplitTaskFactory, times(1)).createMySegmentsUpdateTask(false, Collections.singleton(segment), 123456L);
    }

    @Test
    public void mySegmentsUpdateV2KeyListNotificationNone() {
        mySegmentsUpdateV2KeyListNotification("", NONE);
        verify(mSplitTaskFactory, never()).createMySegmentsUpdateTask(anyBoolean(), anySet(), anyLong());
    }

    @Test
    public void mySegmentsUpdateV2KeyListNotificationErrorFallback() throws MySegmentsParsingException {

        MembershipNotification mySegmentChangeNotification
                = mock(MembershipNotification.class);
        when(mySegmentChangeNotification.getUpdateStrategy()).thenReturn(MySegmentUpdateStrategy.KEY_LIST);
        when(mySegmentChangeNotification.getNames()).thenReturn(Collections.singleton("s1"));
        when(mMsMembershipNotification.getType()).thenReturn(NotificationType.MEMBERSHIP_MS_UPDATE);
        when(mySegmentChangeNotification.getType()).thenReturn(NotificationType.MEMBERSHIP_MS_UPDATE);
        when(mMySegmentsPayloadDecoder.decodeAsString(anyString(), any())).thenThrow(MySegmentsParsingException.class);

        mNotificationProcessor.process(mMsMembershipNotification);

        verify(mMySegmentsPayloadDecoder, never()).getKeyListAction(any(), any());
        verify(mMySegmentChangeQueue, times(1)).offer(any());
    }

    @Test
    public void mySegmentsUpdateV2BoundedNotificationErrorFallback() throws MySegmentsParsingException {

        MembershipNotification mySegmentChangeNotification
                = mock(MembershipNotification.class);
        when(mySegmentChangeNotification.getUpdateStrategy()).thenReturn(MySegmentUpdateStrategy.BOUNDED_FETCH_REQUEST);
        when(mySegmentChangeNotification.getNames()).thenReturn(Collections.singleton("s1"));
        when(mMsMembershipNotification.getType()).thenReturn(NotificationType.MEMBERSHIP_MS_UPDATE);
        when(mySegmentChangeNotification.getType()).thenReturn(NotificationType.MEMBERSHIP_MS_UPDATE);
        when(mMySegmentsPayloadDecoder.decodeAsBytes(anyString(), any())).thenThrow(MySegmentsParsingException.class);

        mNotificationProcessor.process(mMsMembershipNotification);

        verify(mMySegmentsPayloadDecoder, never()).isKeyInBitmap(any(), anyInt());
        verify(mMySegmentChangeQueue, times(1)).offer(any());
    }

    @Test
    public void delayIsCalculatedWithCalculator() {
        MembershipNotification notification = mock(MembershipNotification.class);
        when(notification.getUpdateIntervalMs()).thenReturn(1000L);
        when(notification.getAlgorithmSeed()).thenReturn(1234);
        when(notification.getUpdateStrategy()).thenReturn(MySegmentUpdateStrategy.UNBOUNDED_FETCH_REQUEST);
        when(notification.getHashingAlgorithm()).thenReturn(HashingAlgorithm.MURMUR3_32);
        when(mSyncCalculator.calculateSyncDelay("key", 1000L, 1234, MySegmentUpdateStrategy.UNBOUNDED_FETCH_REQUEST, HashingAlgorithm.MURMUR3_32)).thenReturn(25L);

        mNotificationProcessor.process(notification);

        verify(mSyncCalculator).calculateSyncDelay("key", 1000L, 1234, MySegmentUpdateStrategy.UNBOUNDED_FETCH_REQUEST, HashingAlgorithm.MURMUR3_32);
    }

    private void mySegmentsUpdateV2KeyListNotification(String segmentName, KeyList.Action action) {

        when(mMsMembershipNotification.getUpdateStrategy()).thenReturn(MySegmentUpdateStrategy.KEY_LIST);
        when(mMsMembershipNotification.getNames()).thenReturn(Collections.singleton(segmentName));
        when(mMsMembershipNotification.getChangeNumber()).thenReturn(123456L);
        when(mMsMembershipNotification.getType()).thenReturn(NotificationType.MEMBERSHIP_MS_UPDATE);
        when(mMsMembershipNotification.getType()).thenReturn(NotificationType.MEMBERSHIP_MS_UPDATE);
        try {
            when(mMySegmentsPayloadDecoder.decodeAsString(anyString(), any())).thenReturn("");
        } catch (MySegmentsParsingException e) {
            e.printStackTrace();
        }
        when(mMySegmentsPayloadDecoder.getKeyListAction(any(), any())).thenReturn(action);
        when(mNotificationParser.parseMembershipNotification(anyString())).thenReturn(mMsMembershipNotification);

        mNotificationProcessor.process(mMsMembershipNotification);
    }
}
