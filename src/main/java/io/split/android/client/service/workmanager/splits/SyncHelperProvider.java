package io.split.android.client.service.workmanager.splits;

import io.split.android.client.dtos.TargetingRulesChange;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.splits.SplitChangeProcessor;
import io.split.android.client.service.splits.SplitsSyncHelper;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryStorage;

class SyncHelperProvider {

    SplitsSyncHelper provideSplitsSyncHelper(HttpFetcher<TargetingRulesChange> splitsFetcher, SplitsStorage splitsStorage,
                                             SplitChangeProcessor mSplitChangeProcessor,
                                             TelemetryStorage telemetryStorage,
                                             String mFlagsSpec) {
        return new SplitsSyncHelper(splitsFetcher, splitsStorage,
                mSplitChangeProcessor,
                telemetryStorage,
                mFlagsSpec);
    }
}
