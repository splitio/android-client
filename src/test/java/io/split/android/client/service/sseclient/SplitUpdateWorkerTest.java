package io.split.android.client.service.sseclient;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.service.Synchronizer;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackChannel;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackChannelImpl;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackMessage;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackMessageType;
import io.split.android.client.service.sseclient.reactor.SplitUpdatesWorker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SplitUpdateWorkerTest {

    SyncManagerFeedbackChannel mFeedbackChannel;

    SplitUpdatesWorker mWorker;

    @Mock
    Synchronizer mSynchronizer;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mFeedbackChannel = new SyncManagerFeedbackChannelImpl();
        mWorker = new SplitUpdatesWorker(mSynchronizer);
        mFeedbackChannel.register(mWorker);
    }

    @Test
    public void splitsUpdateReceived() {
        Long changeNumber = 1000L;
        mFeedbackChannel.pushMessage(new SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType.SPLITS_UPDATED, changeNumber));

        verify(mSynchronizer, times(1)).synchronizeSplits(changeNumber);
    }

    @Test
    public void otherMessageReceived() {
        mFeedbackChannel.pushMessage(new SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType.MY_SEGMENTS_UPDATED));
        mFeedbackChannel.pushMessage(new SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType.PUSH_DISABLED));
        mFeedbackChannel.pushMessage(new SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType.PUSH_DISABLED));

        verify(mSynchronizer, never()).syncronizeMySegments();
    }

    @Test
    public void noChangeNumberReceived() {
        mFeedbackChannel.pushMessage(new SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType.SPLITS_UPDATED));

        verify(mSynchronizer, never()).synchronizeSplits(anyLong());
    }

    @Test
    public void noValidChangeNumberReceived() {
        mFeedbackChannel.pushMessage(new SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType.SPLITS_UPDATED, "hi"));

        verify(mSynchronizer, never()).synchronizeSplits(anyLong());
    }
}
