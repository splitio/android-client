package io.split.android.client.service.synchronizer;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
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

import io.split.android.client.SplitClientConfig;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.workmanager.EventsRecorderWorker;
import io.split.android.client.service.workmanager.ImpressionsRecorderWorker;
import io.split.android.client.service.workmanager.MySegmentsSyncWorker;
import io.split.android.client.service.workmanager.SplitsSyncWorker;
import io.split.android.client.utils.Logger;

@VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
public class WorkManagerWrapper {
    final private WorkManager mWorkManager;
    final private String mDatabaseName;
    final private String mApiKey;
    final private String mKey;
    final private SplitClientConfig mSplitClientConfig;
    final private Constraints mConstraints;
    private WeakReference<SplitTaskExecutionListener> mFetcherExecutionListener;
    // This variable is used to avoid loading data first time
    // we receive enqueued event
    final private Set<String> mShouldLoadFromLocal;


    public WorkManagerWrapper(@NonNull WorkManager workManager,
                              @NonNull SplitClientConfig splitClientConfig,
                              @NonNull String apiKey, @NonNull String databaseName,
                              @NonNull String key) {
        mWorkManager = checkNotNull(workManager);
        mDatabaseName = checkNotNull(databaseName);
        mSplitClientConfig = checkNotNull(splitClientConfig);
        mApiKey = checkNotNull(apiKey);
        mKey = checkNotNull(key);
        mShouldLoadFromLocal = new HashSet<>();
        mConstraints = buildConstraints();
    }

    public void setFetcherExecutionListener(SplitTaskExecutionListener fetcherExecutionListener) {
        mFetcherExecutionListener = new WeakReference<>(fetcherExecutionListener);
    }

    public void removeWork() {
        mWorkManager.cancelUniqueWork(SplitTaskType.SPLITS_SYNC.toString());
        mWorkManager.cancelUniqueWork(SplitTaskType.MY_SEGMENTS_SYNC.toString());
        mWorkManager.cancelUniqueWork(SplitTaskType.EVENTS_RECORDER.toString());
        mWorkManager.cancelUniqueWork(SplitTaskType.IMPRESSIONS_RECORDER.toString());
        if (mFetcherExecutionListener != null) {
            mFetcherExecutionListener.clear();
        }
    }

    public void scheduleWork() {
        scheduleWork(SplitTaskType.SPLITS_SYNC.toString(), SplitsSyncWorker.class,
                buildSplitSyncInputData());

        scheduleWork(SplitTaskType.MY_SEGMENTS_SYNC.toString(), MySegmentsSyncWorker.class,
                buildMySegmentsSyncInputData());

        scheduleWork(SplitTaskType.EVENTS_RECORDER.toString(), EventsRecorderWorker.class,
                buildEventsRecorderInputData());

        scheduleWork(SplitTaskType.IMPRESSIONS_RECORDER.toString(),
                ImpressionsRecorderWorker.class, buildImpressionsRecorderInputData());
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
                                            ", state: " + workInfo.getState().toString());
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
        dataBuilder.putString(
                ServiceConstants.WORKER_PARAM_EVENTS_ENDPOINT, mSplitClientConfig.eventsEndpoint());
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
            Logger.d("Avoiding update for " + taskType.toString());
            mShouldLoadFromLocal.add(taskType.toString());
            return;
        }

        SplitTaskExecutionListener listener = mFetcherExecutionListener.get();
        if (listener != null) {
            Logger.d("Updating for " + taskType.toString());
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
        return buildInputData(dataBuilder.build());
    }

    private Data buildMySegmentsSyncInputData() {
        Data.Builder dataBuilder = new Data.Builder();
        dataBuilder.putString(ServiceConstants.WORKER_PARAM_ENDPOINT, mSplitClientConfig.endpoint());
        dataBuilder.putString(ServiceConstants.WORKER_PARAM_KEY, mKey);
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

    private Constraints buildConstraints() {
        Constraints.Builder constraintsBuilder = new Constraints.Builder();
        constraintsBuilder.setRequiredNetworkType(
                mSplitClientConfig.backgroundSyncWhenBatteryWifiOnly() ?
                        NetworkType.UNMETERED : NetworkType.CONNECTED);
        constraintsBuilder.setRequiresBatteryNotLow(
                mSplitClientConfig.backgroundSyncWhenBatteryNotLow());
        return constraintsBuilder.build();
    }
}
