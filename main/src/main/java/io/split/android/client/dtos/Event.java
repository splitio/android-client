package io.split.android.client.dtos;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import io.split.android.client.storage.common.InBytesSizable;
import io.split.android.client.utils.deserializer.EventDeserializer;

@JsonAdapter(EventDeserializer.class)
public class Event extends SerializableEvent implements InBytesSizable, Identifiable {

    public static final String SIZE_IN_BYTES_FIELD = "sizeInBytes";

    transient public long storageId;
    @SerializedName(SIZE_IN_BYTES_FIELD)
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
