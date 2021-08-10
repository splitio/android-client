package io.split.android.client.service.sseclient.notifications;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class KeyList {
    /* package private */ static final String FIELD_ADDED = "a";
    /* package private */ static final String FIELD_REMOVED = "r";

    @SerializedName(FIELD_ADDED)
    List<String> added;

    @SerializedName(FIELD_REMOVED)
    List<String> removed;

    public List<String> getAdded() {
        return added;
    }

    public List<String> getRemoved() {
        return removed;
    }
}
