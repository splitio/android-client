package io.split.android.client.service.executor.parallel;

import static io.split.android.client.utils.Utils.checkNotNull;

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
