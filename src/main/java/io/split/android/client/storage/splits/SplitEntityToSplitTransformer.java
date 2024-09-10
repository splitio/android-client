package io.split.android.client.storage.splits;

import static io.split.android.client.utils.Utils.checkNotNull;
import static io.split.android.client.utils.Utils.partition;

import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import io.split.android.client.dtos.SimpleSplit;
import io.split.android.client.service.executor.parallel.SplitDeferredTaskItem;
import io.split.android.client.service.executor.parallel.SplitParallelTaskExecutor;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;

public class SplitEntityToSplitTransformer implements SplitListTransformer<String, SimpleSplit> {

    private final SplitParallelTaskExecutor<List<SimpleSplit>> mTaskExecutor;
    private final SplitCipher mSplitCipher;

    public SplitEntityToSplitTransformer(@NonNull SplitParallelTaskExecutor<List<SimpleSplit>> taskExecutor,
                                         @NonNull SplitCipher splitCipher) {
        mTaskExecutor = checkNotNull(taskExecutor);
        mSplitCipher = checkNotNull(splitCipher);
    }

    @Override
    public List<SimpleSplit> transform(List<String> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }

        int entitiesCount = entities.size();

        if (entitiesCount > mTaskExecutor.getAvailableThreads()) {
            List<List<SimpleSplit>> result = mTaskExecutor.execute(getSplitDeserializationTasks(entities, entitiesCount));
            List<SimpleSplit> splits = new ArrayList<>();
            for (List<SimpleSplit> subList : result) {
                splits.addAll(subList);
            }

            return splits;
        } else {
            return convertEntitiesToSplitList(entities, mSplitCipher);
        }
    }

    @NonNull
    private List<SplitDeferredTaskItem<List<SimpleSplit>>> getSplitDeserializationTasks(List<String> allEntities, int entitiesCount) {
        int availableThreads = mTaskExecutor.getAvailableThreads();
        int partitionSize = availableThreads > 0 ? entitiesCount / availableThreads : 1;
        List<List<String>> partitions = partition(allEntities, partitionSize);
        List<SplitDeferredTaskItem<List<SimpleSplit>>> taskList = new ArrayList<>(partitions.size());

        for (List<String> partition : partitions) {
            taskList.add(new SplitDeferredTaskItem<>(
                    new Callable<List<SimpleSplit>>() {
                        @Override
                        public List<SimpleSplit> call() {
                            return convertEntitiesToSplitList(partition, mSplitCipher);
                        }
                    }));
        }

        return taskList;
    }

    @NonNull
    private static List<SimpleSplit> convertEntitiesToSplitList(List<String> entities,
                                                                SplitCipher cipher) {
        List<SimpleSplit> splits = new ArrayList<>();

        if (entities == null) {
            return splits;
        }
long start = SystemClock.uptimeMillis();
        try {
            for (String entry : entities) {
                String json;
                try {
                    json = cipher.decrypt(entry);
                    if (json != null) {
                        SimpleSplit split = Json.fromJson(json, SimpleSplit.class);//getSplit(json);
                        if (split != null) {
                            split.originalJson = json;
                            splits.add(split);
                        }
                    }
                } catch (JsonSyntaxException e) {
                    Logger.e("Could not parse entity to split");
                }
            }
            return splits;
        } finally {
            long end = SystemClock.uptimeMillis();
            Log.d("TestingSDK", "Time to convertEntitiesToSplitList: " + (end - start) + " with partition size " + entities.size());
        }
    }
}
