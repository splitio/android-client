package io.split.android.client.service.sseclient.notifications;

public class SplitsChangeNotification extends IncomingNotification {
    private long changeNumber;


    public SplitsChangeNotification(long changeNumber) {
        this.changeNumber = changeNumber;
    }

    public long getChangeNumber() {
        return changeNumber;
    }
}

