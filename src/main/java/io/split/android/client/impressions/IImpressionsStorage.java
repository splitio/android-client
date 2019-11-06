package io.split.android.client.impressions;

import java.util.Map;

import io.split.android.client.storage.legacy.IStorage;

public interface IImpressionsStorage extends IStorage {
    Map<String, StoredImpressions> read();
    void write(Map<String, StoredImpressions> impressions);
}
