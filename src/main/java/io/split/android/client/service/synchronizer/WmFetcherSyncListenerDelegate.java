package io.split.android.client.service.synchronizer;

public interface WmFetcherSyncListenerDelegate {
    void splitsUpdatedInBackground();
    void mySegmentsUpdatedInBackground();
}
