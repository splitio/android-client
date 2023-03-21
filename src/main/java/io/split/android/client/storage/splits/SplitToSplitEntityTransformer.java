package io.split.android.client.storage.splits;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

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

public class SplitToSplitEntityTransformer implements SplitListTransformer<Split, SplitEntity> {

    private final SplitParallelTaskExecutor<List<SplitEntity>> mTaskExecutor;

    // TODO: Change by api key and add logic  to recover when switching
    // from encryption = YES to NO and vice-versa
//    private final SplitCipher mCipher = new SplitCipherImpl(ServiceConstants.SECRET_KEY);
//    private final SplitCipher mCipher = new SplitCipherNop();

    public SplitToSplitEntityTransformer(@NonNull SplitParallelTaskExecutor<List<SplitEntity>> taskExecutor) {
        mTaskExecutor = checkNotNull(taskExecutor);
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
            return getSplitEntities(splits);
        }
    }

    @NonNull
    private List<SplitEntity> getSplitEntities(List<Split> partition) {
        List<SplitEntity> result = new ArrayList<>();
        final SplitCipher cipher = new SplitCipherImpl(Cipher.ENCRYPT_MODE, ServiceConstants.SECRET_KEY);
//        final SplitCipher cipher = new SplitCipherNop();
        for (Split split : partition) {
            SplitEntity entity = new SplitEntity();
            entity.setName(split.name);
            String json = cipher.encrypt(Json.toJson(split));
            if (json == null) {
                continue;
            }
            entity.setBody(json);
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
                public List<SplitEntity> call() throws Exception {
                    return SplitToSplitEntityTransformer.this.getSplitEntities(partition);
                }
            }));
        }

        return taskList;
    }
}
