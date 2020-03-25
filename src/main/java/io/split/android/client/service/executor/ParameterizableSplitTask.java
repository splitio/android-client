package io.split.android.client.service.executor;

import androidx.annotation.NonNull;

public interface ParameterizableSplitTask<T> extends SplitTask {
    void setParam(T parameter);
}
