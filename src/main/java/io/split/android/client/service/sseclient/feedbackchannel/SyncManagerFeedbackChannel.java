package io.split.android.client.service.sseclient.feedbackchannel;

public interface SyncManagerFeedbackChannel {
    void pushMessage(SyncManagerFeedbackMessage message);
    void register(SyncManagerFeedbackListener listener);
}
