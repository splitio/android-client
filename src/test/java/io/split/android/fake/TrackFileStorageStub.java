package io.split.android.fake;

import java.util.Map;

import io.split.android.client.storage.legacy.MemoryStorage;
import io.split.android.client.track.EventsChunk;
import io.split.android.client.track.ITrackStorage;

public class TrackFileStorageStub extends MemoryStorage implements ITrackStorage {
    @Override
    public Map<String, EventsChunk> read() {
        return null;
    }

    @Override
    public void write(Map<String, EventsChunk> tracks) {

    }
}
