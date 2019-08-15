package io.split.android.fake;

import java.io.IOException;
import java.util.Map;

import io.split.android.client.storage.MemoryStorage;
import io.split.android.client.track.EventsChunk;
import io.split.android.client.track.ITracksStorage;

public class TrackFileStorageStub extends MemoryStorage implements ITracksStorage {
    @Override
    public Map<String, EventsChunk> read() throws IOException {
        return null;
    }

    @Override
    public void write(Map<String, EventsChunk> tracks) throws IOException {

    }
}
