package io.split.android.client.service.workmanager.splits;

import java.net.URISyntaxException;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.splits.SplitChangeProcessor;
import io.split.android.client.service.splits.SplitsSyncHelper;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorageProducer;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryStorage;
import io.split.android.client.utils.logger.Logger;

/**
 * Builds the instance of {@link SplitsSyncTask} to be executed by the {@link SplitsSyncWorker}.
 */
class SplitsSyncWorkerTaskBuilder {

    private final StorageProvider mStorageProvider;
    private final FetcherProvider mFetcherProvider;
    private final SplitChangeProcessor mSplitChangeProcessor;
    private final SyncHelperProvider mSplitsSyncHelperProvider;
    private final String mFlagsSpec;

    SplitsSyncWorkerTaskBuilder(StorageProvider storageProvider,
                                FetcherProvider fetcherProvider,
                                SplitChangeProcessor splitChangeProcessor,
                                SyncHelperProvider splitsSyncHelperProvider,
                                String flagsSpec) {
        mStorageProvider = storageProvider;
        mFetcherProvider = fetcherProvider;
        mSplitsSyncHelperProvider = splitsSyncHelperProvider;
        mSplitChangeProcessor = splitChangeProcessor;
        mFlagsSpec = flagsSpec;
    }

    SplitTask getTask() {
        try {
            SplitsStorage splitsStorage = mStorageProvider.provideSplitsStorage();
            TelemetryStorage telemetryStorage = mStorageProvider.provideTelemetryStorage();
            RuleBasedSegmentStorageProducer ruleBasedSegmentStorageProducer = mStorageProvider.provideRuleBasedSegmentStorage();
            String splitsFilterQueryString = splitsStorage.getSplitsFilterQueryString();

            SplitsSyncHelper splitsSyncHelper = mSplitsSyncHelperProvider.provideSplitsSyncHelper(
                    mFetcherProvider.provideFetcher(splitsFilterQueryString),
                    splitsStorage,
                    mSplitChangeProcessor,
                    ruleBasedSegmentStorageProducer,
                    telemetryStorage,
                    mFlagsSpec);

            return SplitsSyncTask.buildForBackground(splitsSyncHelper,
                    splitsStorage,
                    splitsFilterQueryString,
                    telemetryStorage);
        } catch (URISyntaxException e) {
            Logger.e("Error creating Split worker: " + e.getMessage());
        }
        return null;
    }
}
