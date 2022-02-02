package io.split.android.client.storage.splits;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.List;

import io.split.android.client.dtos.Split;
import io.split.android.client.service.executor.parallel.SplitParallelTaskExecutorFactory;
import io.split.android.client.storage.db.SplitEntity;

public class SplitEntityConverterImpl implements SplitEntityConverter {

    private final SplitListTransformer<SplitEntity, Split> mEntityToSplitTransformer;
    private final SplitListTransformer<Split, SplitEntity> mSplitToEntityTransformer;

    public SplitEntityConverterImpl(SplitParallelTaskExecutorFactory executorFactory) {
        mEntityToSplitTransformer = new SplitEntityToSplitTransformer(executorFactory.createForList(Split.class));
        mSplitToEntityTransformer = new SplitToSplitEntityTransformer(executorFactory.createForList(SplitEntity.class));
    }

    @VisibleForTesting
    public SplitEntityConverterImpl(@NonNull SplitListTransformer<SplitEntity, Split> entityToSplitTransformer,
                                    @NonNull SplitListTransformer<Split, SplitEntity> splitToEntityTransformer) {
        mEntityToSplitTransformer = checkNotNull(entityToSplitTransformer);
        mSplitToEntityTransformer = checkNotNull(splitToEntityTransformer);
    }

    @Override
    @NonNull
    public List<Split> getFromEntityList(List<SplitEntity> entities) {
        return mEntityToSplitTransformer.transform(entities);
    }

    @Override
    @NonNull
    public List<SplitEntity> getFromSplitList(List<Split> splits) {
        return mSplitToEntityTransformer.transform(splits);
    }
}
