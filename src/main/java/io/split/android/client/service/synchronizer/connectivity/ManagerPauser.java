package io.split.android.client.service.synchronizer.connectivity;

import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.lifecycle.SplitLifecycleAware;
import io.split.android.client.service.synchronizer.SyncManager;

public class ManagerPauser implements SplitLifecycleAware, NetworkAware {

    private final SyncManager mSyncManager;

    private final AtomicBoolean mLifecyclePaused = new AtomicBoolean(false);

    private final AtomicBoolean mNetworkPaused = new AtomicBoolean(false);

    public ManagerPauser(SyncManager syncManager) {
        mSyncManager = syncManager;
    }

    @Override
    public void pause() {
        mLifecyclePaused.set(true);
        updateStatus();
    }

    @Override
    public void resume() {
        mLifecyclePaused.set(false);
        updateStatus();
    }

    @Override
    public void onNetworkConnected() {
        mNetworkPaused.set(false);
        updateStatus();
    }

    @Override
    public void onNetworkDisconnected() {
        mNetworkPaused.set(true);
        updateStatus();
    }

    private synchronized void updateStatus() {
        if (mLifecyclePaused.get() || mNetworkPaused.get()) {
            mSyncManager.pause();
        } else {
            mSyncManager.resume();
        }
    }
}
