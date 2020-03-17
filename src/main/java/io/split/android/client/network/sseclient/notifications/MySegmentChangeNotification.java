package io.split.android.client.network.sseclient.notifications;

import java.util.List;

public class MySegmentChangeNotification implements IncomingNotification {
    private final NotificationType type;
    private final long changeNumber;
    private final boolean includesPayload;
    private final List<String> segmentList;

    public MySegmentChangeNotification(NotificationType type, long changeNumber, boolean includesPayload, List<String> segmentList) {
        this.type = type;
        this.changeNumber = changeNumber;
        this.includesPayload = includesPayload;
        this.segmentList = segmentList;
    }

    @Override
    public NotificationType getType() {
        return type;
    }

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
