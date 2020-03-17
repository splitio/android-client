package io.split.android.client.service.sseclient.feedbackchannel;

public class SyncManagerFeedbackMessage {
    private SyncManagerFeedbackMessageType message;

    public SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType message) {
        this.message = message;
    }

    public SyncManagerFeedbackMessageType getMessage() {
        return message;
    }
}
