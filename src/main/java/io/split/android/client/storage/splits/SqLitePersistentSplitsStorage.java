package io.split.android.client.storage.splits;

import static io.split.android.client.utils.Utils.checkNotNull;
import static io.split.android.client.utils.Utils.partition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.Split;
import io.split.android.client.service.executor.parallel.SplitParallelTaskExecutorFactory;
import io.split.android.client.service.executor.parallel.SplitParallelTaskExecutorFactoryImpl;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.SplitFactoryImpl.StartupTimeTracker;

public class SqLitePersistentSplitsStorage implements PersistentSplitsStorage {

    private static final int SQL_PARAM_BIND_SIZE = 20;
    private final SplitListTransformer<SplitEntity, Split> mEntityToSplitTransformer;
    private final SplitListTransformer<Split, SplitEntity> mSplitToEntityTransformer;
    private final SplitRoomDatabase mDatabase;
    private final SplitCipher mCipher;

    public SqLitePersistentSplitsStorage(@NonNull SplitRoomDatabase database, @NonNull SplitCipher splitCipher) {
        this(database, new SplitParallelTaskExecutorFactoryImpl(), splitCipher);
    }

    @VisibleForTesting
    public SqLitePersistentSplitsStorage(@NonNull SplitRoomDatabase database,
                                         @NonNull SplitListTransformer<SplitEntity, Split> entityToSplitTransformer,
                                         @NonNull SplitListTransformer<Split, SplitEntity> splitToEntityTransformer,
                                         @NonNull SplitCipher cipher) {
        mDatabase = checkNotNull(database);
        mEntityToSplitTransformer = checkNotNull(entityToSplitTransformer);
        mSplitToEntityTransformer = checkNotNull(splitToEntityTransformer);
        mCipher = checkNotNull(cipher);
    }

    private SqLitePersistentSplitsStorage(@NonNull SplitRoomDatabase database,
                                          @NonNull SplitParallelTaskExecutorFactory executorFactory,
                                          @NonNull SplitCipher splitCipher) {
        this(database,
                new SplitEntityToSplitTransformer(splitCipher),
                new SplitToSplitEntityTransformer(executorFactory.createForList(SplitEntity.class), splitCipher),
                splitCipher);
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
        System.out.println(StartupTimeTracker.getElapsedTimeLog("SqLitePersistentSplitsStorage.getSnapshot: Starting"));
        long startTime = System.currentTimeMillis();
        
        SplitsSnapshotLoader loader = new SplitsSnapshotLoader(mDatabase);
        System.out.println(StartupTimeTracker.getElapsedTimeLog("SqLitePersistentSplitsStorage.getSnapshot: Running database transaction"));
        long transactionStartTime = System.currentTimeMillis();
        mDatabase.runInTransaction(loader);
        System.out.println(StartupTimeTracker.getElapsedTimeLog("SqLitePersistentSplitsStorage.getSnapshot: Database transaction completed in " + 
                (System.currentTimeMillis() - transactionStartTime) + "ms"));
        
        System.out.println(StartupTimeTracker.getElapsedTimeLog("SqLitePersistentSplitsStorage.getSnapshot: Loading splits"));
        long loadSplitsStartTime = System.currentTimeMillis();
        List<Split> splits = loadSplits();
        System.out.println(StartupTimeTracker.getElapsedTimeLog("SqLitePersistentSplitsStorage.getSnapshot: Loaded " + 
                (splits != null ? splits.size() : 0) + " splits in " + (System.currentTimeMillis() - loadSplitsStartTime) + "ms"));
        
        SplitsSnapshot snapshot = new SplitsSnapshot(splits, loader.getChangeNumber(),
                loader.getUpdateTimestamp(), loader.getSplitsFilterQueryString(), loader.getFlagsSpec());
        
        System.out.println(StartupTimeTracker.getElapsedTimeLog("SqLitePersistentSplitsStorage.getSnapshot: Completed in " + 
                (System.currentTimeMillis() - startTime) + "ms"));
        return snapshot;
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

    @Nullable
    @Override
    public String getFlagsSpec() {
        GeneralInfoEntity generalInfoEntity = mDatabase.generalInfoDao().getByName(GeneralInfoEntity.FLAGS_SPEC);
        return generalInfoEntity != null ? generalInfoEntity.getStringValue() : null;
    }

    @Override
    public void updateFlagsSpec(String flagsSpec) {
        mDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.FLAGS_SPEC, flagsSpec));
    }

    @Override
    public void delete(List<String> splitNames) {
        List<String> encryptedNames = new ArrayList<>();
        for (String splitName : splitNames) {
            encryptedNames.add(mCipher.encrypt(splitName));
        }

        // This is to avoid an sqlite error if there are many split to delete
        List<List<String>> deleteChunk = partition(encryptedNames, SQL_PARAM_BIND_SIZE);
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
        System.out.println(StartupTimeTracker.getElapsedTimeLog("SqLitePersistentSplitsStorage.loadSplits: Starting"));
        long startTime = System.currentTimeMillis();
        
        System.out.println(StartupTimeTracker.getElapsedTimeLog("SqLitePersistentSplitsStorage.loadSplits: Getting all split entities from database"));
        long dbStartTime = System.currentTimeMillis();
        
        // Use the optimized SplitQueryDao for better performance
        Map<String, SplitEntity> allNamesAndBodies = mDatabase.getSplitQueryDao().getAllAsMap();
        
        System.out.println(StartupTimeTracker.getElapsedTimeLog("SqLitePersistentSplitsStorage.loadSplits: Got " + 
                (allNamesAndBodies != null ? allNamesAndBodies.size() : 0) + " split entities in " + (System.currentTimeMillis() - dbStartTime) + "ms"));
        
        System.out.println(StartupTimeTracker.getElapsedTimeLog("SqLitePersistentSplitsStorage.loadSplits: Transforming entities to splits"));
        long transformStartTime = System.currentTimeMillis();
        List<Split> splits = mEntityToSplitTransformer.transform(allNamesAndBodies);
        System.out.println(StartupTimeTracker.getElapsedTimeLog("SqLitePersistentSplitsStorage.loadSplits: Transformed to " + 
                (splits != null ? splits.size() : 0) + " splits in " + (System.currentTimeMillis() - transformStartTime) + "ms"));
        
        System.out.println(StartupTimeTracker.getElapsedTimeLog("SqLitePersistentSplitsStorage.loadSplits: Completed in " + 
                (System.currentTimeMillis() - startTime) + "ms"));
        return splits;
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
            names.add(mCipher.encrypt(split.name));
        }
        return names;
    }

    private static class SplitsSnapshotLoader implements Runnable {
        private final SplitRoomDatabase mDatabase;
        private Long mChangeNumber = -1L;
        private Long mUpdateTimestamp = 0L;
        private String mSplitsFilterQueryString = "";
        private String mFlagsSpec = "";

        public SplitsSnapshotLoader(SplitRoomDatabase database) {
            mDatabase = database;
        }

        @Override
        public void run() {
            System.out.println(StartupTimeTracker.getElapsedTimeLog("SplitsSnapshotLoader.run: Starting"));
            long startTime = System.currentTimeMillis();
            
            System.out.println(StartupTimeTracker.getElapsedTimeLog("SplitsSnapshotLoader.run: Getting timestamp entity"));
            long timestampStartTime = System.currentTimeMillis();
            GeneralInfoEntity timestampEntity = mDatabase.generalInfoDao().getByName(GeneralInfoEntity.SPLITS_UPDATE_TIMESTAMP);
            System.out.println(StartupTimeTracker.getElapsedTimeLog("SplitsSnapshotLoader.run: Got timestamp entity in " + 
                    (System.currentTimeMillis() - timestampStartTime) + "ms"));
            
            System.out.println(StartupTimeTracker.getElapsedTimeLog("SplitsSnapshotLoader.run: Getting change number entity"));
            long changeNumberStartTime = System.currentTimeMillis();
            GeneralInfoEntity changeNumberEntity = mDatabase.generalInfoDao().getByName(GeneralInfoEntity.CHANGE_NUMBER_INFO);
            System.out.println(StartupTimeTracker.getElapsedTimeLog("SplitsSnapshotLoader.run: Got change number entity in " + 
                    (System.currentTimeMillis() - changeNumberStartTime) + "ms"));
            
            System.out.println(StartupTimeTracker.getElapsedTimeLog("SplitsSnapshotLoader.run: Getting filter query string entity"));
            long filterQueryStartTime = System.currentTimeMillis();
            GeneralInfoEntity filterQueryStringEntity = mDatabase.generalInfoDao().getByName(GeneralInfoEntity.SPLITS_FILTER_QUERY_STRING);
            System.out.println(StartupTimeTracker.getElapsedTimeLog("SplitsSnapshotLoader.run: Got filter query string entity in " + 
                    (System.currentTimeMillis() - filterQueryStartTime) + "ms"));
            
            System.out.println(StartupTimeTracker.getElapsedTimeLog("SplitsSnapshotLoader.run: Getting flags spec entity"));
            long flagsSpecStartTime = System.currentTimeMillis();
            GeneralInfoEntity flagsSpecEntity = mDatabase.generalInfoDao().getByName(GeneralInfoEntity.FLAGS_SPEC);
            System.out.println(StartupTimeTracker.getElapsedTimeLog("SplitsSnapshotLoader.run: Got flags spec entity in " + 
                    (System.currentTimeMillis() - flagsSpecStartTime) + "ms"));

            if (changeNumberEntity != null) {
                mChangeNumber = changeNumberEntity.getLongValue();
            }

            if (timestampEntity != null) {
                mUpdateTimestamp = timestampEntity.getLongValue();
            }

            if (filterQueryStringEntity != null) {
                mSplitsFilterQueryString = filterQueryStringEntity.getStringValue();
            }

            if (flagsSpecEntity != null) {
                mFlagsSpec = flagsSpecEntity.getStringValue();
            }
            
            System.out.println(StartupTimeTracker.getElapsedTimeLog("SplitsSnapshotLoader.run: Completed in " + 
                    (System.currentTimeMillis() - startTime) + "ms"));
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

        public String getFlagsSpec() {
            return mFlagsSpec;
        }
    }
}
