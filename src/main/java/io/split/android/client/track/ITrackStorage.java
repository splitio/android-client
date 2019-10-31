package io.split.android.client.track;

import java.util.Map;

import io.split.android.client.storage.legacy.IStorage;

public interface ITrackStorage extends IStorage {
    Map<String, EventsChunk> read();
    void write(Map<String, EventsChunk> tracks);
}
