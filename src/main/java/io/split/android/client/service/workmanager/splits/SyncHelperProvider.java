package io.split.android.client.service.workmanager.splits;

import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import io.split.android.client.dtos.TargetingRulesChange;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.rules.RuleBasedSegmentChangeProcessor;
import io.split.android.client.service.splits.SplitChangeProcessor;
import io.split.android.client.service.splits.SplitsSyncHelper;
import io.split.android.client.storage.general.GeneralInfoStorage;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorageProducer;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryStorage;

class SyncHelperProvider {

    SplitsSyncHelper provideSplitsSyncHelper(HttpFetcher<TargetingRulesChange> splitsFetcher,
                                             SplitsStorage splitsStorage,
                                             SplitChangeProcessor splitChangeProcessor,
                                             RuleBasedSegmentChangeProcessor ruleBasedSegmentChangeProcessor,
                                             RuleBasedSegmentStorageProducer ruleBasedSegmentStorage,
                                             GeneralInfoStorage generalInfoStorage,
                                             TelemetryStorage telemetryStorage,
                                             String mFlagsSpec,
                                             @Nullable AtomicReference<TargetingRulesChange> cacheRef,
                                             @Nullable ReentrantLock cachedFetchLock) {
        return new SplitsSyncHelper(splitsFetcher,
                splitsStorage,
                splitChangeProcessor,
                ruleBasedSegmentChangeProcessor,
                ruleBasedSegmentStorage,
                generalInfoStorage,
                telemetryStorage,
                mFlagsSpec,
                true,
                cacheRef,
                cachedFetchLock);
    }
}
