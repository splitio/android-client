package io.split.android.client.service.executor;

import androidx.annotation.NonNull;

public interface SplitTask {
    @NonNull
    SplitTaskExecutionInfo execute();
}
