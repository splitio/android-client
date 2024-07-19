package io.split.android.client.service.sseclient.notifications.mysegments;

import androidx.annotation.NonNull;

import java.math.BigInteger;
import java.util.concurrent.BlockingQueue;

import io.split.android.client.service.mysegments.MySegmentsTaskFactory;

public class MySegmentsNotificationProcessorConfiguration {

    private final MySegmentsTaskFactory mMySegmentsTaskFactory;
    private final BlockingQueue<MySegmentsDeferredSyncConfig> mMySegmentUpdateNotificationsQueue;
    private final BigInteger mHashedUserKey;

    public MySegmentsNotificationProcessorConfiguration(@NonNull MySegmentsTaskFactory mySegmentsTaskFactory,
                                                        @NonNull BlockingQueue<MySegmentsDeferredSyncConfig> mySegmentUpdateNotificationsQueue,
                                                        @NonNull BigInteger hashedUserKey) {
        mMySegmentsTaskFactory = mySegmentsTaskFactory;
        mMySegmentUpdateNotificationsQueue = mySegmentUpdateNotificationsQueue;
        mHashedUserKey = hashedUserKey;
    }

    public MySegmentsTaskFactory getMySegmentsTaskFactory() {
        return mMySegmentsTaskFactory;
    }

    public BlockingQueue<MySegmentsDeferredSyncConfig> getMySegmentUpdateNotificationsQueue() {
        return mMySegmentUpdateNotificationsQueue;
    }

    public BigInteger getHashedUserKey() {
        return mHashedUserKey;
    }
}
