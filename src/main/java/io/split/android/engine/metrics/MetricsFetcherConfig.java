package io.split.android.engine.metrics;

import androidx.annotation.NonNull;

import static com.google.common.base.Preconditions.checkNotNull;

public class MetricsFetcherConfig {
    private final String exceptionLabel;
    private final String timeLabel;
    private final String statusLabel;

    public MetricsFetcherConfig(@NonNull String exceptionLabel,
                                @NonNull String timeLabel,
                                @NonNull String statusLabel) {

        this.exceptionLabel = checkNotNull(exceptionLabel);
        this.timeLabel = checkNotNull(timeLabel);
        this.statusLabel = checkNotNull(statusLabel);
    }

    public String getExceptionLabel() {
        return exceptionLabel;
    }

    public String getTimeLabel() {
        return timeLabel;
    }

    public String getStatusLabel() {
        return statusLabel;
    }
}
