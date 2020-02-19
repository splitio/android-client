package io.split.android.client.service.synchronizer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.workmanager.EventsRecorderWorker;
import io.split.android.client.service.workmanager.ImpressionsRecorderWorker;
import io.split.android.client.service.workmanager.MySegmentsSyncWorker;
import io.split.android.client.service.workmanager.SplitsSyncWorker;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

@VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
public class WorkManagerWrapper {
    final private WorkManager mWorkManager;
    final private List<SplitTaskExecutionListener> mExecutionListeners;
    final private String mDatabaseName;
    final private String mApiKey;
    final private String mKey;
    final private SplitClientConfig mSplitClientConfig;

    public WorkManagerWrapper(@NonNull WorkManager workManager,
                              @NonNull SplitClientConfig splitClientConfig,
                              @NonNull String apiKey, @NonNull String databaseName,
                              @NonNull String key) {
        mWorkManager = checkNotNull(workManager);
        mDatabaseName = checkNotNull(databaseName);
        mSplitClientConfig = checkNotNull(splitClientConfig);
        mApiKey = checkNotNull(apiKey);
        mKey = checkNotNull(key);

        mExecutionListeners = new ArrayList<>();
    }

    public void addTaskExecutionListener(SplitTaskExecutionListener taskExecutionListener) {
        mExecutionListeners.add(taskExecutionListener);
    }

    public void stop() {
        mExecutionListeners.clear();
    }

    public void removeWork() {
        mWorkManager.cancelUniqueWork(SplitTaskType.SPLITS_SYNC.toString());
        mWorkManager.cancelUniqueWork(SplitTaskType.MY_SEGMENTS_SYNC.toString());
        mWorkManager.cancelUniqueWork(SplitTaskType.EVENTS_RECORDER.toString());
        mWorkManager.cancelUniqueWork(SplitTaskType.IMPRESSIONS_RECORDER.toString());
        mExecutionListeners.clear();
    }

    public void scheduleWork() {
        scheduleWork(SplitTaskType.SPLITS_SYNC.toString(), SplitsSyncWorker.class,
                buildSplitSyncInputData(), ServiceConstants.DEFAULT_WORK_EXECUTION_PERIOD);

        scheduleWork(SplitTaskType.MY_SEGMENTS_SYNC.toString(), MySegmentsSyncWorker.class,
                buildMySegmentsSyncInputData(), ServiceConstants.DEFAULT_WORK_EXECUTION_PERIOD);

        scheduleWork(SplitTaskType.EVENTS_RECORDER.toString(), EventsRecorderWorker.class,
                buildEventsRecorderInputData(), ServiceConstants.DEFAULT_WORK_EXECUTION_PERIOD);

        scheduleWork(SplitTaskType.IMPRESSIONS_RECORDER.toString(),
                ImpressionsRecorderWorker.class, buildImpressionsRecorderInputData(),
                ServiceConstants.DEFAULT_WORK_EXECUTION_PERIOD);
    }

    private void scheduleWork(String requestType,
                              Class<? extends ListenableWorker> workerClass,
                              Data inputData,
                              long executionPeriod) {
        PeriodicWorkRequest request = new PeriodicWorkRequest
                .Builder(workerClass,
                executionPeriod,
                TimeUnit.SECONDS).setInputData(buildInputData(inputData))
                .build();
        mWorkManager.enqueueUniquePeriodicWork(requestType, ExistingPeriodicWorkPolicy.KEEP, request);
        observeWorkState(request.getId());
    }

    private void observeWorkState(UUID requestId) {
        mWorkManager.getWorkInfoByIdLiveData(requestId)
                .observe(ProcessLifecycleOwner.get(), new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(@Nullable WorkInfo workInfo) {
                        processWorkInfo(workInfo);
                    }
                });
    }

    private Data buildInputData(Data customData) {
        Data.Builder dataBuilder = new Data.Builder();
        dataBuilder.putString(ServiceConstants.WORKER_PARAM_DATABASE_NAME, mDatabaseName);
        dataBuilder.putString(ServiceConstants.WORKER_PARAM_API_KEY, mApiKey);
        dataBuilder.putString(
                ServiceConstants.WORKER_PARAM_EVENTS_ENDPOINT, mSplitClientConfig.eventsEndpoint());
        if(customData != null) {
            dataBuilder.putAll(customData);
        }
        return dataBuilder.build();
    }

    private void processWorkInfo(WorkInfo workInfo) {
        if (workInfo != null &&
                workInfo.getState() == WorkInfo.State.SUCCEEDED &&
                workInfo.getOutputData() != null) {

            Data outputData = workInfo.getOutputData();
            String taskStatusRaw = outputData.getString(ServiceConstants.TASK_INFO_FIELD_STATUS);
            String taskTypeRaw = outputData.getString(ServiceConstants.TASK_INFO_FIELD_TYPE);

            SplitTaskExecutionStatus taskStatus;
            SplitTaskType taskType;
            try {
                taskStatus = SplitTaskExecutionStatus.valueOf(taskStatusRaw);
                taskType = SplitTaskType.valueOf(taskTypeRaw);
            } catch (IllegalArgumentException exception) {
                Logger.e("Error while reading work status: " +
                        exception.getLocalizedMessage());
                return;
            }
            if (taskStatus == SplitTaskExecutionStatus.SUCCESS) {
                updateTaskStatus(SplitTaskExecutionInfo.success(taskType));
            } else {
                int recordNonSent = outputData.getInt(
                        ServiceConstants.TASK_INFO_FIELD_RECORDS_NON_SENT, 0);
                long bytesNonSent = outputData.getInt(
                        ServiceConstants.TASK_INFO_FIELD_BYTES_NON_SET, 0);
                updateTaskStatus(SplitTaskExecutionInfo.error(taskType, recordNonSent, bytesNonSent));
            }
        }
    }

    private void updateTaskStatus(SplitTaskExecutionInfo taskInfo) {
        for(SplitTaskExecutionListener executionListener : mExecutionListeners) {
            executionListener.taskExecuted(taskInfo);
        }
    }

    private Data buildSplitSyncInputData() {
        Data.Builder dataBuilder = new Data.Builder();
        dataBuilder.putString(ServiceConstants.WORKER_PARAM_ENDPOINT, mSplitClientConfig.endpoint());
        return buildInputData(dataBuilder.build());
    }

    private Data buildMySegmentsSyncInputData() {
        Data.Builder dataBuilder = new Data.Builder();
        dataBuilder.putString(ServiceConstants.WORKER_PARAM_ENDPOINT, mSplitClientConfig.endpoint());
        dataBuilder.putString(ServiceConstants.WORKER_PARAM_KEY, mKey);
        return buildInputData(dataBuilder.build());
    }

    private Data buildEventsRecorderInputData() {
        Data.Builder dataBuilder = new Data.Builder();
        dataBuilder.putString(ServiceConstants.WORKER_PARAM_ENDPOINT, mSplitClientConfig.eventsEndpoint());
        dataBuilder.putInt(ServiceConstants.WORKER_PARAM_EVENTS_PER_PUSH, mSplitClientConfig.eventsPerPush());
        return buildInputData(dataBuilder.build());
    }

    private Data buildImpressionsRecorderInputData() {
        Data.Builder dataBuilder = new Data.Builder();
        dataBuilder.putString(ServiceConstants.WORKER_PARAM_ENDPOINT, mSplitClientConfig.eventsEndpoint());
        dataBuilder.putInt(ServiceConstants.WORKER_PARAM_IMPRESSIONS_PER_PUSH, mSplitClientConfig.impressionsPerPush());
        return buildInputData(dataBuilder.build());
    }
}
