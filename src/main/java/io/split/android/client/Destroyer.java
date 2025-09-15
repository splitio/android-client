package io.split.android.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import io.split.android.client.factory.FactoryMonitor;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.lifecycle.SplitLifecycleManager;
import io.split.android.client.network.HttpClient;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.shared.SplitClientContainer;
import io.split.android.client.storage.common.SplitStorageContainer;
import io.split.android.client.telemetry.TelemetrySynchronizer;
import io.split.android.client.utils.logger.Logger;

class Destroyer implements Runnable {

    // Lock to prevent concurrent shutdowns and ensure sequence
    private final Lock mInitLock;
    // Whe
    private final AtomicBoolean mCheckClients;
    private final SplitClientContainer mClientContainer;
    private final SplitStorageContainer mStorageContainer;
    private final long mInitStartTime;
    private final TelemetrySynchronizer mTelemetrySynchronizer;
    private final ExecutorService mImpressionsLoggingTaskExecutor;
    private final ExecutorService mImpressionsObserverExecutor;
    private final SyncManager mSyncManager;
    private final SplitLifecycleManager mLifecycleManager;
    private final FactoryMonitor mFactoryMonitor;
    private final String mApiKey;
    private final ImpressionListener mCustomerImpressionListener;
    private final HttpClient mDefaultHttpClient;
    private final SplitManager mSplitManager;
    private final SplitTaskExecutor mSplitTaskExecutor;
    private final SplitTaskExecutor mSplitSingleThreadTaskExecutor;
    private final AtomicBoolean mIsTerminated;

    Destroyer(
        Lock initLock,
        AtomicBoolean checkClients,
        SplitClientContainer clientContainer,
        SplitStorageContainer storageContainer,
        long initStartTime,
        TelemetrySynchronizer telemetrySynchronizer,
        ExecutorService impressionsLoggingTaskExecutor,
        ExecutorService impressionsObserverExecutor,
        SyncManager syncManager,
        SplitLifecycleManager lifecycleManager,
        FactoryMonitor factoryMonitor,
        String apiKey,
        ImpressionListener customerImpressionListener,
        HttpClient defaultHttpClient,
        SplitManager splitManager,
        SplitTaskExecutor splitTaskExecutor,
        SplitTaskExecutor splitSingleThreadTaskExecutor,
        AtomicBoolean isTerminated
    ) {
        mInitLock = initLock;
        mCheckClients = checkClients;
        mClientContainer = clientContainer;
        mStorageContainer = storageContainer;
        mInitStartTime = initStartTime;
        mTelemetrySynchronizer = telemetrySynchronizer;
        mImpressionsLoggingTaskExecutor = impressionsLoggingTaskExecutor;
        mImpressionsObserverExecutor = impressionsObserverExecutor;
        mSyncManager = syncManager;
        mLifecycleManager = lifecycleManager;
        mFactoryMonitor = factoryMonitor;
        mApiKey = apiKey;
        mCustomerImpressionListener = customerImpressionListener;
        mDefaultHttpClient = defaultHttpClient;
        mSplitManager = splitManager;
        mSplitTaskExecutor = splitTaskExecutor;
        mSplitSingleThreadTaskExecutor = splitSingleThreadTaskExecutor;
        mIsTerminated = isTerminated;
    }

    @Override
    public void run() {
        mInitLock.lock();
        try {
            if (mCheckClients.get() && !mClientContainer.getAll().isEmpty()) {
                Logger.d("Avoiding shutdown due to active clients");
                return;
            }
            Logger.w("Shutdown called for split");
            mStorageContainer
                .getTelemetryStorage()
                .recordSessionLength(
                    System.currentTimeMillis() - mInitStartTime
                );
            mTelemetrySynchronizer.flush();
            mTelemetrySynchronizer.destroy();
            Logger.d("Successful shutdown of telemetry");
            mImpressionsLoggingTaskExecutor.shutdown();
            mImpressionsObserverExecutor.shutdown();
            Logger.d("Successful shutdown of impressions logging executor");
            mSyncManager.stop();
            Logger.d("Flushing impressions and events");
            mLifecycleManager.destroy();
            mClientContainer.destroy();
            Logger.d("Successful shutdown of lifecycle manager");
            mFactoryMonitor.remove(mApiKey);
            Logger.d("Successful shutdown of segment fetchers");
            mCustomerImpressionListener.close();
            Logger.d("Successful shutdown of ImpressionListener");
            mDefaultHttpClient.close();
            Logger.d("Successful shutdown of httpclient");
            mSplitManager.destroy();
            Logger.d("Successful shutdown of manager");
            mSplitTaskExecutor.stop();
            mSplitSingleThreadTaskExecutor.stop();
            Logger.d("Successful shutdown of task executor");
            mStorageContainer.getAttributesStorageContainer().destroy();
            Logger.d("Successful shutdown of attributes storage");
            mIsTerminated.set(true);
            Logger.d("SplitFactory has been destroyed");
        } catch (Exception e) {
            Logger.e(e, "We could not shutdown split");
        } finally {
            mCheckClients.set(false);
            mInitLock.unlock();
        }
    }
}
