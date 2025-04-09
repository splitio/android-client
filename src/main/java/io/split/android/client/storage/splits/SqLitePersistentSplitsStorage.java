package io.split.android.client.storage.splits;

import static io.split.android.client.storage.splits.MetadataHelper.addOrUpdateFlagSets;
import static io.split.android.client.storage.splits.MetadataHelper.decreaseTrafficTypeCount;
import static io.split.android.client.storage.splits.MetadataHelper.deleteFromFlagSetsIfNecessary;
import static io.split.android.client.storage.splits.MetadataHelper.increaseTrafficTypeCount;
import static io.split.android.client.utils.Utils.checkNotNull;
import static io.split.android.client.utils.Utils.partition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.SplitFactoryImpl;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.Status;
import io.split.android.client.service.executor.parallel.SplitParallelTaskExecutorFactory;
import io.split.android.client.service.executor.parallel.SplitParallelTaskExecutorFactoryImpl;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.SplitFactoryImpl.StartupTimeTracker;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;

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
    public boolean update(ProcessedSplitChange splitChange, Map<String, Integer> mTrafficTypes, Map<String, Set<String>> mFlagSets) {

        if (splitChange == null) {
            return false;
        }

        // Process archived splits
        List<Split> archivedSplits = splitChange.getArchivedSplits();
        List<String> removedSplitNames = splitNameList(archivedSplits);

        SplitToSplitEntityTransformer transformer = (SplitToSplitEntityTransformer) mSplitToEntityTransformer;
        List<SplitEntity> splitEntities = transformer.transform(splitChange.getActiveSplits());

        mDatabase.runInTransaction(new Runnable() {
            @Override
            public void run() {
                mDatabase.generalInfoDao().update(
                        new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, splitChange.getChangeNumber()));
                mDatabase.splitDao().insert(splitEntities);
                mDatabase.splitDao().delete(removedSplitNames);
                mDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.FLAG_SETS_MAP, Json.toJson(mFlagSets)));
                mDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.TRAFFIC_TYPES_MAP, Json.toJson(mTrafficTypes)));
                mDatabase.generalInfoDao().update(
                        new GeneralInfoEntity(GeneralInfoEntity.SPLITS_UPDATE_TIMESTAMP, splitChange.getUpdateTimestamp()));
            }
        });

        return true;
    }

    private void updateTrafficTypesAndFlagSets(Map<String, Integer> trafficTypesMap, Map<String, Set<String>> flagSetsMap) {
        try {
            if (trafficTypesMap.isEmpty() && flagSetsMap.isEmpty()) {
                System.out.println(StartupTimeTracker.getElapsedTimeLog("SqLitePersistentSplitsStorage: No traffic types or flag sets to store"));
                return;
            }

            // Store traffic types
            String trafficTypesJson = Json.toJson(trafficTypesMap);
            GeneralInfoEntity trafficTypesEntity = new GeneralInfoEntity(
                GeneralInfoEntity.TRAFFIC_TYPES_MAP, 
                trafficTypesJson
            );
            trafficTypesEntity.setUpdatedAt(System.currentTimeMillis());

            // Store flag sets
            String flagSetsJson = Json.toJson(flagSetsMap);
            GeneralInfoEntity flagSetsEntity = new GeneralInfoEntity(
                GeneralInfoEntity.FLAG_SETS_MAP, 
                flagSetsJson
            );
            flagSetsEntity.setUpdatedAt(System.currentTimeMillis());

            // Update in database
            mDatabase.generalInfoDao().update(trafficTypesEntity);
            mDatabase.generalInfoDao().update(flagSetsEntity);
            
            System.out.println(StartupTimeTracker.getElapsedTimeLog("SqLitePersistentSplitsStorage: Stored traffic types (" + 
                    trafficTypesMap.size() + ") and flag sets (" + flagSetsMap.size() + ")"));

        } catch (Exception e) {
            System.out.println("Error updating traffic types and flag sets: " + e.getMessage());
        }
    }

    @Override
    public SplitsSnapshot getSnapshot() {
        System.out.println(StartupTimeTracker.getElapsedTimeLog("SqLitePersistentSplitsStorage.getSnapshot: Starting"));
        long startTime = System.currentTimeMillis();
        
        // Load splits
        List<Split> splits = loadSplits();

        // Run the snapshot loader to get metadata
        SplitsSnapshotLoader loader = new SplitsSnapshotLoader(mDatabase, splits);
        loader.run();
        
        SplitsSnapshot snapshot = new SplitsSnapshot(
            splits,
            loader.getChangeNumber(),
            loader.getUpdateTimestamp(),
            loader.getSplitsFilterQueryString(),
            loader.getFlagsSpec(),
            loader.getTrafficTypes(),
            loader.getFlagSets()
        );
        
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
                mDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.FLAG_SETS_MAP, ""));
                mDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.TRAFFIC_TYPES_MAP, ""));
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

    /**
     * Extracts traffic types and flag sets from splits
     */
    private void extractMetadataFromSplits(List<Split> splits, Map<String, Integer> outputTrafficTypeMap, Map<String, Set<String>> outputFlagSetsMap) {
        if (splits == null || splits.isEmpty()) {
            return;
        }

        System.out.println(StartupTimeTracker.getElapsedTimeLog("SqLitePersistentSplitsStorage: Extracting metadata from splits"));
        long startTime = System.currentTimeMillis();

        // Pre-allocate collections for better performance
        if (outputTrafficTypeMap.isEmpty()) {
            outputTrafficTypeMap = new HashMap<>((int)(splits.size() * 0.75));
        }
        if (outputFlagSetsMap.isEmpty()) {
            outputFlagSetsMap = new HashMap<>((int)(splits.size() * 0.25));
        }

        for (Split split : splits) {
            // Process traffic type
            if (split.trafficTypeName != null) {
                String ttLower = split.trafficTypeName.toLowerCase();
                Integer count = outputTrafficTypeMap.get(ttLower);
                outputTrafficTypeMap.put(ttLower, (count == null) ? 1 : count + 1);
            }

            // Process flag sets
            if (split.sets != null && !split.sets.isEmpty()) {
                for (String set : split.sets) {
                    Set<String> splitNames = outputFlagSetsMap.get(set);
                    if (splitNames == null) {
                        splitNames = new HashSet<>();
                        outputFlagSetsMap.put(set, splitNames);
                    }
                    splitNames.add(split.name);
                }
            }
        }

        // Store extracted metadata for future use
        updateTrafficTypesAndFlagSets(outputTrafficTypeMap, outputFlagSetsMap);

        System.out.println(StartupTimeTracker.getElapsedTimeLog("SqLitePersistentSplitsStorage: Extracted metadata in " +
                (System.currentTimeMillis() - startTime) + "ms, found " +
                outputTrafficTypeMap.size() + " traffic types and " + outputFlagSetsMap.size() + " flag sets"));
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
        if (splits == null) {
            return new ArrayList<>();
        }
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
        private Map<String, Integer> mTrafficTypes = new ConcurrentHashMap<>();
        private Map<String, Set<String>> mFlagSets = new ConcurrentHashMap<>();
        private final List<Split> mSplits;

        public SplitsSnapshotLoader(SplitRoomDatabase database, List<Split> splits) {
            mDatabase = database;
            mSplits = splits;
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

            System.out.println(StartupTimeTracker.getElapsedTimeLog("SplitsSnapshotLoader.run: Getting traffic types"));
            long trafficTypesStartTime = System.currentTimeMillis();
            GeneralInfoEntity trafficTypesEntity = mDatabase.generalInfoDao().getByName(GeneralInfoEntity.TRAFFIC_TYPES_MAP);
            System.out.println(StartupTimeTracker.getElapsedTimeLog("SplitsSnapshotLoader.run: Got traffic types in " +
                    (System.currentTimeMillis() - trafficTypesStartTime) + "ms"));

            System.out.println(StartupTimeTracker.getElapsedTimeLog("SplitsSnapshotLoader.run: Getting flag sets"));
            long flagSetsStartTime = System.currentTimeMillis();
            GeneralInfoEntity flagSetsEntity = mDatabase.generalInfoDao().getByName(GeneralInfoEntity.FLAG_SETS_MAP);
            System.out.println(StartupTimeTracker.getElapsedTimeLog("SplitsSnapshotLoader.run: Got flag sets in " +
                    (System.currentTimeMillis() - flagSetsStartTime) + "ms"));

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

            boolean trafficTypesAndSetsMigrationRequired = !mSplits.isEmpty() && trafficTypesEntity == null && flagSetsEntity == null;
            if (trafficTypesAndSetsMigrationRequired) {
                Logger.i("Migration required for cached traffic types and flag sets. Migrating now.");
                System.out.println(SplitFactoryImpl.StartupTimeTracker.getElapsedTimeLog("Migration required for cached traffic types and flag sets. Migrating now."));
                try {
                    for (Split split : mSplits) {
                        if (split.status == Status.ACTIVE) {
                            increaseTrafficTypeCount(split.trafficTypeName, mTrafficTypes);
                            addOrUpdateFlagSets(split, mFlagSets);
                        } else {
                            decreaseTrafficTypeCount(split.trafficTypeName, mTrafficTypes);
                            deleteFromFlagSetsIfNecessary(split, mFlagSets);
                        }
                    }

                    mDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.TRAFFIC_TYPES_MAP, Json.toJson(mTrafficTypes)));
                    mDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.FLAG_SETS_MAP, Json.toJson(mFlagSets)));
                } catch (Exception e) {
                    Logger.e("Failed to migrate traffic types and flag sets", e);
                }
            } else {
                Logger.v("Migration not required");
                if (trafficTypesEntity != null) {
                    Type mapType = new TypeToken<Map<String, Integer>>(){}.getType();
                    mTrafficTypes = Json.fromJson(trafficTypesEntity.getStringValue(), mapType);
                    System.out.println(SplitFactoryImpl.StartupTimeTracker.getElapsedTimeLog("Traffic types loaded from general info: " + mTrafficTypes.size()));
                }

                if (flagSetsEntity != null) {
                    Type mapType = new TypeToken<Map<String, Set<String>>>(){}.getType();
                    mFlagSets = Json.fromJson(flagSetsEntity.getStringValue(), mapType);
                    System.out.println(SplitFactoryImpl.StartupTimeTracker.getElapsedTimeLog( "Flag sets loaded from general info: " + mFlagSets.size()));
                }
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

        public Map<String, Integer> getTrafficTypes() {
            return mTrafficTypes;
        }

        public Map<String, Set<String>> getFlagSets() {
            return mFlagSets;
        }

        private void migrateIfNeeded() {

        }
    }
}
