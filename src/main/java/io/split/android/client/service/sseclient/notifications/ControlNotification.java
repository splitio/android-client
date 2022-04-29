package io.split.android.client.service.sseclient.notifications;

public class ControlNotification extends IncomingNotification {
    public static enum ControlType {
        STREAMING_ENABLED, STREAMING_DISABLED, STREAMING_PAUSED, STREAMING_RESET, STREAMING_RESUMED
    }

    private ControlType controlType;

    public ControlType getControlType() {
        return controlType;
    }

    public void setTimestamp(long timestamp) {
        super.timestamp = timestamp;
    }
}
