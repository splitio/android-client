package io.split.android.client.lifecycle;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import io.split.android.client.EventPropertiesProcessor;
import io.split.android.client.TrackClient;
import io.split.android.client.cache.IMySegmentsCache;
import io.split.android.client.impressions.ImpressionsManager;
import io.split.android.client.service.SyncManager;
import io.split.android.engine.segments.RefreshableMySegmentsFetcherProvider;

public class LifecycleManager implements LifecycleObserver {

    ImpressionsManager mImpressionsManager;
    TrackClient mTrackClient;
    SyncManager mSyncManager;

    public LifecycleManager(SyncManager syncManager,
            ImpressionsManager impressionsManager,
                            TrackClient trackClient) {

        mImpressionsManager = impressionsManager;
        mTrackClient = trackClient;
        mSyncManager = syncManager;
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private void onPause() {
        if(mImpressionsManager != null) {
            mImpressionsManager.pause();
            mImpressionsManager.saveToDisk();
        }

        if(mTrackClient != null) {
            mTrackClient.pause();
            mTrackClient.saveToDisk();
        }

        if(mSyncManager != null) {
            mSyncManager.pause();
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private void onResume() {
        if(mImpressionsManager != null) {
            mImpressionsManager.resume();
        }

        if(mTrackClient != null) {
            mTrackClient.resume();
        }

        if(mSyncManager != null) {
            mSyncManager.resume();
        }
    }

    public void destroy() {
        ProcessLifecycleOwner.get().getLifecycle().removeObserver(this);
    }

}
