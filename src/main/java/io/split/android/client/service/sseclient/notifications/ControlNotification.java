package io.split.android.client.service.sseclient.notifications;

public class ControlNotification extends IncomingNotification {
    public enum ControlType {
        STREAMING_RESUMED, STREAMING_DISABLED, STREAMING_PAUSED, STREAMING_RESET
    }

    @SuppressWarnings("unused")
    private ControlType controlType;

    public ControlType getControlType() {
        return controlType;
    }

    public void setTimestamp(long timestamp) {
        super.timestamp = timestamp;
    }
}
