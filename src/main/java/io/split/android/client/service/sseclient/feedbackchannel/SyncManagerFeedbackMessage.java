package io.split.android.client.service.sseclient.feedbackchannel;

public class SyncManagerFeedbackMessage {
    /***
     * This class represents a message to be pushed in the feedback channel
     */

    private SyncManagerFeedbackMessageType message;
    private Object data;

    public SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType message) {
        this(message, null);
    }

    public SyncManagerFeedbackMessage(SyncManagerFeedbackMessageType message,
                                      Object data) {
        this.message = message;
        this.data = data;
    }

    public SyncManagerFeedbackMessageType getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }
}
