package io.split.android.client.impressions;

public interface ImpressionsManager {
    void pause();

    void resume();

    void flushImpressions();

    void saveToDisk();
}
