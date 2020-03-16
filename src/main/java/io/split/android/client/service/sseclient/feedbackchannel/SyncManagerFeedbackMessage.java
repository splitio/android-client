package io.split.android.client.service.sseclient.feedbackchannel;

public class SyncManagerFeedbackMessage {
    private int message;

    public SyncManagerFeedbackMessage(int message) {
        this.message = message;
    }

    public int getMessage() {
        return message;
    }
}
