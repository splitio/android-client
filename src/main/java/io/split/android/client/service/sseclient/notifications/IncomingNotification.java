package io.split.android.client.service.sseclient.notifications;

public class IncomingNotification extends IncomingNotificationType {
    private String jsonData;
    private String channel;

    public IncomingNotification() {
        super();
    }

    public IncomingNotification(NotificationType type, String channel, String jsonData) {
        super();
        this.type = type;
        this.channel = channel;
        this.jsonData = jsonData;
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
}
