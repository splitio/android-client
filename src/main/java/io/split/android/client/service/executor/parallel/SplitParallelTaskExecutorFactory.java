package io.split.android.client.service.executor.parallel;

import java.util.List;

public interface SplitParallelTaskExecutorFactory {

    <T> SplitParallelTaskExecutor<List<T>> createForList(Class<T> type);

    <T> SplitParallelTaskExecutor<T> create(Class<T> type);
}
