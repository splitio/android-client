package io.split.android.client.tracker;

/** Callback invoked when an exception occurs during tracking. May be null to skip telemetry. */
public interface TrackExceptionListener {
    void accept();
}
