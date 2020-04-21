package io.split.android.client.service.sseclient.notifications;

public class IncomingNotification {
    private NotificationType type;
    private String jsonData;

    public IncomingNotification() {
    }

    public IncomingNotification(NotificationType type, String jsonData) {
        this.type = type;
        this.jsonData = jsonData;
    }

    public NotificationType getType() {
        return type;
    }

    public String getJsonData() {
        return jsonData;
    }
}
