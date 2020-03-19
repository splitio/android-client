package io.split.android.client.service.sseclient.notifications;

import java.util.List;

public class MySegmentChangeNotification extends IncomingNotification {
    private long changeNumber;
    private boolean includesPayload;
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
