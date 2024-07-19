package io.split.android.client.service.sseclient.notifications;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MySegmentChangeNotification extends IncomingNotification {
    @SerializedName("changeNumber")
    private long changeNumber;
    @SerializedName("includesPayload")
    private boolean includesPayload;
    @SerializedName("segmentList")
    private List<String> segmentList;

    public long getChangeNumber() {
        return changeNumber;
    }

    public boolean isIncludesPayload() {
        return includesPayload;
    }

    public List<String> getSegmentList() {
        return segmentList;
    }
}
