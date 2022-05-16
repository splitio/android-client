package io.split.android.client.service.executor;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This class receives a list of {@link SplitTask} and executes them serially.
 *
 * Execution of tasks is interrupted if one of them is unsuccessful.
 *
 * Returns information about the tasks that were executed as a {@link List} in the resulting
 * {@link SplitTaskExecutionInfo}.
 */
public class SplitTaskSerialWrapper implements SplitTask {

    public static final String SPLIT_EXTRA_EXECUTION_RESULTS = "serial_task_results";
    private final SplitTaskType mSplitTaskType;
    private final List<SplitTask> mTaskList;

    public SplitTaskSerialWrapper(SplitTaskType splitTaskType, SplitTask... tasks) {
        mSplitTaskType = splitTaskType;
        mTaskList = Arrays.asList(tasks);
    }

    public SplitTaskSerialWrapper(SplitTask...tasks) {
        this(SplitTaskType.GENERIC_TASK, tasks);
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        boolean isSuccessful = true;

        List<SplitTaskExecutionInfo> executionResults = new ArrayList<>();

        for (SplitTask task : mTaskList) {
            SplitTaskExecutionInfo executionInfo = task.execute();

            if (executionInfo != null) {
                executionResults.add(executionInfo);
                if (!SplitTaskExecutionStatus.SUCCESS.equals(executionInfo.getStatus())) {
                    isSuccessful = false;
                    break;
                }
            }
        }

        Map<String, Object> resultExtraInfo = Collections.singletonMap(SPLIT_EXTRA_EXECUTION_RESULTS, executionResults);
        if (isSuccessful) {
            return SplitTaskExecutionInfo.success(mSplitTaskType, resultExtraInfo);
        } else {
            return SplitTaskExecutionInfo.error(mSplitTaskType, resultExtraInfo);
        }
    }

    @VisibleForTesting
    public List<SplitTask> getTaskList() {
        return mTaskList;
    }
}
