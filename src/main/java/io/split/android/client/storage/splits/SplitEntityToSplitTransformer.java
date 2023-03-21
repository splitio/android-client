package io.split.android.client.storage.splits;

import androidx.annotation.NonNull;

import com.google.common.collect.Lists;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;

import io.split.android.client.common.SplitCipher;
import io.split.android.client.common.SplitCipherImpl;
import io.split.android.client.common.SplitCipherNop;
import io.split.android.client.dtos.Split;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.parallel.SplitDeferredTaskItem;
import io.split.android.client.service.executor.parallel.SplitParallelTaskExecutor;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;

public class SplitEntityToSplitTransformer implements SplitListTransformer<SplitEntity, Split> {

    private final SplitParallelTaskExecutor<List<Split>> mTaskExecutor;

    public SplitEntityToSplitTransformer(SplitParallelTaskExecutor<List<Split>> taskExecutor) {
        mTaskExecutor = taskExecutor;
    }

    @Override
    public List<Split> transform(List<SplitEntity> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }

        int entitiesCount = entities.size();

        if (entitiesCount > mTaskExecutor.getAvailableThreads()) {
            long start = System.currentTimeMillis();
            List<List<Split>> result = mTaskExecutor.execute(getSplitDeserializationTasks(entities, entitiesCount));
            Logger.v("\n\n-> PARSING TIME: " + (System.currentTimeMillis() - start));
            List<Split> splits = new ArrayList<>();
            for (List<Split> subList : result) {
                splits.addAll(subList);
            }
            return splits;
        } else {
            return convertEntitiesToSplitList(entities);
        }
    }

    @NonNull
    private List<SplitDeferredTaskItem<List<Split>>> getSplitDeserializationTasks(List<SplitEntity> allEntities, int entitiesCount) {
        int availableThreads = mTaskExecutor.getAvailableThreads();
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
//        final SplitCipher cipher = new SplitCipherNop();
        final SplitCipher cipher = new SplitCipherImpl(Cipher.DECRYPT_MODE, ServiceConstants.SECRET_KEY);
        for (SplitEntity entity : entities) {
            String json = "";
            try {
                json = cipher.decrypt(entity.getBody());
                splits.add(Json.fromJson(json, Split.class));
            } catch (JsonSyntaxException e) {
                Logger.e("JSON parsing failed for: " + entity.getName());
            }
        }

        return splits;
    }
}
