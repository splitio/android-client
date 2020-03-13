package io.split.android.client.lifecycle;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;
import io.split.android.client.service.synchronizer.SyncManager;

import static com.google.common.base.Preconditions.checkNotNull;

public class LifecycleManager implements LifecycleObserver {

    SyncManager mSyncManager;

    public LifecycleManager(@NonNull SyncManager syncManager) {
        mSyncManager = checkNotNull(syncManager);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private void onPause() {

        if(mSyncManager != null) {
            mSyncManager.pause();
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private void onResume() {
        if(mSyncManager != null) {
            mSyncManager.resume();
        }
    }

    public void destroy() {
        ProcessLifecycleOwner.get().getLifecycle().removeObserver(this);
    }

}
