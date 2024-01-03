package io.split.android.client.service.workmanager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.work.WorkerParameters;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.split.android.client.FlagSetsFilterImpl;
import io.split.android.client.SplitFilter;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.ServiceFactory;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.splits.SplitChangeProcessor;
import io.split.android.client.service.splits.SplitsSyncHelper;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.storage.db.StorageFactory;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryStorage;
import io.split.android.client.utils.logger.Logger;

public class SplitsSyncWorker extends SplitWorker {

    @WorkerThread
    public SplitsSyncWorker(@NonNull Context context,
                            @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        try {
            boolean shouldRecordTelemetry = workerParams.getInputData().getBoolean(ServiceConstants.SHOULD_RECORD_TELEMETRY, false);
            String apiKey = workerParams.getInputData().getString(ServiceConstants.WORKER_PARAM_API_KEY);
            boolean encryptionEnabled = workerParams.getInputData().getBoolean(ServiceConstants.WORKER_PARAM_ENCRYPTION_ENABLED, false);

            SplitFilter filter = buildFilter(workerParams.getInputData().getString(ServiceConstants.WORKER_PARAM_CONFIGURED_FILTER_TYPE),
                    workerParams.getInputData().getStringArray(ServiceConstants.WORKER_PARAM_CONFIGURED_FILTER_VALUES));

            SplitsStorage splitsStorage = StorageFactory.getSplitsStorageForWorker(getDatabase(), apiKey, encryptionEnabled);
            // StorageFactory.getSplitsStorageForWorker creates a new storage instance, so it needs
            // to be populated by calling loadLocal
            splitsStorage.loadLocal();
            HttpFetcher<SplitChange> splitsFetcher = ServiceFactory.getSplitsFetcher(getHttpClient(),
                    getEndPoint(), splitsStorage.getSplitsFilterQueryString());

            TelemetryStorage telemetryStorage = StorageFactory.getTelemetryStorage(shouldRecordTelemetry);

            SplitChangeProcessor splitChangeProcessor = new SplitChangeProcessor(filter, (filter != null && filter.getType() == SplitFilter.Type.BY_SET) ?
                    new FlagSetsFilterImpl(filter.getValues()) : null);

            SplitsSyncHelper splitsSyncHelper = new SplitsSyncHelper(splitsFetcher, splitsStorage,
                    splitChangeProcessor,
                    telemetryStorage);

            mSplitTask = buildSplitSyncTask(splitsStorage, telemetryStorage, splitsSyncHelper);
        } catch (URISyntaxException e) {
            Logger.e("Error creating Split worker: " + e.getMessage());
        }
    }

    @Nullable
    private static SplitFilter buildFilter(String filterType, String[] filterValuesArray) {
        SplitFilter filter = null;
        if (filterType != null) {
            List<String> configuredFilterValues = new ArrayList<>();
            if (filterValuesArray != null) {
                configuredFilterValues = Arrays.asList(filterValuesArray);
            }

            if (SplitFilter.Type.BY_NAME.queryStringField().equals(filterType)) {
                filter = SplitFilter.byName(configuredFilterValues);
            } else if (SplitFilter.Type.BY_SET.queryStringField().equals(filterType)) {
                filter = SplitFilter.bySet(configuredFilterValues);
            }
        }
        return filter;
    }

    @NonNull
    private SplitTask buildSplitSyncTask(SplitsStorage splitsStorage, TelemetryStorage telemetryStorage, SplitsSyncHelper splitsSyncHelper) {
        return SplitsSyncTask.buildForBackground(splitsSyncHelper,
                splitsStorage,
                false,
                getCacheExpirationInSeconds(),
                splitsStorage.getSplitsFilterQueryString(),
                telemetryStorage);
    }
}
