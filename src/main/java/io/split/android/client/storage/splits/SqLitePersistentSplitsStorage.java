package io.split.android.client.storage.splits;

import static io.split.android.client.utils.Utils.checkNotNull;
import static io.split.android.client.utils.Utils.partition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.Split;
import io.split.android.client.service.executor.parallel.SplitParallelTaskExecutorFactory;
import io.split.android.client.service.executor.parallel.SplitParallelTaskExecutorFactoryImpl;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;

public class SqLitePersistentSplitsStorage implements PersistentSplitsStorage {

    private static final int SQL_PARAM_BIND_SIZE = 20;
    private final SplitListTransformer<SplitEntity, Split> mEntityToSplitTransformer;
    private final SplitListTransformer<Split, SplitEntity> mSplitToEntityTransformer;
    private final SplitRoomDatabase mDatabase;

    public SqLitePersistentSplitsStorage(@NonNull SplitRoomDatabase database, @NonNull SplitCipher splitCipher) {
        this(database, new SplitParallelTaskExecutorFactoryImpl(), splitCipher);
    }

    @VisibleForTesting
    public SqLitePersistentSplitsStorage(@NonNull SplitRoomDatabase database,
                                         @NonNull SplitListTransformer<SplitEntity, Split> entityToSplitTransformer,
                                         @NonNull SplitListTransformer<Split, SplitEntity> splitToEntityTransformer) {
        mDatabase = checkNotNull(database);
        mEntityToSplitTransformer = checkNotNull(entityToSplitTransformer);
        mSplitToEntityTransformer = checkNotNull(splitToEntityTransformer);
    }

    private SqLitePersistentSplitsStorage(@NonNull SplitRoomDatabase database,
                                          @NonNull SplitParallelTaskExecutorFactory executorFactory,
                                          @NonNull SplitCipher splitCipher) {
        this(database,
                new SplitEntityToSplitTransformer(executorFactory.createForList(Split.class), splitCipher),
                new SplitToSplitEntityTransformer(executorFactory.createForList(SplitEntity.class), splitCipher));
    }

    @Override
    public boolean update(ProcessedSplitChange splitChange) {

        if (splitChange == null) {
            return false;
        }
        List<String> removedSplits = splitNameList(splitChange.getArchivedSplits());
        List<SplitEntity> splitEntities = convertSplitListToEntities(splitChange.getActiveSplits());

        mDatabase.runInTransaction(new Runnable() {
            @Override
            public void run() {
                mDatabase.generalInfoDao().update(
                        new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, splitChange.getChangeNumber()));
                mDatabase.splitDao().insert(splitEntities);
                mDatabase.splitDao().delete(removedSplits);
                mDatabase.generalInfoDao().update(
                        new GeneralInfoEntity(GeneralInfoEntity.SPLITS_UPDATE_TIMESTAMP, splitChange.getUpdateTimestamp()));
            }
        });

        return true;
    }

    @Override
    public SplitsSnapshot getSnapshot() {
        SplitsSnapshotLoader loader = new SplitsSnapshotLoader(mDatabase);
        mDatabase.runInTransaction(loader);
        return new SplitsSnapshot(loadSplits(), loader.getChangeNumber(),
                loader.getUpdateTimestamp(), loader.getSplitsFilterQueryString());
    }

    @Override
    public void update(Split split) {
        List<Split> splits = new ArrayList<>();
        splits.add(split);
        mDatabase.splitDao().insert(convertSplitListToEntities(splits));
    }

    @Override
    public void updateFilterQueryString(String queryString) {
        mDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.SPLITS_FILTER_QUERY_STRING, queryString));
    }

    @Override
    public void delete(List<String> splitNames) {
        // This is to avoid an sqlite error if there are many split to delete
        List<List<String>> deleteChunk = partition(splitNames, SQL_PARAM_BIND_SIZE);
        for (List<String> splits : deleteChunk) {
            mDatabase.splitDao().delete(splits);
        }
    }

    @Override
    public void close() {
    }

    @Override
    public void clear() {
        mDatabase.runInTransaction(new Runnable() {
            @Override
            public void run() {
                mDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, -1));
                mDatabase.splitDao().deleteAll();
            }
        });
    }

    @Override
    public List<Split> getAll() {
        return loadSplits();
    }

    @Override
    @Nullable
    public String getFilterQueryString() {
        GeneralInfoEntity generalInfoEntity = mDatabase.generalInfoDao().getByName(GeneralInfoEntity.SPLITS_FILTER_QUERY_STRING);
        return generalInfoEntity != null ? generalInfoEntity.getStringValue() : null;
    }

    private List<Split> loadSplits() {
        return mEntityToSplitTransformer.transform(mDatabase.splitDao().getAll());
    }

    private List<SplitEntity> convertSplitListToEntities(List<Split> splits) {
        return mSplitToEntityTransformer.transform(splits);
    }

    private List<String> splitNameList(List<Split> splits) {
        List<String> names = new ArrayList<>();
        if (splits == null) {
            return names;
        }
        for (Split split : splits) {
            names.add(split.name);
        }
        return names;
    }

    private static class SplitsSnapshotLoader implements Runnable {
        private SplitRoomDatabase mDatabase;
        private Long mChangeNumber = -1L;
        private Long mUpdateTimestamp = 0L;
        private String mSplitsFilterQueryString = "";

        public SplitsSnapshotLoader(SplitRoomDatabase database) {
            mDatabase = database;
        }

        @Override
        public void run() {
            GeneralInfoEntity timestampEntity = mDatabase.generalInfoDao().getByName(GeneralInfoEntity.SPLITS_UPDATE_TIMESTAMP);
            GeneralInfoEntity changeNumberEntity = mDatabase.generalInfoDao().getByName(GeneralInfoEntity.CHANGE_NUMBER_INFO);
            GeneralInfoEntity filterQueryStringEntity = mDatabase.generalInfoDao().getByName(GeneralInfoEntity.SPLITS_FILTER_QUERY_STRING);
            if (changeNumberEntity != null) {
                mChangeNumber = changeNumberEntity.getLongValue();
            }

            if (timestampEntity != null) {
                mUpdateTimestamp = timestampEntity.getLongValue();
            }
            if (filterQueryStringEntity != null) {
                mSplitsFilterQueryString = filterQueryStringEntity.getStringValue();

            }
        }

        public Long getChangeNumber() {
            return mChangeNumber;
        }

        public Long getUpdateTimestamp() {
            return mUpdateTimestamp;
        }

        public String getSplitsFilterQueryString() {
            return mSplitsFilterQueryString;
        }
    }
}
