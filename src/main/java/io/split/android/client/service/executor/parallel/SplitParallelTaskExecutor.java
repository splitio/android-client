package io.split.android.client.service.executor.parallel;

import java.util.Collection;
import java.util.List;

/**
 * Used to run a group of short lived tasks in parallel.
 *
 * @param <T> Type for the return value of each task.
 */
public interface SplitParallelTaskExecutor<T> {

    List<T> execute(Collection<SplitDeferredTaskItem<T>> taskItems);
}
