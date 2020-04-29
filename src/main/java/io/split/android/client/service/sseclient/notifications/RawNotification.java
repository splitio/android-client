package io.split.android.client.service.sseclient.notifications;

public class RawNotification {

    private String clientId;
    private String name;
    private String data;
    private String channel;
    private long timestamp;

    public String getClientId() {
        return clientId;
    }

    public String getData() {
        return data;
    }

    public String getChannel() {
        return channel;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getName() {
        return name;
    }
}
