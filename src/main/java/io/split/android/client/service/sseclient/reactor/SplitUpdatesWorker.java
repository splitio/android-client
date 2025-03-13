package io.split.android.client.service.sseclient.reactor;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.concurrent.BlockingQueue;

import io.split.android.client.common.CompressionUtilProvider;
import io.split.android.client.dtos.RuleBasedSegment;
import io.split.android.client.dtos.Split;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.sseclient.notifications.InstantUpdateChangeNotification;
import io.split.android.client.service.sseclient.notifications.NotificationType;
import io.split.android.client.service.synchronizer.Synchronizer;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.utils.Base64Util;
import io.split.android.client.utils.CompressionUtil;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;

public class SplitUpdatesWorker extends UpdateWorker {

    /***
     * This class will be in charge of update splits when a new notification arrived.
     */
    private final BlockingQueue<InstantUpdateChangeNotification> mNotificationsQueue;
    private final Synchronizer mSynchronizer;
    private final SplitsStorage mSplitsStorage;
    private final RuleBasedSegmentStorage mRuleBasedSegmentStorage;
    private final CompressionUtilProvider mCompressionUtilProvider;
    private final SplitTaskExecutor mSplitTaskExecutor;
    private final SplitTaskFactory mSplitTaskFactory;
    private final Base64Decoder mBase64Decoder;

    public SplitUpdatesWorker(@NonNull Synchronizer synchronizer,
                              @NonNull BlockingQueue<InstantUpdateChangeNotification> notificationsQueue,
                              @NonNull SplitsStorage splitsStorage,
                              @NonNull RuleBasedSegmentStorage ruleBasedSegmentStorage,
                              @NonNull CompressionUtilProvider compressionUtilProvider,
                              @NonNull SplitTaskExecutor splitTaskExecutor,
                              @NonNull SplitTaskFactory splitTaskFactory) {
        this(synchronizer,
                notificationsQueue,
                splitsStorage,
                ruleBasedSegmentStorage,
                compressionUtilProvider,
                splitTaskExecutor,
                splitTaskFactory,
                new Base64DecoderImpl());
    }

    @VisibleForTesting
    public SplitUpdatesWorker(@NonNull Synchronizer synchronizer,
                              @NonNull BlockingQueue<InstantUpdateChangeNotification> notificationsQueue,
                              @NonNull SplitsStorage splitsStorage,
                              @NonNull RuleBasedSegmentStorage ruleBasedSegmentStorage,
                              @NonNull CompressionUtilProvider compressionUtilProvider,
                              @NonNull SplitTaskExecutor splitTaskExecutor,
                              @NonNull SplitTaskFactory splitTaskFactory,
                              @NonNull Base64Decoder base64Decoder) {
        super();
        mSynchronizer = checkNotNull(synchronizer);
        mNotificationsQueue = checkNotNull(notificationsQueue);
        mSplitsStorage = checkNotNull(splitsStorage);
        mRuleBasedSegmentStorage = checkNotNull(ruleBasedSegmentStorage);
        mCompressionUtilProvider = checkNotNull(compressionUtilProvider);
        mSplitTaskExecutor = checkNotNull(splitTaskExecutor);
        mSplitTaskFactory = checkNotNull(splitTaskFactory);
        mBase64Decoder = checkNotNull(base64Decoder);
    }

    @Override
    protected void onWaitForNotificationLoop() throws InterruptedException {
        try {
            InstantUpdateChangeNotification notification = mNotificationsQueue.take();
            Logger.d("A new notification to update feature flags has been received");

            long storageChangeNumber = getStorageChangeNumber(notification.getType());
            if (notification.getChangeNumber() <= storageChangeNumber) {
                Logger.d("Notification change number is lower than the current one. Ignoring notification");
                return;
            }

            if (isLegacyNotification(notification) || isInvalidChangeNumber(notification, storageChangeNumber)) {
                handleLegacyNotification(notification);
            } else {
                handleNotification(notification);
            }
        } catch (InterruptedException e) {
            Logger.d("Feature flags update worker has been interrupted");
            throw (e);
        }
    }

    private static boolean isInvalidChangeNumber(InstantUpdateChangeNotification notification, long storageChangeNumber) {
        return notification.getPreviousChangeNumber() == null ||
                notification.getPreviousChangeNumber() == 0 ||
                storageChangeNumber != notification.getPreviousChangeNumber();
    }

    private static boolean isLegacyNotification(InstantUpdateChangeNotification notification) {
        return notification.getData() == null ||
                notification.getCompressionType() == null;
    }

    private long getStorageChangeNumber(NotificationType type) {
        return (type == NotificationType.RULE_BASED_SEGMENT_UPDATE) ?
                mRuleBasedSegmentStorage.getChangeNumber() :
                mSplitsStorage.getTill();
    }

    private void handleNotification(InstantUpdateChangeNotification notification) {
        String decompressed = decompressData(notification.getData(),
                mCompressionUtilProvider.get(notification.getCompressionType()));

        if (decompressed == null) {
            handleLegacyNotification(notification);
            return;
        }

        try {
            inPlaceUpdate(notification, decompressed);
        } catch (Exception e) {
            Logger.e("Could not parse feature flag");
            handleLegacyNotification(notification);
        }
    }

    private void inPlaceUpdate(InstantUpdateChangeNotification notification, String decompressed) {
        SplitTask updateTask = (notification.getType() == NotificationType.RULE_BASED_SEGMENT_UPDATE) ?
                mSplitTaskFactory.createRuleBasedSegmentUpdateTask(Json.fromJson(decompressed, RuleBasedSegment.class), notification.getChangeNumber()) :
                mSplitTaskFactory.createSplitsUpdateTask(Json.fromJson(decompressed, Split.class), notification.getChangeNumber());
        SplitTaskExecutionListener executionListener = new SplitTaskExecutionListener() {
            @Override
            public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
                if (taskInfo.getStatus() == SplitTaskExecutionStatus.ERROR) {
                    handleLegacyNotification(notification);
                }
            }
        };

        mSplitTaskExecutor.submit(updateTask, executionListener);
    }

    private void handleLegacyNotification(InstantUpdateChangeNotification notification) {
        if (notification.getType() == NotificationType.RULE_BASED_SEGMENT_UPDATE) {
            mSynchronizer.synchronizeRuleBasedSegments(notification.getChangeNumber());
        } else {
            mSynchronizer.synchronizeSplits(notification.getChangeNumber());
        }
        Logger.d("Enqueuing polling task");
    }

    @Nullable
    private String decompressData(String data, CompressionUtil compressionUtil) {
        try {
            if (compressionUtil == null) {
                Logger.e("Compression type not supported");
                return null;
            }

            byte[] decoded = mBase64Decoder.decode(data);
            if (decoded == null) {
                Logger.e("Could not decode payload");
                return null;
            }

            byte[] decompressed = compressionUtil.decompress(decoded);
            if (decompressed == null) {
                Logger.e("Decompressed payload is null");
                return null;
            }

            return new String(decompressed);
        } catch (Exception e) {
            Logger.e("Could not decompress payload");
            return null;
        }
    }

    public interface Base64Decoder {
        byte[] decode(String data);
    }

    private static class Base64DecoderImpl implements Base64Decoder {
        @Override
        public byte[] decode(String data) {
            return Base64Util.bytesDecode(data);
        }
    }
}
