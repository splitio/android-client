package io.split.android.client.storage.splits;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import com.google.common.collect.Lists;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.Split;
import io.split.android.client.service.executor.parallel.SplitDeferredTaskItem;
import io.split.android.client.service.executor.parallel.SplitParallelTaskExecutor;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

public class SplitEntityConverterImpl implements SplitEntityConverter {

    private final SplitParallelTaskExecutor<List<Split>> mParallelTaskExecutor;

    public SplitEntityConverterImpl(@NonNull SplitParallelTaskExecutor<List<Split>> parallelTaskExecutor) {
        mParallelTaskExecutor = checkNotNull(parallelTaskExecutor);
    }

    @Override
    public List<Split> getFromEntityList(List<SplitEntity> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }

        int entitiesCount = entities.size();

        if (entitiesCount > mParallelTaskExecutor.getAvailableThreads()) {
            List<List<Split>> result = mParallelTaskExecutor.execute(getSplitDeserializationTasks(entities, entitiesCount));
            List<Split> splits = new ArrayList<>();
            for (List<Split> subList : result) {
                splits.addAll(subList);
            }

            return splits;
        } else {
            return convertEntitiesToSplitList(entities);
        }
    }

    private List<SplitDeferredTaskItem<List<Split>>> getSplitDeserializationTasks(List<SplitEntity> allEntities, int entitiesCount) {
        int availableThreads = mParallelTaskExecutor.getAvailableThreads();
        int partitionSize = availableThreads > 0 ? entitiesCount / availableThreads : 1;
        List<List<SplitEntity>> partitions = Lists.partition(allEntities, partitionSize);
        List<SplitDeferredTaskItem<List<Split>>> taskList = new ArrayList<>(partitions.size());

        for (List<SplitEntity> partition : partitions) {
            taskList.add(new SplitDeferredTaskItem<>(() -> convertEntitiesToSplitList(partition)));
        }

        return taskList;
    }

    private List<Split> convertEntitiesToSplitList(List<SplitEntity> entities) {
        List<Split> splits = new ArrayList<>();

        if (entities == null) {
            return splits;
        }

        for (SplitEntity entity : entities) {
            try {
                splits.add(Json.fromJson(entity.getBody(), Split.class));
            } catch (JsonSyntaxException e) {
                Logger.e("Could not parse entity to split: " + entity.getName());
            }
        }
        return splits;
    }
}
