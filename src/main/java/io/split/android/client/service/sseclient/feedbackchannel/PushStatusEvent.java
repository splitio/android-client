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
        PUSH_SUBSYSTEM_UP, PUSH_SUBSYSTEM_DOWN, PUSH_RETRYABLE_ERROR, PUSH_NON_RETRYABLE_ERROR, PUSH_DISABLED
    }

    final private EventType message;

    public PushStatusEvent(EventType message) {
        this.message = message;
    }

    public EventType getMessage() {
        return message;
    }

}
