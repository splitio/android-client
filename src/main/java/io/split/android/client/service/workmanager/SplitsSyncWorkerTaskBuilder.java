package io.split.android.client.service.workmanager;

import androidx.annotation.NonNull;

import java.net.URISyntaxException;

import io.split.android.client.dtos.SplitChange;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.splits.SplitChangeProcessor;
import io.split.android.client.service.splits.SplitsSyncHelper;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryStorage;
import io.split.android.client.utils.logger.Logger;

class SplitsSyncWorkerTaskBuilder {

    private final long mCacheExpirationInSeconds;
    private final StorageProvider mStorageProvider;
    private final FetcherProvider mFetcherProvider;
    private final SplitChangeProcessor mSplitChangeProcessor;
    private final String mFlagsSpec;

    public SplitsSyncWorkerTaskBuilder(StorageProvider storageProvider,
                                       FetcherProvider fetcherProvider,
                                       SplitChangeProcessor splitChangeProcessor,
                                       long cacheExpirationInSeconds,
                                       String flagsSpec) {
        mStorageProvider = storageProvider;
        mFetcherProvider = fetcherProvider;
        mSplitChangeProcessor = splitChangeProcessor;
        mCacheExpirationInSeconds = cacheExpirationInSeconds;
        mFlagsSpec = flagsSpec;
    }

    SplitTask getTask() {
        try {
            SplitsStorage splitsStorage = mStorageProvider.provideSplitsStorage();
            // StorageFactory.getSplitsStorageForWorker creates a new storage instance, so it needs
            // to be populated by calling loadLocal
            splitsStorage.loadLocal();
            HttpFetcher<SplitChange> splitsFetcher = mFetcherProvider.provideFetcher(splitsStorage.getSplitsFilterQueryString());

            TelemetryStorage telemetryStorage = mStorageProvider.provideTelemetryStorage();

            SplitsSyncHelper splitsSyncHelper = new SplitsSyncHelper(splitsFetcher, splitsStorage,
                    mSplitChangeProcessor,
                    telemetryStorage,
                    mFlagsSpec);

            return buildSplitSyncTask(splitsStorage, telemetryStorage, splitsSyncHelper);
        } catch (URISyntaxException e) {
            Logger.e("Error creating Split worker: " + e.getMessage());
        }
        return null;
    }

    @NonNull
    private SplitTask buildSplitSyncTask(SplitsStorage splitsStorage, TelemetryStorage telemetryStorage, SplitsSyncHelper splitsSyncHelper) {
        return SplitsSyncTask.buildForBackground(splitsSyncHelper,
                splitsStorage,
                false,
                mCacheExpirationInSeconds,
                splitsStorage.getSplitsFilterQueryString(),
                telemetryStorage);
    }

    public interface StorageProvider {

        SplitsStorage provideSplitsStorage();
        TelemetryStorage provideTelemetryStorage();
    }

    public interface FetcherProvider {

        HttpFetcher<SplitChange> provideFetcher(String splitsFilterQueryString) throws URISyntaxException;
    }
}
