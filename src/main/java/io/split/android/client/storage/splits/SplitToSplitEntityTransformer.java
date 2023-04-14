package io.split.android.client.storage.splits;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import io.split.android.client.dtos.Split;
import io.split.android.client.service.executor.parallel.SplitDeferredTaskItem;
import io.split.android.client.service.executor.parallel.SplitParallelTaskExecutor;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.utils.Json;

public class SplitToSplitEntityTransformer implements SplitListTransformer<Split, SplitEntity> {

    private final SplitParallelTaskExecutor<List<SplitEntity>> mTaskExecutor;
    private final SplitCipher mSplitCipher;

    public SplitToSplitEntityTransformer(@NonNull SplitParallelTaskExecutor<List<SplitEntity>> taskExecutor,
                                         @NonNull SplitCipher splitCipher) {
        mTaskExecutor = checkNotNull(taskExecutor);
        mSplitCipher = checkNotNull(splitCipher);
    }

    @Override
    public List<SplitEntity> transform(List<Split> splits) {
        List<SplitEntity> splitEntities = new ArrayList<>();

        if (splits == null) {
            return splitEntities;
        }

        int splitsSize = splits.size();
        if (splitsSize > mTaskExecutor.getAvailableThreads()) {
            List<List<SplitEntity>> subLists = mTaskExecutor.execute(getSplitEntityTasks(splits, splitsSize));

            for (List<SplitEntity> subList : subLists) {
                splitEntities.addAll(subList);
            }

            return splitEntities;

        } else {
            return getSplitEntities(splits, mSplitCipher);
        }
    }

    @NonNull
    private List<SplitEntity> getSplitEntities(List<Split> partition, SplitCipher cipher) {
        List<SplitEntity> result = new ArrayList<>();

        for (Split split : partition) {
            String encryptedJson = cipher.encrypt(Json.toJson(split));
            if (encryptedJson == null) {
                continue;
            }
            SplitEntity entity = new SplitEntity();
            entity.setName(split.name);
            entity.setBody(encryptedJson);
            entity.setUpdatedAt(System.currentTimeMillis() / 1000);
            result.add(entity);
        }

        return result;
    }

    @NonNull
    private List<SplitDeferredTaskItem<List<SplitEntity>>> getSplitEntityTasks(List<Split> splits, int splitsSize) {
        int availableThreads = mTaskExecutor.getAvailableThreads();
        int partitionSize = splitsSize / availableThreads;
        List<List<Split>> partitions = Lists.partition(splits, partitionSize);
        List<SplitDeferredTaskItem<List<SplitEntity>>> taskList = new ArrayList<>(partitions.size());

        for (List<Split> partition : partitions) {
            taskList.add(new SplitDeferredTaskItem<>(new Callable<List<SplitEntity>>() {
                @Override
                public List<SplitEntity> call() {
                    return SplitToSplitEntityTransformer.this.getSplitEntities(partition, mSplitCipher);
                }
            }));
        }

        return taskList;
    }
}
