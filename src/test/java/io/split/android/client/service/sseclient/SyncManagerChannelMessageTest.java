package io.split.android.client.service.sseclient;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackChannel;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackListener;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackMessage;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackMessageType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SyncManagerChannelMessageTest {

    SyncManagerFeedbackChannel mChannel;

    @Before
    public void setup() {
        mChannel = new SyncManagerFeedbackChannel();
    }

    @Test
    public void messageReceived() {
        SyncManagerFeedbackListener l1 = Mockito.mock(SyncManagerFeedbackListener.class);
        SyncManagerFeedbackListener l2 = Mockito.mock(SyncManagerFeedbackListener.class);
        SyncManagerFeedbackListener l3 = Mockito.mock(SyncManagerFeedbackListener.class);

        mChannel.register(l1);
        mChannel.pushMessage(new SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType.PUSH_ENABLED));
        mChannel.pushMessage(new SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType.PUSH_ENABLED));

        mChannel.register(l2);
        mChannel.pushMessage(new SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType.PUSH_DISABLED));

        mChannel.register(l3);
        mChannel.pushMessage(new SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType.PUSH_DISABLED));

        verify(l1, times(4)).onFeedbackMessage(any(SyncManagerFeedbackMessage.class));
        verify(l2, times(2)).onFeedbackMessage(any(SyncManagerFeedbackMessage.class));
        verify(l3, times(1)).onFeedbackMessage(any(SyncManagerFeedbackMessage.class));
    }

    @Test
    public void correctMessage() {
        SyncManagerFeedbackListener l1 = Mockito.mock(SyncManagerFeedbackListener.class);

        SyncManagerFeedbackMessage m0 = new SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType.PUSH_ENABLED);
        SyncManagerFeedbackMessage m1 = new SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType.PUSH_ENABLED);
        SyncManagerFeedbackMessage m2 = new SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType.PUSH_DISABLED);

        mChannel.pushMessage(m0);

        mChannel.register(l1);
        mChannel.pushMessage(m1);
        mChannel.pushMessage(m2);

        verify(l1, never()).onFeedbackMessage(m0);
        verify(l1, times(1)).onFeedbackMessage(m1);
        verify(l1, times(1)).onFeedbackMessage(m2);
    }

    @Test
    public void noNPE() {
        SyncManagerFeedbackListener l1 = Mockito.mock(SyncManagerFeedbackListener.class);
        SyncManagerFeedbackListener l2 = Mockito.mock(SyncManagerFeedbackListener.class);

        SyncManagerFeedbackMessage m1 = new SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType.PUSH_ENABLED);

        mChannel.pushMessage(m1);

        mChannel.register(l1);
        mChannel.register(l2);
        l1 = null;
        mChannel.pushMessage(m1);

        verify(l2, times(1)).onFeedbackMessage(m1);
        Assert.assertNull(l1);
    }

    @After
    public void tearDown() {
        mChannel.close();
    }
}
