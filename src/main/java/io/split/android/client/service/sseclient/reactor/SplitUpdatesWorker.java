package io.split.android.client.service.sseclient.reactor;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.concurrent.BlockingQueue;

import io.split.android.client.common.CompressionUtilProvider;
import io.split.android.client.dtos.Split;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.sseclient.notifications.SplitsChangeNotification;
import io.split.android.client.service.synchronizer.Synchronizer;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.utils.Base64Util;
import io.split.android.client.utils.CompressionUtil;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;

public class SplitUpdatesWorker extends UpdateWorker {

    /***
     * This class will be in charge of update splits when a new notification arrived.
     */

    private final BlockingQueue<SplitsChangeNotification> mNotificationsQueue;
    private final Synchronizer mSynchronizer;
    private final SplitsStorage mSplitsStorage;
    private final CompressionUtilProvider mCompressionUtilProvider;
    private final SplitTaskExecutor mSplitTaskExecutor;
    private final SplitTaskFactory mSplitTaskFactory;
    private final Base64Decoder mBase64Decoder;

    public SplitUpdatesWorker(@NonNull Synchronizer synchronizer,
                              @NonNull BlockingQueue<SplitsChangeNotification> notificationsQueue,
                              @NonNull SplitsStorage splitsStorage,
                              @NonNull CompressionUtilProvider compressionUtilProvider,
                              @NonNull SplitTaskExecutor splitTaskExecutor,
                              @NonNull SplitTaskFactory splitTaskFactory) {
        this(synchronizer,
                notificationsQueue,
                splitsStorage,
                compressionUtilProvider,
                splitTaskExecutor,
                splitTaskFactory,
                new Base64DecoderImpl());
    }

    @VisibleForTesting
    public SplitUpdatesWorker(@NonNull Synchronizer synchronizer,
                              @NonNull BlockingQueue<SplitsChangeNotification> notificationsQueue,
                              @NonNull SplitsStorage splitsStorage,
                              @NonNull CompressionUtilProvider compressionUtilProvider,
                              @NonNull SplitTaskExecutor splitTaskExecutor,
                              @NonNull SplitTaskFactory splitTaskFactory,
                              @NonNull Base64Decoder base64Decoder) {
        super();
        mSynchronizer = checkNotNull(synchronizer);
        mNotificationsQueue = checkNotNull(notificationsQueue);
        mSplitsStorage = checkNotNull(splitsStorage);
        mCompressionUtilProvider = checkNotNull(compressionUtilProvider);
        mSplitTaskExecutor = checkNotNull(splitTaskExecutor);
        mSplitTaskFactory = checkNotNull(splitTaskFactory);
        mBase64Decoder = checkNotNull(base64Decoder);
    }

    @Override
    protected void onWaitForNotificationLoop() throws InterruptedException {
        try {
            SplitsChangeNotification notification = mNotificationsQueue.take();
            Logger.d("A new notification to update splits has been received");

            long storageChangeNumber = mSplitsStorage.getTill();
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
            Logger.d("Splits update worker has been interrupted");
            throw (e);
        }
    }

    private boolean isInvalidChangeNumber(SplitsChangeNotification notification, long storageChangeNumber) {
        return notification.getPreviousChangeNumber() == null ||
                notification.getPreviousChangeNumber() == 0 ||
                storageChangeNumber != notification.getPreviousChangeNumber();
    }

    private static boolean isLegacyNotification(SplitsChangeNotification notification) {
        return notification.getData() == null ||
                notification.getCompressionType() == null;
    }

    private void handleLegacyNotification(SplitsChangeNotification notification) {
        mSynchronizer.synchronizeSplits(notification.getChangeNumber());
        Logger.d("Enqueuing polling task");
    }

    private void handleNotification(SplitsChangeNotification notification) {
        String decompressed = decompressData(notification.getData(), mCompressionUtilProvider.get(notification.getCompressionType()));

        if (decompressed == null) {
            Logger.e("Could not decompress payload");
        }

        try {
            Split split = Json.fromJson(decompressed, Split.class);

            mSplitTaskExecutor.submit(
                    mSplitTaskFactory.createSplitsUpdateTask(split, notification.getChangeNumber()),
                    null);
            Logger.d("Updating split: " + split.name);
        } catch (Exception e) {
            Logger.e("Could not parse split");
        }
    }

    @Nullable
    private String decompressData(String data, CompressionUtil compressionUtil) {
        if (compressionUtil == null) {
            Logger.e("Compression type not supported");
            return null;
        }

        byte[] decoded = mBase64Decoder.decode(data);
        if (decoded == null) {
            Logger.e("Could not decode payload");
        }

        byte[] decompressed = compressionUtil.decompress(decoded);
        if (decompressed == null) {
            Logger.e("Could not decompress payload");
        }

        return new String(decompressed);
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
