package io.split.android.client.service.sseclient.feedbackchannel;

public class BroadcastedEvent {
    /***
     * This class represents a message to be pushed in the feedback channel
     */

    final private BroadcastedEventType message;

    public BroadcastedEvent(BroadcastedEventType message) {
        this.message = message;
    }

    public BroadcastedEventType getMessage() {
        return message;
    }

}
