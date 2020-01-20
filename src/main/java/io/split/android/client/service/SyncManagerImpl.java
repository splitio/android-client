package io.split.android.client.service;


import android.app.Service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.impressions.Impression;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.workmanager.EventsRecorderWorker;
import io.split.android.client.service.workmanager.ImpressionsRecorderWorker;
import io.split.android.client.service.workmanager.MySegmentsSyncWorker;
import io.split.android.client.service.workmanager.SplitsSyncWorker;
import io.split.android.client.storage.SplitStorageContainer;
import io.split.android.client.storage.events.PersistentEventsStorage;
import io.split.android.client.storage.impressions.PersistentImpressionsStorage;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class SyncManagerImpl implements SyncManager, SplitTaskExecutionListener {

    private final SplitTaskExecutor mTaskExecutor;
    private final SplitStorageContainer mSplitsStorageContainer;
    private final SplitClientConfig mSplitClientConfig;
    private final SplitEventsManager mSplitEventsManager;
    private final SplitTaskFactory mSplitTaskFactory;
    private WorkManager mWorkManager;

    private AtomicInteger mPushedEventCount;
    private AtomicLong mTotalEventsSizeInBytes;

    private AtomicInteger mPushedImpressionsCount;
    private AtomicLong mTotalImpressionsSizeInBytes;


    public SyncManagerImpl(@NonNull SplitClientConfig splitClientConfig,
                           @NonNull SplitTaskExecutor taskExecutor,
                           @NonNull SplitStorageContainer splitStorageContainer,
                           @NonNull SplitTaskFactory splitTaskFactory,
                           @NonNull SplitEventsManager splitEventsManager,
                           WorkManager workManager) {

        mTaskExecutor = checkNotNull(taskExecutor);
        mSplitsStorageContainer = checkNotNull(splitStorageContainer);
        mSplitClientConfig = checkNotNull(splitClientConfig);
        mSplitEventsManager = checkNotNull(splitEventsManager);
        mSplitTaskFactory = checkNotNull(splitTaskFactory);
        mPushedEventCount = new AtomicInteger(0);
        mTotalEventsSizeInBytes = new AtomicLong(0);
        mPushedImpressionsCount = new AtomicInteger(0);
        mTotalImpressionsSizeInBytes = new AtomicLong(0);
        if (mSplitClientConfig.synchronizeInBackground()) {
            mWorkManager = checkNotNull(workManager);
        }
    }

    @Override
    public void start() {
        scheduleTasks();
    }

    @Override
    public void pause() {
        mTaskExecutor.pause();
    }

    @Override
    public void resume() {
        mTaskExecutor.resume();
    }

    @Override
    public void stop() {
        mTaskExecutor.stop();
    }

    @Override
    public void flush() {
        mTaskExecutor.submit(mSplitTaskFactory.createEventsRecorderTask(), this);
        mTaskExecutor.submit(mSplitTaskFactory.createImpressionsRecorderTask(), this);
    }

    @Override
    public void pushEvent(Event event) {
        PersistentEventsStorage eventsStorage = mSplitsStorageContainer.getEventsStorage();
        eventsStorage.push(event);
        int pushedEventCount = mPushedEventCount.addAndGet(1);
        long totalEventsSizeInBytes = mTotalEventsSizeInBytes.addAndGet(event.getSizeInBytes());
        if (pushedEventCount > mSplitClientConfig.eventsQueueSize() ||
                totalEventsSizeInBytes >= ServiceConstants.MAX_EVENTS_SIZE_BYTES) {
            mPushedEventCount.set(0);
            mTotalEventsSizeInBytes.set(0);
            mTaskExecutor.submit(mSplitTaskFactory.createEventsRecorderTask(), this);
        }
    }

    @Override
    public void pushImpression(Impression impression) {

        KeyImpression keyImpression = buildKeyImpression(impression);
        PersistentImpressionsStorage impressionsStorage =
                mSplitsStorageContainer.getImpressionsStorage();
        impressionsStorage.push(keyImpression);
        int pushedImpressionCount = mPushedImpressionsCount.addAndGet(1);
        long totalImpressionsSizeInBytes =
                mTotalImpressionsSizeInBytes.addAndGet(ServiceConstants.ESTIMATED_IMPRESSION_SIZE_IN_BYTES);
        if (pushedImpressionCount > mSplitClientConfig.impressionsQueueSize() ||
                totalImpressionsSizeInBytes >= mSplitClientConfig.impressionsChunkSize()) {
            mPushedImpressionsCount.set(0);
            mTotalImpressionsSizeInBytes.set(0);
            mTaskExecutor.submit(mSplitTaskFactory.createImpressionsRecorderTask(), this);
        }
    }

    private void scheduleTasks() {
        scheduleSplitsFetcherTask();
        scheduleMySegmentsFetcherTask();
        scheduleEventsRecorderTask();
        scheduleImpressionsRecorderTask();
    }

    private void scheduleSplitsFetcherTask() {
        if (mSplitClientConfig.synchronizeInBackground()) {
            scheduleWork(SplitTaskType.SPLITS_SYNC.toString(), SplitsSyncWorker.class, ServiceConstants.DEFAULT_WORK_EXECUTION_PERIOD);
        } else {
            mTaskExecutor.schedule(mSplitTaskFactory.createSplitsSyncTask(), ServiceConstants.NO_INITIAL_DELAY,
                    mSplitClientConfig.featuresRefreshRate(), null);
        }
    }

    private void scheduleMySegmentsFetcherTask() {
        if (mSplitClientConfig.synchronizeInBackground()) {
            scheduleWork(SplitTaskType.MY_SEGMENTS_SYNC.toString(), MySegmentsSyncWorker.class, ServiceConstants.DEFAULT_WORK_EXECUTION_PERIOD);
        } else {
            mTaskExecutor.schedule(mSplitTaskFactory.createMySegmentsSyncTask(), ServiceConstants.NO_INITIAL_DELAY,
                    mSplitClientConfig.segmentsRefreshRate(), null);
        }
    }

    private void scheduleEventsRecorderTask() {
        if (mSplitClientConfig.synchronizeInBackground()) {
            scheduleWork(SplitTaskType.EVENTS_RECORDER.toString(), EventsRecorderWorker.class, ServiceConstants.DEFAULT_WORK_EXECUTION_PERIOD);
        } else {
            mTaskExecutor.schedule(mSplitTaskFactory.createEventsRecorderTask(), ServiceConstants.NO_INITIAL_DELAY,
                    mSplitClientConfig.eventFlushInterval(), this);
        }
    }

    private void scheduleImpressionsRecorderTask() {
        if (mSplitClientConfig.synchronizeInBackground()) {
            scheduleWork(SplitTaskType.IMPRESSIONS_RECORDER.toString(), ImpressionsRecorderWorker.class, ServiceConstants.DEFAULT_WORK_EXECUTION_PERIOD);
        } else {
            mTaskExecutor.schedule(mSplitTaskFactory.createImpressionsRecorderTask(), ServiceConstants.NO_INITIAL_DELAY,
                    mSplitClientConfig.impressionsRefreshRate(), this);
        }
    }

    @Override
    public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
        updateTaskStatus(taskInfo);
    }

    private void updateTaskStatus(SplitTaskExecutionInfo taskInfo) {
        switch (taskInfo.getTaskType()) {
            case EVENTS_RECORDER:
                updateEventsTaskStatus(taskInfo);
                break;
            case IMPRESSIONS_RECORDER:
                updateImpressionsTaskStatus(taskInfo);
                break;
        }
    }

    private void scheduleWork(String requestType,
                              Class<? extends ListenableWorker> workerClass,
                              long executionPeriod) {
        PeriodicWorkRequest request = new PeriodicWorkRequest
                .Builder(workerClass,
                executionPeriod,
                TimeUnit.SECONDS).build();
        mWorkManager.enqueueUniquePeriodicWork(requestType, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    private void updateEventsTaskStatus(SplitTaskExecutionInfo executionInfo) {
        if (executionInfo.getStatus() == SplitTaskExecutionStatus.ERROR) {
            mPushedEventCount.addAndGet(executionInfo.getNonSentRecords());
            mTotalEventsSizeInBytes.addAndGet(executionInfo.getNonSentBytes());
        }
    }

    private void updateImpressionsTaskStatus(SplitTaskExecutionInfo executionInfo) {
        if (executionInfo.getStatus() == SplitTaskExecutionStatus.ERROR) {
            mPushedImpressionsCount.addAndGet(executionInfo.getNonSentRecords());
            mTotalImpressionsSizeInBytes.addAndGet(executionInfo.getNonSentBytes());
        }
    }

    private KeyImpression buildKeyImpression(Impression impression) {
        KeyImpression keyImpression = new KeyImpression();
        keyImpression.feature = impression.split();
        keyImpression.keyName = impression.key();
        keyImpression.bucketingKey = impression.bucketingKey();
        keyImpression.label = impression.appliedRule();
        keyImpression.treatment = impression.treatment();
        keyImpression.time = impression.time();
        keyImpression.changeNumber = impression.changeNumber();
        return keyImpression;
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
            String taskStatusRaw = outputData .getString(ServiceConstants.TASK_INFO_FIELD_STATUS);
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
            if(taskStatus == SplitTaskExecutionStatus.SUCCESS) {
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
}
