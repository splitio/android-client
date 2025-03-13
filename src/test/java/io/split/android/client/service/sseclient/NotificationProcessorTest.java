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
import io.split.android.client.service.sseclient.notifications.InstantUpdateChangeNotification;
import io.split.android.client.service.sseclient.notifications.MembershipNotification;
import io.split.android.client.service.sseclient.notifications.NotificationParser;
import io.split.android.client.service.sseclient.notifications.NotificationProcessor;
import io.split.android.client.service.sseclient.notifications.NotificationType;
import io.split.android.client.service.sseclient.notifications.RuleBasedSegmentChangeNotification;
import io.split.android.client.service.sseclient.notifications.SplitKillNotification;
import io.split.android.client.service.sseclient.notifications.SplitsChangeNotification;
import io.split.android.client.service.sseclient.notifications.memberships.MembershipsNotificationProcessor;

public class NotificationProcessorTest {

    @Mock
    private SplitTaskExecutor mSplitTaskExecutor;
    @Mock
    private SplitTaskFactory mSplitTaskFactory;
    @Mock
    private NotificationParser mNotificationParser;
    @Mock
    private BlockingQueue<InstantUpdateChangeNotification> mSplitsChangeQueue;
    @Mock
    private IncomingNotification mIncomingNotification;
    private NotificationProcessor mNotificationProcessor;

    @Before
    public void setup() {

        MockitoAnnotations.openMocks(this);
        when(mIncomingNotification.getJsonData()).thenReturn("{}");
        when(mSplitTaskFactory.createSplitKillTask(any()))
                .thenReturn(mock(SplitKillTask.class));

        mNotificationProcessor = new NotificationProcessor(mSplitTaskExecutor,
                mSplitTaskFactory, mNotificationParser,
                mSplitsChangeQueue);
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
    public void notificationProcessorDelegatesMySegmentsNotificationsV2ToRegisteredProcessors() {
        MembershipsNotificationProcessor mySegmentsNotificationProcessor = mock(MembershipsNotificationProcessor.class);
        MembershipsNotificationProcessor mySegmentsNotificationProcessor2 = mock(MembershipsNotificationProcessor.class);
        MembershipNotification mySegmentChangeNotification = mock(MembershipNotification.class);

        when(mySegmentChangeNotification.getJsonData()).thenReturn("{}");
        when(mIncomingNotification.getType()).thenReturn(NotificationType.MEMBERSHIPS_MS_UPDATE);
        when(mNotificationParser.parseMembershipNotification(anyString())).thenReturn(mySegmentChangeNotification);

        mNotificationProcessor.registerMembershipsNotificationProcessor("1", mySegmentsNotificationProcessor);
        mNotificationProcessor.registerMembershipsNotificationProcessor("2", mySegmentsNotificationProcessor2);
        mNotificationProcessor.process(mIncomingNotification);

        verify(mySegmentsNotificationProcessor).process(mySegmentChangeNotification);
    }

    @Test
    public void ruleBasedSegmentNotification() {

        RuleBasedSegmentChangeNotification notification = mock(RuleBasedSegmentChangeNotification.class);
        when(mIncomingNotification.getType()).thenReturn(NotificationType.RULE_BASED_SEGMENT_UPDATE);
        when(notification.getChangeNumber()).thenReturn(200L);
        when(notification.getType()).thenReturn(NotificationType.RULE_BASED_SEGMENT_UPDATE);
        when(mNotificationParser.parseIncoming(anyString())).thenReturn(mIncomingNotification);
        when(mNotificationParser.parseRuleBasedSegmentUpdate(anyString())).thenReturn(notification);

        mNotificationProcessor.process(mIncomingNotification);

        ArgumentCaptor<RuleBasedSegmentChangeNotification> messageCaptor =
                ArgumentCaptor.forClass(RuleBasedSegmentChangeNotification.class);
        verify(mSplitsChangeQueue, times(1)).offer(messageCaptor.capture());
        Assert.assertEquals(NotificationType.RULE_BASED_SEGMENT_UPDATE, messageCaptor.getValue().getType());
        Assert.assertEquals(200L, messageCaptor.getValue().getChangeNumber());
    }
}
