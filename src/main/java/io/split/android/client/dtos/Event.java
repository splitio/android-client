package io.split.android.client.dtos;

import io.split.android.client.storage.InBytesSizable;

public class Event extends SerializableEvent implements InBytesSizable, Identifiable {

    transient public long storageId;
    private int sizeInBytes = 0;

    public void setSizeInBytes(int sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    @Override
    public long getId() {
        return storageId;
    }
}
