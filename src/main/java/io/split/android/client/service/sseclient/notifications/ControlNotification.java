package io.split.android.client.service.sseclient.notifications;

public class ControlNotification extends IncomingNotification {
    public static enum ControlType {
        STREAMING_ENABLED, STREAMING_DISABLED, STREAMING_PAUSED
    }

    private ControlType controlType;

    public ControlType getControlType() {
        return controlType;
    }
}

