package io.split.android.client.service.executor.parallel;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.concurrent.Callable;

public class SplitDeferredTaskItem<T> implements Callable<T> {

    private final Callable<T> mCallable;

    public SplitDeferredTaskItem(@NonNull Callable<T> callable) {
        mCallable = checkNotNull(callable);
    }

    @Override
    public T call() throws Exception {
        return mCallable.call();
    }
}
