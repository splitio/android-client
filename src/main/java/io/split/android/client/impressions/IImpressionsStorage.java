package io.split.android.client.impressions;

import java.io.IOException;
import java.util.Map;

import io.split.android.client.storage.IStorage;
import io.split.android.client.track.EventsChunk;

public interface IImpressionsStorage extends IStorage {
    Map<String, StoredImpressions> read();
    void write(Map<String, StoredImpressions> impressions);
}
