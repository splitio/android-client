package io.split.android.client.track;

import java.io.IOException;
import java.util.Map;

import io.split.android.client.storage.IStorage;

public interface ITracksStorage extends IStorage {
    Map<String, EventsChunk> read() throws IOException;
    void write(Map<String, EventsChunk> tracks) throws IOException;
}
