package io.split.android.client.service.sseclient.feedbackchannel;

public class DelayStatusEvent extends PushStatusEvent {

    private final long mDelay;

    public DelayStatusEvent(long delay) {
        super(EventType.PUSH_DELAY_RECEIVED);
        mDelay = delay;
    }

    public Long getDelay() {
        return mDelay;
    }
}
