package io.split.android.client.service.sseclient.feedbackchannel;

public class PushStatusEvent {
    /***
     * This class represents a message to be pushed in the feedback channel
     */

    public static enum EventType {
        /***
         * Types of messages that can be pushed to the
         * Synchronization feedback channel
         */
        PUSH_ENABLED, PUSH_DISABLED
    }

    final private EventType message;

    public PushStatusEvent(EventType message) {
        this.message = message;
    }

    public EventType getMessage() {
        return message;
    }

}
