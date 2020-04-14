package io.split.android.client.service.sseclient.feedbackchannel;

public class SyncManagerFeedbackMessage {
    /***
     * This class represents a message to be pushed in the feedback channel
     */

    final private SyncManagerFeedbackMessageType message;

    public SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType message) {
        this.message = message;
    }

    public SyncManagerFeedbackMessageType getMessage() {
        return message;
    }

}
