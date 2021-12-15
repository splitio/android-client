package io.split.android.client.telemetry.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import io.split.android.client.telemetry.model.streaming.StreamingEvent;

public class Stats {

    @SerializedName("lS")
    private LastSync lastSynchronizations;

    @SerializedName("mL")
    private MethodLatencies methodLatencies;

    @SerializedName("mE")
    private MethodExceptions methodExceptions;

    @SerializedName("hE")
    private HttpErrors httpErrors;

    @SerializedName("hL")
    private HttpLatencies httpLatencies;

    @SerializedName("tR")
    private long tokenRefreshes;

    @SerializedName("aR")
    private long authRejections;

    @SerializedName("iQ")
    private long impressionsQueued;

    @SerializedName("iDe")
    private long impressionsDeduped;

    @SerializedName("iDr")
    private long impressionsDropped;

    @SerializedName("spC")
    private long splitCount;

    @SerializedName("seC")
    private long segmentCount;

    @SerializedName("skC")
    private long segmentKeyCount;

    @SerializedName("sL")
    private long sessionLengthMs;

    @SerializedName("eQ")
    private long eventsQueued;

    @SerializedName("eD")
    private long eventsDropped;

    @SerializedName("sE")
    private List<StreamingEvent> streamingEvents;

    @SerializedName("t")
    private List<String> tags;
}
