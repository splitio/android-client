package io.split.android.client.service.sseclient;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.feedbackchannel.BroadcastedEventListener;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent;

import static io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent.EventType.PUSH_DISABLED;
import static io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent.EventType.PUSH_ENABLED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SyncManagerChannelMessageTest {

    PushManagerEventBroadcaster mChannel;

    @Before
    public void setup() {
        mChannel = new PushManagerEventBroadcaster();
    }

    @Test
    public void messageReceived() {
        BroadcastedEventListener l1 = Mockito.mock(BroadcastedEventListener.class);
        BroadcastedEventListener l2 = Mockito.mock(BroadcastedEventListener.class);
        BroadcastedEventListener l3 = Mockito.mock(BroadcastedEventListener.class);

        mChannel.register(l1);
        mChannel.pushMessage(new PushStatusEvent(PUSH_ENABLED));
        mChannel.pushMessage(new PushStatusEvent(PUSH_ENABLED));

        mChannel.register(l2);
        mChannel.pushMessage(new PushStatusEvent(PUSH_DISABLED));

        mChannel.register(l3);
        mChannel.pushMessage(new PushStatusEvent(PUSH_DISABLED));

        verify(l1, times(4)).onEvent(any(PushStatusEvent.class));
        verify(l2, times(2)).onEvent(any(PushStatusEvent.class));
        verify(l3, times(1)).onEvent(any(PushStatusEvent.class));
    }

    @Test
    public void correctMessage() {
        BroadcastedEventListener l1 = Mockito.mock(BroadcastedEventListener.class);

        PushStatusEvent m0 = new PushStatusEvent(PUSH_ENABLED);
        PushStatusEvent m1 = new PushStatusEvent(PUSH_ENABLED);
        PushStatusEvent m2 = new PushStatusEvent(PUSH_DISABLED);

        mChannel.pushMessage(m0);

        mChannel.register(l1);
        mChannel.pushMessage(m1);
        mChannel.pushMessage(m2);

        verify(l1, never()).onEvent(m0);
        verify(l1, times(1)).onEvent(m1);
        verify(l1, times(1)).onEvent(m2);
    }

    @Test
    public void noNPE() {
        BroadcastedEventListener l1 = Mockito.mock(BroadcastedEventListener.class);
        BroadcastedEventListener l2 = Mockito.mock(BroadcastedEventListener.class);

        PushStatusEvent m1 = new PushStatusEvent(PUSH_ENABLED);

        mChannel.pushMessage(m1);

        mChannel.register(l1);
        mChannel.register(l2);
        l1 = null;
        mChannel.pushMessage(m1);

        verify(l2, times(1)).onEvent(m1);
        Assert.assertNull(l1);
    }

    @After
    public void tearDown() {
        mChannel.close();
    }
}
