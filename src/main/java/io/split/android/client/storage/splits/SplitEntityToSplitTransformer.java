package io.split.android.client.storage.splits;

import static io.split.android.client.utils.Utils.checkNotNull;
import static io.split.android.client.utils.Utils.partition;

import androidx.annotation.NonNull;

import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import io.split.android.client.dtos.SimpleSplit;
import io.split.android.client.service.executor.parallel.SplitDeferredTaskItem;
import io.split.android.client.service.executor.parallel.SplitParallelTaskExecutor;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;

public class SplitEntityToSplitTransformer implements SplitTransformer<Map<String, String>, List<SimpleSplit>> {

    private final SplitParallelTaskExecutor<List<SimpleSplit>> mTaskExecutor;
    private final SplitCipher mSplitCipher;

    public SplitEntityToSplitTransformer(@NonNull SplitParallelTaskExecutor<List<SimpleSplit>> taskExecutor,
                                         @NonNull SplitCipher splitCipher) {
        mTaskExecutor = checkNotNull(taskExecutor);
        mSplitCipher = checkNotNull(splitCipher);
    }

    @Override
    public List<SimpleSplit> transform(Map<String, String> entities) {
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
    private List<SplitDeferredTaskItem<List<SimpleSplit>>> getSplitDeserializationTasks(Map<String, String> allEntities, int entitiesCount) {
        int availableThreads = mTaskExecutor.getAvailableThreads();
        int partitionSize = availableThreads > 0 ? entitiesCount / availableThreads : 1;
        List<Map<String, String>> partitions = partition(allEntities, partitionSize);
        List<SplitDeferredTaskItem<List<SimpleSplit>>> taskList = new ArrayList<>(partitions.size());

        for (Map<String, String> partition : partitions) {
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
    private static List<SimpleSplit> convertEntitiesToSplitList(Map<String, String> entities,
                                                                SplitCipher cipher) {
        List<SimpleSplit> splits = new ArrayList<>();

        if (entities == null) {
            return splits;
        }

        for (Map.Entry<String, String> entry : entities.entrySet()) {
            String name;
            String json;
            try {
                name = cipher.decrypt(entry.getKey());
                json = cipher.decrypt(entry.getValue());
                if (name != null && json != null) {
                    SimpleSplit split = getSplit(json);
                    if (split != null) {
                        split.name = name;
                        split.originalJson = json;
                        splits.add(split);
                    }
                }
            } catch (JsonSyntaxException e) {
                Logger.e("Could not parse entity to split: " + entry.getKey());
            }
        }
        return splits;
    }

    private static SimpleSplit getSplit(String json) {
        //return Json.fromJson(json, Split.class);
//        return new Split();
        return Json.fromJson(json, SimpleSplit.class);
    }
}
