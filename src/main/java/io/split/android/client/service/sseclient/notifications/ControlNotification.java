package io.split.android.client.service.sseclient.notifications;

public class ControlNotification {
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
