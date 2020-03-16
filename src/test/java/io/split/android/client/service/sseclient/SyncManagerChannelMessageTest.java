package io.split.android.client.service.sseclient;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackChannel;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackChannelImpl;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackListener;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackMessage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SyncManagerChannelMessageTest {

    SyncManagerFeedbackChannel mChannel;

    @Before
    public void setup() {
        mChannel = new SyncManagerFeedbackChannelImpl();
    }

    @Test
    public void messageReceived() {
        SyncManagerFeedbackListener l1 = Mockito.mock(SyncManagerFeedbackListener.class);
        SyncManagerFeedbackListener l2 = Mockito.mock(SyncManagerFeedbackListener.class);
        SyncManagerFeedbackListener l3 = Mockito.mock(SyncManagerFeedbackListener.class);

        mChannel.register(l1);
        mChannel.pushMessage(new SyncManagerFeedbackMessage(1));
        mChannel.pushMessage(new SyncManagerFeedbackMessage(2));

        mChannel.register(l2);
        mChannel.pushMessage(new SyncManagerFeedbackMessage(3));

        mChannel.register(l3);
        mChannel.pushMessage(new SyncManagerFeedbackMessage(3));

        verify(l1, times(4)).onFedbackMessage(any(SyncManagerFeedbackMessage.class));
        verify(l2, times(2)).onFedbackMessage(any(SyncManagerFeedbackMessage.class));
        verify(l3, times(1)).onFedbackMessage(any(SyncManagerFeedbackMessage.class));
    }

    @Test
    public void correctMessage() {
        SyncManagerFeedbackListener l1 = Mockito.mock(SyncManagerFeedbackListener.class);

        SyncManagerFeedbackMessage m0 = new SyncManagerFeedbackMessage(0);
        SyncManagerFeedbackMessage m1 = new SyncManagerFeedbackMessage(100);
        SyncManagerFeedbackMessage m2 = new SyncManagerFeedbackMessage(200);

        mChannel.pushMessage(m0);

        mChannel.register(l1);
        mChannel.pushMessage(m1);
        mChannel.pushMessage(m2);

        verify(l1, never()).onFedbackMessage(m0);
        verify(l1, times(1)).onFedbackMessage(m1);
        verify(l1, times(1)).onFedbackMessage(m2);
    }

    @Test
    public void noNPE() {
        SyncManagerFeedbackListener l1 = Mockito.mock(SyncManagerFeedbackListener.class);
        SyncManagerFeedbackListener l2 = Mockito.mock(SyncManagerFeedbackListener.class);

        SyncManagerFeedbackMessage m1 = new SyncManagerFeedbackMessage(100);

        mChannel.pushMessage(m1);

        mChannel.register(l1);
        mChannel.register(l2);
        l1 = null;
        mChannel.pushMessage(m1);

        verify(l2, times(1)).onFedbackMessage(m1);
        Assert.assertNull(l1);
    }

    @After
    public void tearDown() {
        mChannel.close();
    }
}
