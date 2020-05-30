package io.split.android.client.service.synchronizer;

import androidx.annotation.VisibleForTesting;

import io.split.android.client.dtos.Event;
import io.split.android.client.impressions.Impression;
import io.split.android.client.lifecycle.SplitLifecycleAware;

@VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
public interface SyncManager extends SplitLifecycleAware {
    void start();

    void flush();

    void pushEvent(Event event);

    void pushImpression(Impression impression);

    void stop();

}
