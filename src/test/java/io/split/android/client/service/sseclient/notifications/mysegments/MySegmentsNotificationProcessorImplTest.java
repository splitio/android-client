package io.split.android.client.service.sseclient.notifications.mysegments;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import io.split.android.client.common.CompressionType;
import io.split.android.client.common.CompressionUtilProvider;
import io.split.android.client.exceptions.MySegmentsParsingException;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.mysegments.MySegmentUpdateParams;
import io.split.android.client.service.mysegments.MySegmentsOverwriteTask;
import io.split.android.client.service.mysegments.MySegmentsSyncTask;
import io.split.android.client.service.mysegments.MySegmentsTaskFactory;
import io.split.android.client.service.mysegments.MySegmentsUpdateTask;
import io.split.android.client.service.sseclient.notifications.KeyList;
import io.split.android.client.service.sseclient.notifications.MySegmentUpdateStrategy;
import io.split.android.client.service.sseclient.notifications.MySegmentsV2PayloadDecoder;
import io.split.android.client.service.sseclient.notifications.NotificationParser;
import io.split.android.client.service.sseclient.notifications.NotificationType;
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
    private MySegmentChangeNotification mIncomingNotification;
    @Mock
    private MySegmentChangeV2Notification mIncomingNotificationV2;
    @Mock
    private MySegmentsNotificationProcessorConfiguration mConfiguration;

    private MySegmentsNotificationProcessorImpl mNotificationProcessor;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        when(mCompressionUtilProvider.get(any())).thenReturn(mCompressionUtil);
        when(mMySegmentsPayloadDecoder.hashKey(anyString())).thenReturn(mHashedUserKey);
        when(mIncomingNotification.getJsonData()).thenReturn("{}");
        when(mIncomingNotificationV2.getJsonData()).thenReturn("{}");
        when(mSplitTaskFactory.createMySegmentsUpdateTask(anyBoolean(), anySet(), anyLong()))
                .thenReturn(mock(MySegmentsUpdateTask.class));
        when(mSplitTaskFactory.createMySegmentsOverwriteTask(any()))
                .thenReturn(mock(MySegmentsOverwriteTask.class));
        when(mSplitTaskFactory.createMySegmentsSyncTask(anyBoolean(), anyLong(), anyLong()))
                .thenReturn(mock(MySegmentsSyncTask.class));
        when(mConfiguration.getHashedUserKey()).thenReturn(mHashedUserKey);
        when(mConfiguration.getMySegmentsTaskFactory()).thenReturn(mSplitTaskFactory);
        when(mConfiguration.getNotificationsQueue()).thenReturn(mMySegmentChangeQueue);
        mNotificationProcessor = new MySegmentsNotificationProcessorImpl(
                mNotificationParser,
                mSplitTaskExecutor,
                mMySegmentsPayloadDecoder,
                mCompressionUtilProvider,
                mConfiguration
        );
    }

    @Test
    public void mySegmentsUpdateWithSegmentListNotification() {
        List<String> segments = new ArrayList<>();
        segments.add("s1");
        when(mIncomingNotification.isIncludesPayload()).thenReturn(true);
        when(mIncomingNotification.getSegmentList()).thenReturn(segments);
        when(mIncomingNotification.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE);
        when(mNotificationParser.parseMySegmentUpdate(anyString())).thenReturn(mIncomingNotification);

        mNotificationProcessor.processMySegmentsUpdate(mIncomingNotification);

        verify(mSplitTaskFactory, times(1)).createMySegmentsOverwriteTask(any());
        verify(mSplitTaskExecutor, times(1)).submit(any(), isNull());
    }

    @Test
    public void mySegmentsUpdateWithNullSegmentListNotification() {
        List<String> segments = null;
        when(mIncomingNotification.isIncludesPayload()).thenReturn(true);
        when(mIncomingNotification.getSegmentList()).thenReturn(segments);
        when(mIncomingNotification.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE);
        when(mNotificationParser.parseMySegmentUpdate(anyString())).thenReturn(mIncomingNotification);

        mNotificationProcessor.processMySegmentsUpdate(mIncomingNotification);

        verify(mSplitTaskFactory, times(1)).createMySegmentsOverwriteTask(any());
        verify(mSplitTaskExecutor, times(1)).submit(any(), isNull());
    }

    @Test
    public void mySegmentsUpdateNoSegmentListNotification() {

        MySegmentChangeNotification mySegmentChangeNotification
                = mock(MySegmentChangeNotification.class);
        when(mySegmentChangeNotification.isIncludesPayload()).thenReturn(false);
        when(mIncomingNotification.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE);
        when(mySegmentChangeNotification.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE);
        when(mNotificationParser.parseMySegmentUpdate(anyString())).thenReturn(mySegmentChangeNotification);

        mNotificationProcessor.processMySegmentsUpdate(mySegmentChangeNotification);

        verify(mSplitTaskFactory, never()).createMySegmentsOverwriteTask(any());
        ArgumentCaptor<MySegmentUpdateParams> messageCaptor =
                ArgumentCaptor.forClass(MySegmentUpdateParams.class);
        verify(mMySegmentChangeQueue, times(1)).offer(messageCaptor.capture());
    }

    @Test
    public void mySegmentsUpdateV2UnboundedNotification() {

        MySegmentChangeV2Notification mySegmentChangeNotification
                = mock(MySegmentChangeV2Notification.class);
        when(mySegmentChangeNotification.getUpdateStrategy()).thenReturn(MySegmentUpdateStrategy.UNBOUNDED_FETCH_REQUEST);
        when(mySegmentChangeNotification.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE_V2);
        when(mNotificationParser.parseMySegmentUpdateV2(anyString())).thenReturn(mySegmentChangeNotification);

        mNotificationProcessor.processMySegmentsUpdateV2(mySegmentChangeNotification);

        verify(mMySegmentChangeQueue, times(1)).offer(any());
    }

    @Test
    public void mySegmentsUpdateV2RemovalNotification() {

        String segmentName = "ToRemove";
        when(mIncomingNotificationV2.getUpdateStrategy()).thenReturn(MySegmentUpdateStrategy.SEGMENT_REMOVAL);
        when(mIncomingNotificationV2.getSegmentName()).thenReturn(segmentName);
        when(mIncomingNotificationV2.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE_V2);
        when(mIncomingNotificationV2.getChangeNumber()).thenReturn(25L);
        when(mNotificationParser.parseMySegmentUpdateV2(anyString())).thenReturn(mIncomingNotificationV2);

        mNotificationProcessor.processMySegmentsUpdateV2(mIncomingNotificationV2);

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

        MySegmentChangeV2Notification mySegmentChangeNotification
                = mock(MySegmentChangeV2Notification.class);
        when(mySegmentChangeNotification.getUpdateStrategy()).thenReturn(MySegmentUpdateStrategy.BOUNDED_FETCH_REQUEST);
        when(mySegmentChangeNotification.getData()).thenReturn("dummy");
        when(mIncomingNotificationV2.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE_V2);
        when(mySegmentChangeNotification.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE_V2);
        try {
            when(mMySegmentsPayloadDecoder.decodeAsBytes(anyString(), any())).thenReturn(new byte[]{});
        } catch (MySegmentsParsingException e) {
        }
        when(mMySegmentsPayloadDecoder.computeKeyIndex(any(), anyInt())).thenReturn(1);
        when(mMySegmentsPayloadDecoder.isKeyInBitmap(any(), anyInt())).thenReturn(hasToFetch);
        when(mNotificationParser.parseMySegmentUpdateV2(anyString())).thenReturn(mySegmentChangeNotification);

        mNotificationProcessor.processMySegmentsUpdateV2(mySegmentChangeNotification);

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

        MySegmentChangeV2Notification mySegmentChangeNotification
                = mock(MySegmentChangeV2Notification.class);
        when(mySegmentChangeNotification.getUpdateStrategy()).thenReturn(MySegmentUpdateStrategy.KEY_LIST);
        when(mySegmentChangeNotification.getSegmentName()).thenReturn("s1");
        when(mIncomingNotificationV2.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE_V2);
        when(mySegmentChangeNotification.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE_V2);
        when(mMySegmentsPayloadDecoder.decodeAsString(anyString(), any())).thenThrow(MySegmentsParsingException.class);

        mNotificationProcessor.processMySegmentsUpdateV2(mIncomingNotificationV2);

        verify(mMySegmentsPayloadDecoder, never()).getKeyListAction(any(), any());
        verify(mMySegmentChangeQueue, times(1)).offer(any());
    }

    @Test
    public void mySegmentsUpdateV2BoundedNotificationErrorFallback() throws MySegmentsParsingException {

        MySegmentChangeV2Notification mySegmentChangeNotification
                = mock(MySegmentChangeV2Notification.class);
        when(mySegmentChangeNotification.getUpdateStrategy()).thenReturn(MySegmentUpdateStrategy.BOUNDED_FETCH_REQUEST);
        when(mySegmentChangeNotification.getSegmentName()).thenReturn("s1");
        when(mIncomingNotificationV2.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE_V2);
        when(mySegmentChangeNotification.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE_V2);
        when(mMySegmentsPayloadDecoder.decodeAsBytes(anyString(), any())).thenThrow(MySegmentsParsingException.class);

        mNotificationProcessor.processMySegmentsUpdateV2(mIncomingNotificationV2);

        verify(mMySegmentsPayloadDecoder, never()).isKeyInBitmap(any(), anyInt());
        verify(mMySegmentChangeQueue, times(1)).offer(any());
    }

    @Test
    public void processMySegmentsUpdateV2DelegatesToProcessorHelper() {
        MySegmentChangeV2Notification notification = mock(MySegmentChangeV2Notification.class);
        when(notification.getUpdateStrategy()).thenReturn(MySegmentUpdateStrategy.UNBOUNDED_FETCH_REQUEST);
        when(notification.getData()).thenReturn("dummy");
        when(notification.getSegmentName()).thenReturn("dummy");
        when(notification.getChangeNumber()).thenReturn(267L);
        when(notification.getCompression()).thenReturn(CompressionType.GZIP);
        when(notification.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE_V2);

        MySegmentsNotificationProcessorHelper helper = mock(MySegmentsNotificationProcessorHelper.class);
        mNotificationProcessor = new MySegmentsNotificationProcessorImpl(helper,
                mSplitTaskExecutor, mConfiguration);

        mNotificationProcessor.processMySegmentsUpdateV2(notification);

        verify(helper).processMySegmentsUpdate(MySegmentUpdateStrategy.UNBOUNDED_FETCH_REQUEST, "dummy", CompressionType.GZIP, Collections.singleton("dummy"), 267L, mMySegmentChangeQueue, 0);
        verify(helper, times(0)).processMyLargeSegmentsUpdate(any(), any(), any(), any(), any(), any(), anyLong());
    }

    private void mySegmentsUpdateV2KeyListNotification(String segmentName, KeyList.Action action) {

        when(mIncomingNotificationV2.getUpdateStrategy()).thenReturn(MySegmentUpdateStrategy.KEY_LIST);
        when(mIncomingNotificationV2.getSegmentName()).thenReturn(segmentName);
        when(mIncomingNotificationV2.getChangeNumber()).thenReturn(123456L);
        when(mIncomingNotificationV2.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE_V2);
        when(mIncomingNotificationV2.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE_V2);
        try {
            when(mMySegmentsPayloadDecoder.decodeAsString(anyString(), any())).thenReturn("");
        } catch (MySegmentsParsingException e) {
            e.printStackTrace();
        }
        when(mMySegmentsPayloadDecoder.getKeyListAction(any(), any())).thenReturn(action);
        when(mNotificationParser.parseMySegmentUpdateV2(anyString())).thenReturn(mIncomingNotificationV2);

        mNotificationProcessor.processMySegmentsUpdateV2(mIncomingNotificationV2);
    }
}
