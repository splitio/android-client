package io.split.android.client.tracker;

/** Callback invoked with the validated event when tracking succeeds. */
public interface EventPushListener {
    void accept(TrackerEvent event);
}
