package io.split.android.client.service.sseclient.notifications.mysegments;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import java.math.BigInteger;
import java.util.concurrent.BlockingQueue;

import io.split.android.client.service.mysegments.MySegmentsTaskFactory;
import io.split.android.client.service.sseclient.notifications.MySegmentChangeNotification;

public class MySegmentsNotificationProcessorConfiguration {

    private final MySegmentsTaskFactory mMySegmentsTaskFactory;
    private final BlockingQueue<MySegmentChangeNotification> mMySegmentUpdateNotificationsQueue;
    private final String mUserKey;
    private final BigInteger mHashedUserKey;

    public MySegmentsNotificationProcessorConfiguration(@NonNull MySegmentsTaskFactory mySegmentsTaskFactory,
                                                        @NonNull BlockingQueue<MySegmentChangeNotification> mySegmentUpdateNotificationsQueue,
                                                        @NonNull String userKey,
                                                        @NonNull BigInteger hashedUserKey) {
        mMySegmentsTaskFactory = checkNotNull(mySegmentsTaskFactory);
        mMySegmentUpdateNotificationsQueue = checkNotNull(mySegmentUpdateNotificationsQueue);
        mUserKey = checkNotNull(userKey);
        mHashedUserKey = checkNotNull(hashedUserKey);
    }

    public MySegmentsTaskFactory getMySegmentsTaskFactory() {
        return mMySegmentsTaskFactory;
    }

    public BlockingQueue<MySegmentChangeNotification> getMySegmentUpdateNotificationsQueue() {
        return mMySegmentUpdateNotificationsQueue;
    }

    public String getUserKey() {
        return mUserKey;
    }

    public BigInteger getHashedUserKey() {
        return mHashedUserKey;
    }
}
