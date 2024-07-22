package io.split.android.client.service.sseclient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.BlockingQueue;

import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.splits.SplitKillTask;
import io.split.android.client.service.sseclient.notifications.IncomingNotification;
import io.split.android.client.service.sseclient.notifications.MyLargeSegmentChangeNotification;
import io.split.android.client.service.sseclient.notifications.MySegmentChangeNotification;
import io.split.android.client.service.sseclient.notifications.MySegmentChangeV2Notification;
import io.split.android.client.service.sseclient.notifications.MySegmentsPayloadDecoder;
import io.split.android.client.service.sseclient.notifications.NotificationParser;
import io.split.android.client.service.sseclient.notifications.NotificationProcessor;
import io.split.android.client.service.sseclient.notifications.NotificationType;
import io.split.android.client.service.sseclient.notifications.SplitKillNotification;
import io.split.android.client.service.sseclient.notifications.SplitsChangeNotification;
import io.split.android.client.service.sseclient.notifications.mysegments.MyLargeSegmentsNotificationProcessor;
import io.split.android.client.service.sseclient.notifications.mysegments.MySegmentsNotificationProcessor;

public class NotificationProcessorTest {

    @Mock
    private SplitTaskExecutor mSplitTaskExecutor;
    @Mock
    private SplitTaskFactory mSplitTaskFactory;
    @Mock
    private NotificationParser mNotificationParser;
    @Mock
    private BlockingQueue<SplitsChangeNotification> mSplitsChangeQueue;
    @Mock
    private IncomingNotification mIncomingNotification;
    @Mock
    private MySegmentsPayloadDecoder mMySegmentsPayloadDecoder;
    private NotificationProcessor mNotificationProcessor;

    @Before
    public void setup() {

        MockitoAnnotations.openMocks(this);
        when(mIncomingNotification.getJsonData()).thenReturn("{}");
        when(mSplitTaskFactory.createSplitKillTask(any()))
                .thenReturn(mock(SplitKillTask.class));

        mNotificationProcessor = new NotificationProcessor(mSplitTaskExecutor,
                mSplitTaskFactory, mNotificationParser,
                mSplitsChangeQueue, mMySegmentsPayloadDecoder);
    }

    @Test
    public void splitUpdateNotification() {

        SplitsChangeNotification updateNotification = mock(SplitsChangeNotification.class);

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
    public void splitKillNotification() {
        when(mIncomingNotification.getType()).thenReturn(NotificationType.SPLIT_KILL);
        when(mNotificationParser.parseIncoming(anyString())).thenReturn(mIncomingNotification);
        when(mNotificationParser.parseSplitKill(anyString())).thenReturn(new SplitKillNotification());

        mNotificationProcessor.process(mIncomingNotification);

        verify(mSplitTaskFactory, times(1)).createSplitKillTask(any());
        verify(mSplitTaskExecutor, times(1)).submit(any(), isNull());
    }

    @Test
    public void notificationProcessorDelegatesRegisteredProcessorDependingOnKey() {
        MySegmentsNotificationProcessor mySegmentsNotificationProcessor = mock(MySegmentsNotificationProcessor.class);
        MySegmentsNotificationProcessor mySegmentsNotificationProcessor2 = mock(MySegmentsNotificationProcessor.class);
        MySegmentChangeNotification mySegmentChangeNotification = mock(MySegmentChangeNotification.class);
        when(mNotificationParser.extractUserKeyHashFromChannel("a_b_MjAwNjI0Nzg3NQ==_mySegments")).thenReturn("MjAwNjI0Nzg3NQ==");
        when(mIncomingNotification.getChannel()).thenReturn("a_b_MjAwNjI0Nzg3NQ==_mySegments");
        when(mMySegmentsPayloadDecoder.hashUserKeyForMySegmentsV1("user_key")).thenReturn("MjAwNjI0Nzg3NQ==");

        when(mySegmentChangeNotification.getJsonData()).thenReturn("{}");
        when(mIncomingNotification.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE);
        when(mNotificationParser.parseMySegmentUpdate(anyString())).thenReturn(mySegmentChangeNotification);

        mNotificationProcessor.registerMySegmentsProcessor("user_key", mySegmentsNotificationProcessor);
        mNotificationProcessor.registerMySegmentsProcessor("button_key", mySegmentsNotificationProcessor2);
        mNotificationProcessor.process(mIncomingNotification);

        verify(mySegmentsNotificationProcessor).processMySegmentsUpdate(mySegmentChangeNotification);
    }

    @Test
    public void notificationProcessorDelegatesMySegmentsNotificationsV2ToRegisteredProcessors() {
        MySegmentsNotificationProcessor mySegmentsNotificationProcessor = mock(MySegmentsNotificationProcessor.class);
        MySegmentsNotificationProcessor mySegmentsNotificationProcessor2 = mock(MySegmentsNotificationProcessor.class);
        MySegmentChangeV2Notification mySegmentChangeNotification = mock(MySegmentChangeV2Notification.class);

        when(mySegmentChangeNotification.getJsonData()).thenReturn("{}");
        when(mIncomingNotification.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE_V2);
        when(mNotificationParser.parseMySegmentUpdateV2(anyString())).thenReturn(mySegmentChangeNotification);

        mNotificationProcessor.registerMySegmentsProcessor("1", mySegmentsNotificationProcessor);
        mNotificationProcessor.registerMySegmentsProcessor("2", mySegmentsNotificationProcessor2);
        mNotificationProcessor.process(mIncomingNotification);

        verify(mySegmentsNotificationProcessor).processMySegmentsUpdateV2(mySegmentChangeNotification);
    }

    @Test
    public void notificationProcessorDelegatesLargeSegmentsNotificationToRegisteredProcessors() {
        MyLargeSegmentsNotificationProcessor mySegmentsNotificationProcessor = mock(MyLargeSegmentsNotificationProcessor.class);
        MyLargeSegmentsNotificationProcessor mySegmentsNotificationProcessor2 = mock(MyLargeSegmentsNotificationProcessor.class);
        MyLargeSegmentChangeNotification mySegmentChangeNotification = mock(MyLargeSegmentChangeNotification.class);

        when(mySegmentChangeNotification.getData()).thenReturn("{}");
        when(mIncomingNotification.getType()).thenReturn(NotificationType.MY_LARGE_SEGMENT_UPDATE);
        when(mNotificationParser.parseMyLargeSegmentUpdate(anyString())).thenReturn(mySegmentChangeNotification);

        mNotificationProcessor.registerMyLargeSegmentsProcessor("1", mySegmentsNotificationProcessor);
        mNotificationProcessor.registerMyLargeSegmentsProcessor("2", mySegmentsNotificationProcessor2);
        mNotificationProcessor.process(mIncomingNotification);

        verify(mySegmentsNotificationProcessor).process(mySegmentChangeNotification);
        verify(mySegmentsNotificationProcessor2).process(mySegmentChangeNotification);
    }
}
