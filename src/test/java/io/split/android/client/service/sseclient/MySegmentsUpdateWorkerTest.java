package io.split.android.client.service.sseclient;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.service.synchronizer.Synchronizer;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackChannel;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackChannelImpl;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackMessage;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackMessageType;
import io.split.android.client.service.sseclient.reactor.MySegmentsUpdateWorker;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class MySegmentsUpdateWorkerTest {

    SyncManagerFeedbackChannel mFeedbackChannel;

    MySegmentsUpdateWorker mWorker;

    @Mock
    Synchronizer mSynchronizer;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mFeedbackChannel = new SyncManagerFeedbackChannelImpl();
        mWorker = new MySegmentsUpdateWorker(mSynchronizer);
        mFeedbackChannel.register(mWorker);
    }

    @Test
    public void mySegmentsUpdateReceived() {

        mFeedbackChannel.pushMessage(new SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType.MY_SEGMENTS_UPDATED));
        verify(mSynchronizer, times(1)).syncronizeMySegments();
    }

    @Test
    public void otherMessageReceived() {

        mFeedbackChannel.pushMessage(new SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType.SPLITS_UPDATED));
        mFeedbackChannel.pushMessage(new SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType.PUSH_DISABLED));
        mFeedbackChannel.pushMessage(new SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType.PUSH_DISABLED));

        verify(mSynchronizer, never()).syncronizeMySegments();
    }
}
