package io.split.android.client.localhost;

import io.split.android.engine.metrics.Metrics;

public class LocalhostMetrics implements Metrics {
    @Override
    public void count(String counter, long delta) {
    }

    @Override
    public void time(String operation, long timeInMs) {
    }
}
