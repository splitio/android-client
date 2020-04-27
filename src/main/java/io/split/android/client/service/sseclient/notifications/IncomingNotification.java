package io.split.android.client.service.sseclient.notifications;

public class IncomingNotification extends IncomingNotificationType {
    private String jsonData;
    private String channel;
    private long timestamp;

    public IncomingNotification() {
        super();
    }

    public IncomingNotification(NotificationType type, String channel,
                                String jsonData, long timestamp) {
        super();
        this.type = type;
        this.channel = channel;
        this.jsonData = jsonData;
        this.timestamp = timestamp;
    }

    public NotificationType getType() {
        return type;
    }

    public String getJsonData() {
        return jsonData;
    }

    public String getChannel() {
        return channel;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
