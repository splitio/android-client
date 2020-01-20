package io.split.android.client.service.workmanager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.ListenableWorker;
import androidx.work.WorkerFactory;
import androidx.work.WorkerParameters;

import io.split.android.client.service.executor.SplitTaskFactory;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitWorkerFactory extends WorkerFactory {
    private final SplitTaskFactory mSplitTaskFactory;

    public SplitWorkerFactory(@NonNull SplitTaskFactory splitTaskFactory) {
        mSplitTaskFactory = checkNotNull(splitTaskFactory);
    }

    @Nullable
    @Override
    public ListenableWorker createWorker(@NonNull Context appContext,
                                         @NonNull String workerClassName,
                                         @NonNull WorkerParameters workerParameters) {

        return splitTaskForName(appContext, workerClassName, workerParameters);
    }

    private SplitWorker splitTaskForName(Context appContext,
                                         String workerClassName,
                                         WorkerParameters workerParameters) {

        if (workerClassName.equals(SplitsSyncWorker.class.getSimpleName())) {
            return new SplitsSyncWorker(appContext,
                    workerParameters,
                    mSplitTaskFactory.createSplitsSyncTask());

        } else if (workerClassName.equals(MySegmentsSyncWorker.class.getSimpleName())) {
            return new SplitsSyncWorker(appContext,
                    workerParameters,
                    mSplitTaskFactory.createMySegmentsSyncTask());

        } else if (workerClassName.equals(EventsRecorderWorker.class.getSimpleName())) {
            return new SplitsSyncWorker(appContext,
                    workerParameters,
                    mSplitTaskFactory.createEventsRecorderTask());

        } else if (workerClassName.equals(ImpressionsRecorderWorker.class.getSimpleName())) {
            return new SplitsSyncWorker(appContext,
                    workerParameters,
                    mSplitTaskFactory.createImpressionsRecorderTask());
        }
        return null;
    }
}
