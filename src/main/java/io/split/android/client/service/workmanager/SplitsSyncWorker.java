package io.split.android.client.service.workmanager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import java.net.URISyntaxException;

import io.split.android.client.dtos.SplitChange;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.ServiceFactory;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.splits.SplitChangeProcessor;
import io.split.android.client.service.splits.SplitsSyncBackgroundTask;
import io.split.android.client.service.splits.SplitsSyncHelper;
import io.split.android.client.storage.db.StorageFactory;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.utils.logger.Logger;

public class SplitsSyncWorker extends SplitWorker {
    public SplitsSyncWorker(@NonNull Context context,
                            @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        try {
            boolean shouldRecordTelemetry = workerParams.getInputData().getBoolean(ServiceConstants.SHOULD_RECORD_TELEMETRY, false);
            SplitsStorage splitsStorage = StorageFactory.getSplitsStorage(getDatabase());
            HttpFetcher<SplitChange> splitsFetcher = ServiceFactory.getSplitsFetcher(getNetworkHelper(), getHttpClient(),
                            getEndPoint(), splitsStorage.getSplitsFilterQueryString());
            SplitsSyncHelper splitsSyncHelper = new SplitsSyncHelper(splitsFetcher, splitsStorage, new SplitChangeProcessor(), StorageFactory.getTelemetryStorage(shouldRecordTelemetry));
            mSplitTask = new SplitsSyncBackgroundTask(splitsSyncHelper, splitsStorage, getCacheExpirationInSeconds());
        } catch (URISyntaxException e) {
            Logger.e("Error creating Split worker: " + e.getMessage());
        }
    }
}
