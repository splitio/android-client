package io.split.android.client.service.synchronizer;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.split.android.android_client.BuildConfig;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFilter;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.impressions.ImpressionManagerConfig;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsWorkManagerWrapper;
import io.split.android.client.service.workmanager.EventsRecorderWorker;
import io.split.android.client.service.workmanager.ImpressionsRecorderWorker;
import io.split.android.client.service.workmanager.MySegmentsSyncWorker;
import io.split.android.client.service.workmanager.splits.SplitsSyncWorker;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;
import io.split.android.client.service.workmanager.UniqueKeysRecorderWorker;

public class WorkManagerWrapper implements MySegmentsWorkManagerWrapper {
    final private WorkManager mWorkManager;
    final private String mDatabaseName;
    final private String mApiKey;
    final private SplitClientConfig mSplitClientConfig;
    final private Constraints mConstraints;
    private WeakReference<SplitTaskExecutionListener> mFetcherExecutionListener;
    // This variable is used to avoid loading data first time
    // we receive enqueued event
    private final Set<String> mShouldLoadFromLocal;
    @Nullable
    private final SplitFilter mFilter;

    public WorkManagerWrapper(@NonNull WorkManager workManager,
                              @NonNull SplitClientConfig splitClientConfig,
                              @NonNull String apiKey,
                              @NonNull String databaseName,
                              @Nullable SplitFilter filter) {
        mWorkManager = checkNotNull(workManager);
        mDatabaseName = checkNotNull(databaseName);
        mSplitClientConfig = checkNotNull(splitClientConfig);
        mApiKey = checkNotNull(apiKey);
        mShouldLoadFromLocal = new HashSet<>();
        mConstraints = buildConstraints();
        mFilter = filter;
    }

    public void setFetcherExecutionListener(SplitTaskExecutionListener fetcherExecutionListener) {
        mFetcherExecutionListener = new WeakReference<>(fetcherExecutionListener);
    }

    @Override
    public void removeWork() {
        mWorkManager.cancelUniqueWork(SplitTaskType.SPLITS_SYNC.toString());
        mWorkManager.cancelUniqueWork(SplitTaskType.MY_SEGMENTS_SYNC.toString());
        mWorkManager.cancelUniqueWork(SplitTaskType.EVENTS_RECORDER.toString());
        mWorkManager.cancelUniqueWork(SplitTaskType.IMPRESSIONS_RECORDER.toString());
        mWorkManager.cancelUniqueWork(SplitTaskType.UNIQUE_KEYS_RECORDER_TASK.toString());
        if (mFetcherExecutionListener != null) {
            mFetcherExecutionListener.clear();
        }
    }

    public void scheduleWork() {
        scheduleWork(SplitTaskType.SPLITS_SYNC.toString(), SplitsSyncWorker.class,
                buildSplitSyncInputData());

        scheduleWork(SplitTaskType.EVENTS_RECORDER.toString(), EventsRecorderWorker.class,
                buildEventsRecorderInputData());

        scheduleWork(SplitTaskType.IMPRESSIONS_RECORDER.toString(),
                ImpressionsRecorderWorker.class, buildImpressionsRecorderInputData());

        if (isNoneImpressionsMode()) {
            scheduleWork(SplitTaskType.UNIQUE_KEYS_RECORDER_TASK.toString(),
                    UniqueKeysRecorderWorker.class, buildUniqueKeysRecorderInputData());
        }
    }

    @Override
    public void scheduleMySegmentsWork(Set<String> keys) {
        scheduleWork(SplitTaskType.MY_SEGMENTS_SYNC.toString(), MySegmentsSyncWorker.class,
                buildMySegmentsSyncInputData(keys));
    }

    private void scheduleWork(String requestType,
                              Class<? extends ListenableWorker> workerClass,
                              Data inputData) {
        PeriodicWorkRequest request = new PeriodicWorkRequest
                .Builder(workerClass, mSplitClientConfig.backgroundSyncPeriod(), TimeUnit.MINUTES)
                .setInputData(buildInputData(inputData))
                .setConstraints(mConstraints)
                .setInitialDelay(ServiceConstants.DEFAULT_INITIAL_DELAY, TimeUnit.MINUTES)
                .build();
        mWorkManager.enqueueUniquePeriodicWork(requestType, ExistingPeriodicWorkPolicy.REPLACE, request);
        observeWorkState(workerClass.getCanonicalName());
    }

    private void observeWorkState(String tag) {
        Logger.d("Adding work manager observer for request id " + tag);
        ThreadUtils.runInMainThread(new Runnable() {
            @Override
            public void run() {
                mWorkManager.getWorkInfosByTagLiveData(tag)
                        .observe(ProcessLifecycleOwner.get(), new Observer<List<WorkInfo>>() {
                            @Override
                            public void onChanged(@Nullable List<WorkInfo> workInfoList) {
                                if (workInfoList == null) {
                                    return;
                                }

                                for (WorkInfo workInfo : workInfoList) {
                                    Logger.d("Work manager task: " + workInfo.getTags() +
                                            ", state: " + workInfo.getState());
                                    updateTaskStatus(workInfo);
                                }
                            }
                        });
            }
        });
    }

    private Data buildInputData(Data customData) {
        Data.Builder dataBuilder = new Data.Builder();
        dataBuilder.putString(ServiceConstants.WORKER_PARAM_DATABASE_NAME, mDatabaseName);
        dataBuilder.putString(ServiceConstants.WORKER_PARAM_API_KEY, mApiKey);
        dataBuilder.putBoolean(ServiceConstants.WORKER_PARAM_ENCRYPTION_ENABLED, mSplitClientConfig.encryptionEnabled());
        try {
            String pinsJson = Json.toJson(mSplitClientConfig.certificatePinningConfiguration().getPins());
            dataBuilder.putString(ServiceConstants.WORKER_PARAM_CERTIFICATE_PINS, pinsJson);
        } catch (Exception e) {
            Logger.e("Error converting pins to JSON for BG sync", e.getLocalizedMessage());
        }

        if (customData != null) {
            dataBuilder.putAll(customData);
        }
        return dataBuilder.build();
    }

    private void updateTaskStatus(WorkInfo workInfo) {

        if (mFetcherExecutionListener == null || workInfo == null ||
                workInfo.getTags() == null ||
                !WorkInfo.State.ENQUEUED.equals(workInfo.getState())) {
            return;
        }
        SplitTaskType taskType = taskTypeFromTags(workInfo.getTags());
        if (taskType == null) {
            return;
        }
        boolean shouldLoadLocal = mShouldLoadFromLocal.contains(taskType.toString());
        if (!shouldLoadLocal) {
            Logger.d("Avoiding update for " + taskType);
            mShouldLoadFromLocal.add(taskType.toString());
            return;
        }

        SplitTaskExecutionListener listener = mFetcherExecutionListener.get();
        if (listener != null) {
            Logger.d("Updating for " + taskType);
            listener.taskExecuted(SplitTaskExecutionInfo.success(taskType));
        }
    }

    private SplitTaskType taskTypeFromTags(Set<String> tags) {
        if (tags.contains(SplitsSyncWorker.class.getCanonicalName())) {
            return SplitTaskType.SPLITS_SYNC;
        } else if (tags.contains(MySegmentsSyncWorker.class.getCanonicalName())) {
            return SplitTaskType.MY_SEGMENTS_SYNC;
        }
        return null;
    }

    private Data buildSplitSyncInputData() {
        Data.Builder dataBuilder = new Data.Builder();
        dataBuilder.putLong(ServiceConstants.WORKER_PARAM_SPLIT_CACHE_EXPIRATION, mSplitClientConfig.cacheExpirationInSeconds());
        dataBuilder.putString(ServiceConstants.WORKER_PARAM_ENDPOINT, mSplitClientConfig.endpoint());
        dataBuilder.putBoolean(ServiceConstants.SHOULD_RECORD_TELEMETRY, mSplitClientConfig.shouldRecordTelemetry());
        dataBuilder.putString(ServiceConstants.WORKER_PARAM_CONFIGURED_FILTER_TYPE, (mFilter != null) ? mFilter.getType().queryStringField() : null);
        dataBuilder.putStringArray(ServiceConstants.WORKER_PARAM_CONFIGURED_FILTER_VALUES, (mFilter != null) ? mFilter.getValues().toArray(new String[0]) : new String[0]);
        dataBuilder.putString(ServiceConstants.WORKER_PARAM_FLAGS_SPEC, BuildConfig.FLAGS_SPEC);
        return buildInputData(dataBuilder.build());
    }

    private Data buildMySegmentsSyncInputData(Set<String> keys) {
        Data.Builder dataBuilder = new Data.Builder();
        String[] keysArray = new String[keys.size()];
        keys.toArray(keysArray);
        dataBuilder.putString(ServiceConstants.WORKER_PARAM_ENDPOINT, mSplitClientConfig.endpoint());
        dataBuilder.putStringArray(ServiceConstants.WORKER_PARAM_KEY, keysArray);
        dataBuilder.putBoolean(ServiceConstants.SHOULD_RECORD_TELEMETRY, mSplitClientConfig.shouldRecordTelemetry());
        return buildInputData(dataBuilder.build());
    }

    private Data buildEventsRecorderInputData() {
        Data.Builder dataBuilder = new Data.Builder();
        dataBuilder.putString(
                ServiceConstants.WORKER_PARAM_ENDPOINT, mSplitClientConfig.eventsEndpoint());
        dataBuilder.putInt(
                ServiceConstants.WORKER_PARAM_EVENTS_PER_PUSH, mSplitClientConfig.eventsPerPush());
        dataBuilder.putBoolean(
                ServiceConstants.SHOULD_RECORD_TELEMETRY, mSplitClientConfig.shouldRecordTelemetry());
        return buildInputData(dataBuilder.build());
    }

    private Data buildImpressionsRecorderInputData() {
        Data.Builder dataBuilder = new Data.Builder();
        dataBuilder.putString(
                ServiceConstants.WORKER_PARAM_ENDPOINT, mSplitClientConfig.eventsEndpoint());
        dataBuilder.putInt(
                ServiceConstants.WORKER_PARAM_IMPRESSIONS_PER_PUSH,
                mSplitClientConfig.impressionsPerPush());
        dataBuilder.putBoolean(ServiceConstants.SHOULD_RECORD_TELEMETRY,
                mSplitClientConfig.shouldRecordTelemetry());

        return buildInputData(dataBuilder.build());
    }

    private Data buildUniqueKeysRecorderInputData() {
        Data.Builder dataBuilder = new Data.Builder();
        dataBuilder.putString(
                ServiceConstants.WORKER_PARAM_ENDPOINT, mSplitClientConfig.telemetryEndpoint());
        dataBuilder.putInt(
                ServiceConstants.WORKER_PARAM_UNIQUE_KEYS_PER_PUSH, mSplitClientConfig.mtkPerPush());
        dataBuilder.putLong(
                ServiceConstants.WORKER_PARAM_UNIQUE_KEYS_ESTIMATED_SIZE_IN_BYTES, ServiceConstants.ESTIMATED_IMPRESSION_SIZE_IN_BYTES);

        return buildInputData(dataBuilder.build());
    }

    private Constraints buildConstraints() {
        Constraints.Builder constraintsBuilder = new Constraints.Builder();
        constraintsBuilder.setRequiredNetworkType(
                mSplitClientConfig.backgroundSyncWhenBatteryWifiOnly() ?
                        NetworkType.UNMETERED : NetworkType.CONNECTED);
        constraintsBuilder.setRequiresBatteryNotLow(
                mSplitClientConfig.backgroundSyncWhenBatteryNotLow());
        return constraintsBuilder.build();
    }

    private boolean isNoneImpressionsMode() {
        return ImpressionManagerConfig.Mode.fromImpressionMode(mSplitClientConfig.impressionsMode()).isNone();
    }
}
