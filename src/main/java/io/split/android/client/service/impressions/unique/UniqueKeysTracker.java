package io.split.android.client.service.impressions.unique;

public interface UniqueKeysTracker {

    boolean track(String key, String featureName);

    void start();

    void stop();
}
