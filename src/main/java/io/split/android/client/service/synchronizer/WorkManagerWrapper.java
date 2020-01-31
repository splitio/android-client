package io.split.android.client.service.synchronizer;

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

import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
    final private SplitTaskExecutionListener mEventsRecorderListener;
    final private SplitTaskExecutionListener mImpressionsRecorderListener;

    public WorkManagerWrapper(WorkManager workManager,
                              SplitTaskExecutionListener eventsRecorderListener,
                              SplitTaskExecutionListener impressionsRecorderListener) {
        mWorkManager = checkNotNull(workManager);
        mEventsRecorderListener = checkNotNull(eventsRecorderListener);
        mImpressionsRecorderListener = checkNotNull(impressionsRecorderListener);
        scheduleWork();
    }

    private void scheduleWork() {
        scheduleWork(SplitTaskType.SPLITS_SYNC.toString(), SplitsSyncWorker.class,
                ServiceConstants.DEFAULT_WORK_EXECUTION_PERIOD);

        scheduleWork(SplitTaskType.MY_SEGMENTS_SYNC.toString(), MySegmentsSyncWorker.class,
                ServiceConstants.DEFAULT_WORK_EXECUTION_PERIOD);

        scheduleWork(SplitTaskType.EVENTS_RECORDER.toString(), EventsRecorderWorker.class,
                ServiceConstants.DEFAULT_WORK_EXECUTION_PERIOD);

        scheduleWork(SplitTaskType.IMPRESSIONS_RECORDER.toString(),
                ImpressionsRecorderWorker.class, ServiceConstants.DEFAULT_WORK_EXECUTION_PERIOD);
    }

    private void scheduleWork(String requestType,
                              Class<? extends ListenableWorker> workerClass,
                              long executionPeriod) {
        PeriodicWorkRequest request = new PeriodicWorkRequest
                .Builder(workerClass,
                executionPeriod,
                TimeUnit.SECONDS).build();
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
        switch (taskInfo.getTaskType()) {
            case EVENTS_RECORDER:
                mEventsRecorderListener.taskExecuted(taskInfo);
                break;
            case IMPRESSIONS_RECORDER:
                mImpressionsRecorderListener.taskExecuted(taskInfo);
                break;
        }
    }
}
