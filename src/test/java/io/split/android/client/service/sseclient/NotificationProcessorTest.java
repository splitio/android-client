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
    SyncManagerFeedbackChannel mSyncManagerFeedbackChannel;

    NotificationProcessor mNotificationProcessor;

    @Before
    public void setup() {

        RawNotification rawNotification = Mockito.mock(RawNotification.class);
        MockitoAnnotations.initMocks(this);
        when(rawNotification.getData()).thenReturn("{}");

        when(mNotificationParser.parseRawNotification(anyString())).thenReturn(rawNotification);

        when(mSplitTaskFactory.createMySegmentsUpdateTask())
                .thenReturn(Mockito.mock(MySegmentsUpdateTask.class));
        when(mSplitTaskFactory.createSplitKillTask())
                .thenReturn(Mockito.mock(SplitKillTask.class));

        mNotificationProcessor = new NotificationProcessor(mSplitTaskExecutor, mSplitTaskFactory,
                mNotificationParser, mSyncManagerFeedbackChannel);
    }

    @Test
    public void splitUpdateNotification() {
        IncomingNotification incomingNotification = Mockito.mock(IncomingNotification.class);
        when(incomingNotification.getType()).thenReturn(NotificationType.SPLIT_UPDATE);
        when(mNotificationParser.parseIncoming(anyString())).thenReturn(incomingNotification);
        when(mNotificationParser.parseSplitUpdate(anyString())).thenReturn(new SplitsChangeNotification());

        mNotificationProcessor.process("somenotification");

        ArgumentCaptor<SyncManagerFeedbackMessage> messageCaptor =
                ArgumentCaptor.forClass(SyncManagerFeedbackMessage.class);
        verify(mSyncManagerFeedbackChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(SyncManagerFeedbackMessageType.SPLITS_UPDATED, messageCaptor.getValue().getMessage());
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

        verify(mSyncManagerFeedbackChannel, never()).pushMessage(any());
        verify(mSplitTaskFactory, times(1)).createMySegmentsUpdateTask();
        verify(mSplitTaskExecutor, times(1)).submit(any(), isNull());
    }

    @Test
    public void mySegmentsUpdateNoSegmentListNotification() {

        MySegmentChangeNotification mySegmentChangeNotification
                = Mockito.mock(MySegmentChangeNotification.class);
        when(mySegmentChangeNotification.isIncludesPayload()).thenReturn(false);

        IncomingNotification incomingNotification = Mockito.mock(IncomingNotification.class);
        when(incomingNotification.getType()).thenReturn(NotificationType.MY_SEGMENTS_UPDATE);
        when(mNotificationParser.parseIncoming(anyString())).thenReturn(incomingNotification);
        when(mNotificationParser.parseMySegmentUpdate(anyString())).thenReturn(mySegmentChangeNotification);

        mNotificationProcessor.process("somenotification");

        verify(mSplitTaskFactory, never()).createMySegmentsUpdateTask();
        ArgumentCaptor<SyncManagerFeedbackMessage> messageCaptor =
                ArgumentCaptor.forClass(SyncManagerFeedbackMessage.class);
        verify(mSyncManagerFeedbackChannel, times(1)).pushMessage(messageCaptor.capture());
        Assert.assertEquals(SyncManagerFeedbackMessageType.MY_SEGMENTS_UPDATED, messageCaptor.getValue().getMessage());
    }

    @Test
    public void splitKillNotification() {

        IncomingNotification incomingNotification = Mockito.mock(IncomingNotification.class);
        when(incomingNotification.getType()).thenReturn(NotificationType.SPLIT_KILL);
        when(mNotificationParser.parseIncoming(anyString())).thenReturn(incomingNotification);
        when(mNotificationParser.parseSplitKill(anyString())).thenReturn(new SplitKillNotification());

        mNotificationProcessor.process("somenotification");

        verify(mSyncManagerFeedbackChannel, never()).pushMessage(any());
        verify(mSplitTaskFactory, times(1)).createSplitKillTask();
        verify(mSplitTaskExecutor, times(1)).submit(any(), isNull());
    }
}
