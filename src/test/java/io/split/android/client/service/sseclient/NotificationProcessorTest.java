package io.split.android.client.service.sseclient;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.mysegments.MySegmentsUpdateTask;
import io.split.android.client.service.splits.SplitKillTask;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackChannel;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackMessage;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackMessageType;
import io.split.android.client.service.sseclient.notifications.IncomingNotification;
import io.split.android.client.service.sseclient.notifications.MySegmentChangeNotification;
import io.split.android.client.service.sseclient.notifications.NotificationParser;
import io.split.android.client.service.sseclient.notifications.NotificationProcessor;
import io.split.android.client.service.sseclient.notifications.NotificationType;
import io.split.android.client.service.sseclient.notifications.RawNotification;
import io.split.android.client.service.sseclient.notifications.SplitKillNotification;
import io.split.android.client.service.sseclient.notifications.SplitsChangeNotification;

import static org.mockito.ArgumentMatchers.any;
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

    NotificationProcessor mNotificationProcessor;

    @Before
    public void setup() {

        RawNotification rawNotification = Mockito.mock(RawNotification.class);
        MockitoAnnotations.initMocks(this);
        when(rawNotification.getData()).thenReturn("{}");

        when(mNotificationParser.parseRawNotification(anyString())).thenReturn(rawNotification);

        when(mSplitTaskFactory.createMySegmentsUpdateTask(any()))
                .thenReturn(Mockito.mock(MySegmentsUpdateTask.class));
        when(mSplitTaskFactory.createSplitKillTask(any()))
                .thenReturn(Mockito.mock(SplitKillTask.class));

        mNotificationProcessor = new NotificationProcessor(mSplitTaskExecutor,
                mSplitTaskFactory, mNotificationParser,
                mMySegmentChangeQueue, mSplitsChangeQueue);
    }

    @Test
    public void splitUpdateNotification() {

        SplitsChangeNotification updateNotification =  Mockito.mock(SplitsChangeNotification.class);

        IncomingNotification incomingNotification = Mockito.mock(IncomingNotification.class);
        when(incomingNotification.getType()).thenReturn(NotificationType.SPLIT_UPDATE);
        when(updateNotification.getType()).thenReturn(NotificationType.SPLIT_UPDATE);
        when(updateNotification.getChangeNumber()).thenReturn(100L);
        when(mNotificationParser.parseIncoming(anyString())).thenReturn(incomingNotification);
        when(mNotificationParser.parseSplitUpdate(anyString())).thenReturn(updateNotification);

        mNotificationProcessor.process("somenotification");

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
        IncomingNotification incomingNotification = Mockito.mock(IncomingNotification.class);
        when(incomingNotification.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE);
        when(mNotificationParser.parseIncoming(anyString())).thenReturn(incomingNotification);
        when(mNotificationParser.parseMySegmentUpdate(anyString())).thenReturn(mySegmentChangeNotification);

        mNotificationProcessor.process("somenotification");

        verify(mSplitsChangeQueue, never()).offer(any());
        verify(mSplitTaskFactory, times(1)).createMySegmentsUpdateTask(any());
        verify(mSplitTaskExecutor, times(1)).submit(any(), isNull());
    }

    @Test
    public void mySegmentsUpdateNoSegmentListNotification() {

        MySegmentChangeNotification mySegmentChangeNotification
                = Mockito.mock(MySegmentChangeNotification.class);
        when(mySegmentChangeNotification.isIncludesPayload()).thenReturn(false);

        IncomingNotification incomingNotification = Mockito.mock(IncomingNotification.class);
        when(incomingNotification.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE);
        when(mySegmentChangeNotification.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE);
        when(mNotificationParser.parseIncoming(anyString())).thenReturn(incomingNotification);
        when(mNotificationParser.parseMySegmentUpdate(anyString())).thenReturn(mySegmentChangeNotification);

        mNotificationProcessor.process("somenotification");

        verify(mSplitTaskFactory, never()).createMySegmentsUpdateTask(any());
        ArgumentCaptor<MySegmentChangeNotification> messageCaptor =
                ArgumentCaptor.forClass(MySegmentChangeNotification.class);
        verify(mMySegmentChangeQueue, times(1)).offer(messageCaptor.capture());
        Assert.assertEquals(NotificationType.MY_SEGMENTS_UPDATE, messageCaptor.getValue().getType());
    }

    @Test
    public void splitKillNotification() {

        IncomingNotification incomingNotification = Mockito.mock(IncomingNotification.class);
        when(incomingNotification.getType()).thenReturn(NotificationType.SPLIT_KILL);
        when(mNotificationParser.parseIncoming(anyString())).thenReturn(incomingNotification);
        when(mNotificationParser.parseSplitKill(anyString())).thenReturn(new SplitKillNotification());

        mNotificationProcessor.process("somenotification");

        verify(mMySegmentChangeQueue, never()).offer(any());
        verify(mSplitTaskFactory, times(1)).createSplitKillTask(any());
        verify(mSplitTaskExecutor, times(1)).submit(any(), isNull());
    }
}
