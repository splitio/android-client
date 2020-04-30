package io.split.android.client.service.sseclient.notifications;

public class OccupancyNotification extends IncomingNotification {

    public OccupancyNotification() {
        super();
        this.type = NotificationType.OCCUPANCY;
    }

    public static class Metrics {
        private int publishers;
        public int getPublishers() {
            return publishers;
        }
    }

    private Metrics metrics;

    public Metrics getMetrics() {
        return metrics;
    }
}
