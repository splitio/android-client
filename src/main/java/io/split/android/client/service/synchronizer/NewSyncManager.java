package io.split.android.client.service.synchronizer;

import androidx.annotation.VisibleForTesting;

// TODO: Will be renamed to SyncManager on final integration
@VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
public interface NewSyncManager {
    void start();

    void pause();

    void resume();

    void stop();

}
