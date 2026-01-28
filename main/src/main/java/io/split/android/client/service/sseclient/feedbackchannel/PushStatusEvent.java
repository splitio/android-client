package io.split.android.client.service.sseclient.feedbackchannel;

public class PushStatusEvent {
    /***
     * This class represents a message to be pushed in the feedback channel
     */

    public enum EventType {
        /***
         * Types of messages that can be pushed to the
         * Synchronization feedback channel
         */
        PUSH_SUBSYSTEM_UP, PUSH_SUBSYSTEM_DOWN, PUSH_RETRYABLE_ERROR, PUSH_NON_RETRYABLE_ERROR, PUSH_DISABLED,
        PUSH_RESET,
        SUCCESSFUL_SYNC,
        PUSH_DELAY_RECEIVED,
    }

    private final EventType mMessage;

    public PushStatusEvent(EventType message) {
        mMessage = message;
    }

    public EventType getMessage() {
        return mMessage;
    }
}
