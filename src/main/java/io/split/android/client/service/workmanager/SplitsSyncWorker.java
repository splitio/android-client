package io.split.android.client.service.workmanager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import java.net.URISyntaxException;

import io.split.android.client.service.ServiceFactory;
import io.split.android.client.service.splits.SplitChangeProcessor;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.storage.db.StorageFactory;
import io.split.android.client.utils.Logger;

public class SplitsSyncWorker extends SplitWorker {
    public SplitsSyncWorker(@NonNull Context context,
                            @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        try {
            mSplitTask = new SplitsSyncTask(
                    ServiceFactory.getSplitsFetcher(getNetworkHelper(), getHttpClient(),
                            getEndPoint(), getMetrics()),
                    StorageFactory.getSplitsStorage(getDatabase()),
                    new SplitChangeProcessor(), false, true,
                    getCacheExpirationInSeconds());
        } catch (URISyntaxException e) {
            Logger.e("Error creating Split worker: " + e.getMessage());
        }
    }
}
