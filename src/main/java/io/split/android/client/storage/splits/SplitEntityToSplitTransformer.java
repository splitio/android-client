package io.split.android.client.storage.splits;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import com.google.common.collect.Lists;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import io.split.android.client.dtos.Split;
import io.split.android.client.service.executor.parallel.SplitDeferredTaskItem;
import io.split.android.client.service.executor.parallel.SplitParallelTaskExecutor;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;

public class SplitEntityToSplitTransformer implements SplitListTransformer<SplitEntity, Split> {

    private final SplitParallelTaskExecutor<List<Split>> mTaskExecutor;
    private final SplitCipher mSplitCipher;

    public SplitEntityToSplitTransformer(@NonNull SplitParallelTaskExecutor<List<Split>> taskExecutor,
                                         @NonNull SplitCipher splitCipher) {
        mTaskExecutor = checkNotNull(taskExecutor);
        mSplitCipher = checkNotNull(splitCipher);
    }

    @Override
    public List<Split> transform(List<SplitEntity> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }

        int entitiesCount = entities.size();

        if (entitiesCount > mTaskExecutor.getAvailableThreads()) {
            List<List<Split>> result = mTaskExecutor.execute(getSplitDeserializationTasks(entities, entitiesCount));
            List<Split> splits = new ArrayList<>();
            for (List<Split> subList : result) {
                splits.addAll(subList);
            }

            return splits;
        } else {
            return convertEntitiesToSplitList(entities, mSplitCipher);
        }
    }

    @NonNull
    private List<SplitDeferredTaskItem<List<Split>>> getSplitDeserializationTasks(List<SplitEntity> allEntities, int entitiesCount) {
        int availableThreads = mTaskExecutor.getAvailableThreads();
        int partitionSize = availableThreads > 0 ? entitiesCount / availableThreads : 1;
        List<List<SplitEntity>> partitions = Lists.partition(allEntities, partitionSize);
        List<SplitDeferredTaskItem<List<Split>>> taskList = new ArrayList<>(partitions.size());

        for (List<SplitEntity> partition : partitions) {
            taskList.add(new SplitDeferredTaskItem<>(
                    new Callable<List<Split>>() {
                        @Override
                        public List<Split> call() {
                            return convertEntitiesToSplitList(partition, mSplitCipher);
                        }
                    }));
        }

        return taskList;
    }

    @NonNull
    private static List<Split> convertEntitiesToSplitList(List<SplitEntity> entities,
                                                          SplitCipher cipher) {
        List<Split> splits = new ArrayList<>();

        if (entities == null) {
            return splits;
        }

        for (SplitEntity entity : entities) {
            String name;
            String json;
            try {
                name = cipher.decrypt(entity.getName());
                json = cipher.decrypt(entity.getBody());
                if (name != null && json != null) {
                    Split split = Json.fromJson(json, Split.class);
                    split.name = name;
                    splits.add(split);
                }
            } catch (JsonSyntaxException e) {
                Logger.e("Could not parse entity to split: " + entity.getName());
            }
        }
        return splits;
    }
}
