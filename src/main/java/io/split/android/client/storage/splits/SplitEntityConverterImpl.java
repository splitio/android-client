package io.split.android.client.storage.splits;

import androidx.annotation.NonNull;

import com.google.common.collect.Lists;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.Split;
import io.split.android.client.service.executor.parallel.SplitDeferredTaskItem;
import io.split.android.client.service.executor.parallel.SplitParallelTaskExecutor;
import io.split.android.client.service.executor.parallel.SplitParallelTaskExecutorFactory;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

public class SplitEntityConverterImpl implements SplitEntityConverter {

    private final SplitParallelTaskExecutor<List<Split>> mSplitTaskExecutor;
    private final SplitParallelTaskExecutor<List<SplitEntity>> mEntityTaskExecutor;

    public SplitEntityConverterImpl(SplitParallelTaskExecutorFactory executorFactory) {
        mSplitTaskExecutor = executorFactory.createForList(Split.class);
        mEntityTaskExecutor = executorFactory.createForList(SplitEntity.class);
    }

    @Override
    @NonNull
    public List<Split> getFromEntityList(List<SplitEntity> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }

        int entitiesCount = entities.size();

        if (entitiesCount > mSplitTaskExecutor.getAvailableThreads()) {
            List<List<Split>> result = mSplitTaskExecutor.execute(getSplitDeserializationTasks(entities, entitiesCount));
            List<Split> splits = new ArrayList<>();
            for (List<Split> subList : result) {
                splits.addAll(subList);
            }

            return splits;
        } else {
            return convertEntitiesToSplitList(entities);
        }
    }

    @Override
    @NonNull
    public List<SplitEntity> getFromSplitList(List<Split> splits) {
        List<SplitEntity> splitEntities = new ArrayList<>();

        if (splits == null) {
            return splitEntities;
        }

        int splitsSize = splits.size();
        if (splitsSize > mEntityTaskExecutor.getAvailableThreads()) {
            List<List<SplitEntity>> subLists = mEntityTaskExecutor.execute(getSplitEntityTasks(splits, splitsSize));

            for (List<SplitEntity> subList : subLists) {
                splitEntities.addAll(subList);
            }

            return splitEntities;

        } else {
            return getSplitEntities(splits);
        }
    }

    @NonNull
    private List<SplitEntity> getSplitEntities(List<Split> partition) {
        List<SplitEntity> result = new ArrayList<>();

        for (Split split : partition) {
            SplitEntity entity = new SplitEntity();
            entity.setName(split.name);
            entity.setBody(Json.toJson(split));
            entity.setUpdatedAt(System.currentTimeMillis() / 1000);
            result.add(entity);
        }

        return result;
    }

    @NonNull
    private List<SplitDeferredTaskItem<List<SplitEntity>>> getSplitEntityTasks(List<Split> splits, int splitsSize) {
        int availableThreads = mEntityTaskExecutor.getAvailableThreads();
        int partitionSize = splitsSize / availableThreads;
        List<List<Split>> partitions = Lists.partition(splits, partitionSize);
        List<SplitDeferredTaskItem<List<SplitEntity>>> taskList = new ArrayList<>(partitions.size());

        for (List<Split> partition : partitions) {
            taskList.add(new SplitDeferredTaskItem<>(() -> getSplitEntities(partition)));
        }

        return taskList;
    }

    @NonNull
    private List<SplitDeferredTaskItem<List<Split>>> getSplitDeserializationTasks(List<SplitEntity> allEntities, int entitiesCount) {
        int availableThreads = mSplitTaskExecutor.getAvailableThreads();
        int partitionSize = availableThreads > 0 ? entitiesCount / availableThreads : 1;
        List<List<SplitEntity>> partitions = Lists.partition(allEntities, partitionSize);
        List<SplitDeferredTaskItem<List<Split>>> taskList = new ArrayList<>(partitions.size());

        for (List<SplitEntity> partition : partitions) {
            taskList.add(new SplitDeferredTaskItem<>(() -> convertEntitiesToSplitList(partition)));
        }

        return taskList;
    }

    @NonNull
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
