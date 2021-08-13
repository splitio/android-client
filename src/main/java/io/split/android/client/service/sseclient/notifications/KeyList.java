package io.split.android.client.service.sseclient.notifications;

import com.google.gson.annotations.SerializedName;

import java.math.BigInteger;
import java.util.List;

public class KeyList {
    public static enum Action {
        NONE, ADD, REMOVE
    }
    /* package private */ static final String FIELD_ADDED = "a";
    /* package private */ static final String FIELD_REMOVED = "r";

    @SerializedName(FIELD_ADDED)
    private List<BigInteger> added;

    @SerializedName(FIELD_REMOVED)
    private List<BigInteger> removed;

    public List<BigInteger> getAdded() {
        return added;
    }

    public List<BigInteger> getRemoved() {
        return removed;
    }
}
