package io.split.android.client.service.sseclient.notifications.mysegments;

import androidx.annotation.NonNull;

import java.math.BigInteger;
import java.util.concurrent.BlockingQueue;

import io.split.android.client.service.mysegments.MySegmentsTaskFactory;
import io.split.android.client.service.sseclient.notifications.MySegmentChangeNotification;

public class MySegmentsNotificationProcessorConfiguration {

    private final MySegmentsTaskFactory mMySegmentsTaskFactory;
    private final BlockingQueue<MySegmentChangeNotification> mMySegmentUpdateNotificationsQueue;
    private final BigInteger mHashedUserKey;

    public MySegmentsNotificationProcessorConfiguration(@NonNull MySegmentsTaskFactory mySegmentsTaskFactory,
                                                        @NonNull BlockingQueue<MySegmentChangeNotification> mySegmentUpdateNotificationsQueue,
                                                        @NonNull BigInteger hashedUserKey) {
        mMySegmentsTaskFactory = mySegmentsTaskFactory;
        mMySegmentUpdateNotificationsQueue = mySegmentUpdateNotificationsQueue;
        mHashedUserKey = hashedUserKey;
    }

    public MySegmentsTaskFactory getMySegmentsTaskFactory() {
        return mMySegmentsTaskFactory;
    }

    public BlockingQueue<MySegmentChangeNotification> getMySegmentUpdateNotificationsQueue() {
        return mMySegmentUpdateNotificationsQueue;
    }

    public BigInteger getHashedUserKey() {
        return mHashedUserKey;
    }
}
