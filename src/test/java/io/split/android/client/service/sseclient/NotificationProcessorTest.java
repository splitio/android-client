package io.split.android.client.service.sseclient;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import helper.TestingHelper;
import io.split.android.client.exceptions.MySegmentsParsingException;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.mysegments.MySegmentsSyncTask;
import io.split.android.client.service.mysegments.MySegmentsOverwriteTask;
import io.split.android.client.service.mysegments.MySegmentsUpdateTask;
import io.split.android.client.service.splits.SplitKillTask;
import io.split.android.client.service.sseclient.notifications.IncomingNotification;
import io.split.android.client.service.sseclient.notifications.KeyList;
import io.split.android.client.service.sseclient.notifications.MySegmentChangeNotification;
import io.split.android.client.service.sseclient.notifications.MySegmentChangeV2Notification;
import io.split.android.client.service.sseclient.notifications.MySegmentsV2PayloadDecoder;
import io.split.android.client.service.sseclient.notifications.NotificationParser;
import io.split.android.client.service.sseclient.notifications.NotificationProcessor;
import io.split.android.client.service.sseclient.notifications.NotificationType;
import io.split.android.client.service.sseclient.notifications.SplitKillNotification;
import io.split.android.client.service.sseclient.notifications.SplitsChangeNotification;

import static io.split.android.client.service.sseclient.notifications.KeyList.Action.ADD;
import static io.split.android.client.service.sseclient.notifications.KeyList.Action.NONE;
import static io.split.android.client.service.sseclient.notifications.KeyList.Action.REMOVE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NotificationProcessorTest {

    @Mock
    SplitTaskExecutor mSplitTaskExecutor;

    @Mock
    SplitTaskFactory mSplitTaskFactory;

    @Mock
    NotificationParser mNotificationParser;

    @Mock
    BlockingQueue<MySegmentChangeNotification> mMySegmentChangeQueue;

    @Mock
    BlockingQueue<SplitsChangeNotification> mSplitsChangeQueue;

    @Mock
    IncomingNotification mIncomingNotification;

    @Mock
    MySegmentsV2PayloadDecoder mMySegmentsPayloadDecoder;

    String mUserKey = "603516ce-1243-400b-b919-0dce5d8aecfd";
    BigInteger mHashedUserKey = new BigInteger("11288179738259047283");

    NotificationProcessor mNotificationProcessor;

    @Before
    public void setup() {

        MockitoAnnotations.initMocks(this);
        when(mMySegmentsPayloadDecoder.hashKey(anyString())).thenReturn(mHashedUserKey);
        when(mIncomingNotification.getJsonData()).thenReturn("{}");
        when(mSplitTaskFactory.createMySegmentsUpdateTask(anyBoolean(), anyString()))
                .thenReturn(Mockito.mock(MySegmentsUpdateTask.class));
        when(mSplitTaskFactory.createMySegmentsOverwriteTask(any()))
                .thenReturn(Mockito.mock(MySegmentsOverwriteTask.class));
        when(mSplitTaskFactory.createSplitKillTask(any()))
                .thenReturn(Mockito.mock(SplitKillTask.class));

        when(mSplitTaskFactory.createMySegmentsSyncTask(anyBoolean()))
                .thenReturn(Mockito.mock(MySegmentsSyncTask.class));

        mNotificationProcessor = new NotificationProcessor(mUserKey, mSplitTaskExecutor,
                mSplitTaskFactory, mNotificationParser, mMySegmentsPayloadDecoder,
                mMySegmentChangeQueue, mSplitsChangeQueue);
    }

    @Test
    public void splitUpdateNotification() {

        SplitsChangeNotification updateNotification =  Mockito.mock(SplitsChangeNotification.class);

        when(mIncomingNotification.getType()).thenReturn(NotificationType.SPLIT_UPDATE);
        when(updateNotification.getType()).thenReturn(NotificationType.SPLIT_UPDATE);
        when(updateNotification.getChangeNumber()).thenReturn(100L);
        when(mNotificationParser.parseIncoming(anyString())).thenReturn(mIncomingNotification);
        when(mNotificationParser.parseSplitUpdate(anyString())).thenReturn(updateNotification);

        mNotificationProcessor.process(mIncomingNotification);

        ArgumentCaptor<SplitsChangeNotification> messageCaptor =
                ArgumentCaptor.forClass(SplitsChangeNotification.class);
        verify(mSplitsChangeQueue, times(1)).offer(messageCaptor.capture());
        Assert.assertEquals(NotificationType.SPLIT_UPDATE, messageCaptor.getValue().getType());
        Assert.assertEquals(100L, messageCaptor.getValue().getChangeNumber());
    }

    @Test
    public void mySegmentsUpdateWithSegmentListNotification() {
        List<String> segments = new ArrayList<>();
        segments.add("s1");
        MySegmentChangeNotification mySegmentChangeNotification
                = Mockito.mock(MySegmentChangeNotification.class);
        when(mySegmentChangeNotification.isIncludesPayload()).thenReturn(true);
        when(mySegmentChangeNotification.getSegmentList()).thenReturn(segments);
        when(mIncomingNotification.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE);
        when(mNotificationParser.parseIncoming(anyString())).thenReturn(mIncomingNotification);
        when(mNotificationParser.parseMySegmentUpdate(anyString())).thenReturn(mySegmentChangeNotification);

        mNotificationProcessor.process(mIncomingNotification);

        verify(mSplitsChangeQueue, never()).offer(any());
        verify(mSplitTaskFactory, times(1)).createMySegmentsOverwriteTask(any());
        verify(mSplitTaskExecutor, times(1)).submit(any(), isNull());
    }

    @Test
    public void mySegmentsUpdateWithNullSegmentListNotification() {
        List<String> segments = null;
        MySegmentChangeNotification mySegmentChangeNotification
                = Mockito.mock(MySegmentChangeNotification.class);
        when(mySegmentChangeNotification.isIncludesPayload()).thenReturn(true);
        when(mySegmentChangeNotification.getSegmentList()).thenReturn(segments);
        when(mIncomingNotification.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE);
        when(mNotificationParser.parseIncoming(anyString())).thenReturn(mIncomingNotification);
        when(mNotificationParser.parseMySegmentUpdate(anyString())).thenReturn(mySegmentChangeNotification);

        mNotificationProcessor.process(mIncomingNotification);

        verify(mSplitsChangeQueue, never()).offer(any());
        verify(mSplitTaskFactory, times(1)).createMySegmentsOverwriteTask(any());
        verify(mSplitTaskExecutor, times(1)).submit(any(), isNull());
    }

    @Test
    public void mySegmentsUpdateNoSegmentListNotification() {

        MySegmentChangeNotification mySegmentChangeNotification
                = Mockito.mock(MySegmentChangeNotification.class);
        when(mySegmentChangeNotification.isIncludesPayload()).thenReturn(false);
        when(mIncomingNotification.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE);
        when(mySegmentChangeNotification.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE);
        when(mNotificationParser.parseIncoming(anyString())).thenReturn(mIncomingNotification);
        when(mNotificationParser.parseMySegmentUpdate(anyString())).thenReturn(mySegmentChangeNotification);

        mNotificationProcessor.process(mIncomingNotification);

        verify(mSplitTaskFactory, never()).createMySegmentsOverwriteTask(any());
        ArgumentCaptor<MySegmentChangeNotification> messageCaptor =
                ArgumentCaptor.forClass(MySegmentChangeNotification.class);
        verify(mMySegmentChangeQueue, times(1)).offer(messageCaptor.capture());
        Assert.assertEquals(NotificationType.MY_SEGMENTS_UPDATE, messageCaptor.getValue().getType());
    }

    @Test
    public void mySegmentsUpdateV2UnboundedNotification() {

        MySegmentChangeV2Notification mySegmentChangeNotification
                = Mockito.mock(MySegmentChangeV2Notification.class);
        when(mySegmentChangeNotification.getEnvScopedType()).thenReturn(MySegmentChangeV2Notification.Type.UNBOUNDED_FETCH_REQUEST);
        when(mIncomingNotification.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE_V2);
        when(mySegmentChangeNotification.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE_V2);
        when(mNotificationParser.parseIncoming(anyString())).thenReturn(mIncomingNotification);
        when(mNotificationParser.parseMySegmentUpdateV2(anyString())).thenReturn(mySegmentChangeNotification);

        mNotificationProcessor.process(mIncomingNotification);

        ArgumentCaptor<Boolean> messageCaptor =
                ArgumentCaptor.forClass(Boolean.class);
        verify(mSplitTaskFactory, times(1)).createMySegmentsSyncTask(messageCaptor.capture());

        Assert.assertEquals(Boolean.TRUE, messageCaptor.getValue());
    }

    @Test
    public void mySegmentsUpdateV2RemovalNotification() {

        String segmentName = "ToRemove";
        MySegmentChangeV2Notification mySegmentChangeNotification
                = Mockito.mock(MySegmentChangeV2Notification.class);
        when(mySegmentChangeNotification.getEnvScopedType()).thenReturn(MySegmentChangeV2Notification.Type.SEGMENT_REMOVAL);
        when(mySegmentChangeNotification.getSegmentName()).thenReturn(segmentName);
        when(mIncomingNotification.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE_V2);
        when(mySegmentChangeNotification.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE_V2);
        when(mNotificationParser.parseIncoming(anyString())).thenReturn(mIncomingNotification);
        when(mNotificationParser.parseMySegmentUpdateV2(anyString())).thenReturn(mySegmentChangeNotification);

        mNotificationProcessor.process(mIncomingNotification);

        ArgumentCaptor<String> messageCaptor =
                ArgumentCaptor.forClass(String.class);
        verify(mSplitTaskFactory, times(1)).createMySegmentsUpdateTask(anyBoolean(), messageCaptor.capture());

        Assert.assertEquals(segmentName, messageCaptor.getValue());
    }

    @Test
    public void mySegmentsUpdateV2BoundedNotificationFetch() {
        mySegmentsUpdateV2BoundedNotification(true);
        verify(mSplitTaskFactory, times(1)).createMySegmentsSyncTask(true);
    }

    @Test
    public void mySegmentsUpdateV2BoundedNotificationNoFetch() {
        mySegmentsUpdateV2BoundedNotification(false);

        verify(mSplitTaskFactory, never()).createMySegmentsSyncTask(anyBoolean());
    }

    public void mySegmentsUpdateV2BoundedNotification(boolean hasToFetch) {

        String segmentName = "ToRemove";
        MySegmentChangeV2Notification mySegmentChangeNotification
                = Mockito.mock(MySegmentChangeV2Notification.class);
        when(mySegmentChangeNotification.getEnvScopedType()).thenReturn(MySegmentChangeV2Notification.Type.BOUNDED_FETCH_REQUEST);
        when(mySegmentChangeNotification.getData()).thenReturn("dummy");
        when(mIncomingNotification.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE_V2);
        when(mySegmentChangeNotification.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE_V2);
        when(mMySegmentsPayloadDecoder.computeKeyIndex(any(), anyInt())).thenReturn(1);
        when(mMySegmentsPayloadDecoder.isKeyInBitmap(any(), anyInt())).thenReturn(hasToFetch);
        when(mNotificationParser.parseMySegmentUpdateV2(anyString())).thenReturn(mySegmentChangeNotification);

        mNotificationProcessor.process(mIncomingNotification);

    }

    @Test
    public void mySegmentsUpdateV2KeyListNotificationAdd() {
        String segment = "TheSegment";
        mySegmentsUpdateV2KeyListNotification(segment, ADD);
        verify(mSplitTaskFactory, times(1)).createMySegmentsUpdateTask(true, segment);
    }

    @Test
    public void mySegmentsUpdateV2KeyListNotificationRemove() {
        String segment = "TheSegment";
        mySegmentsUpdateV2KeyListNotification(segment, REMOVE);
        verify(mSplitTaskFactory, times(1)).createMySegmentsUpdateTask(false, segment);
    }

    @Test
    public void mySegmentsUpdateV2KeyListNotificationNone() {
        mySegmentsUpdateV2KeyListNotification("", NONE);
        verify(mSplitTaskFactory, never()).createMySegmentsUpdateTask(anyBoolean(), anyString());
    }

    public void mySegmentsUpdateV2KeyListNotification(String segmentName, KeyList.Action action) {

        MySegmentChangeV2Notification mySegmentChangeNotification
                = Mockito.mock(MySegmentChangeV2Notification.class);
        when(mySegmentChangeNotification.getEnvScopedType()).thenReturn(MySegmentChangeV2Notification.Type.KEY_LIST);
        when(mySegmentChangeNotification.getSegmentName()).thenReturn(segmentName);
        when(mIncomingNotification.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE_V2);
        when(mySegmentChangeNotification.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE_V2);
        when(mMySegmentsPayloadDecoder.getKeyListAction(any(), any())).thenReturn(action);
        when(mNotificationParser.parseMySegmentUpdateV2(anyString())).thenReturn(mySegmentChangeNotification);

        mNotificationProcessor.process(mIncomingNotification);
    }

    @Test
    public void splitKillNotification() {
        when(mIncomingNotification.getType()).thenReturn(NotificationType.SPLIT_KILL);
        when(mNotificationParser.parseIncoming(anyString())).thenReturn(mIncomingNotification);
        when(mNotificationParser.parseSplitKill(anyString())).thenReturn(new SplitKillNotification());

        mNotificationProcessor.process(mIncomingNotification);

        verify(mMySegmentChangeQueue, never()).offer(any());
        verify(mSplitTaskFactory, times(1)).createSplitKillTask(any());
        verify(mSplitTaskExecutor, times(1)).submit(any(), isNull());
    }
}
