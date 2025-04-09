package io.split.android.client.storage.splits;

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

import io.split.android.client.dtos.Split;
import io.split.android.client.service.executor.parallel.SplitParallelTaskExecutorFactory;
import io.split.android.client.service.executor.parallel.SplitParallelTaskExecutorFactoryImpl;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.SplitFactoryImpl.StartupTimeTracker;
import io.split.android.client.utils.Json;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class SqLitePersistentSplitsStorage implements PersistentSplitsStorage {

    private static final int SQL_PARAM_BIND_SIZE = 20;
    private static boolean sMetadataMigrationAttempted = false;
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
        
        // Process archived splits
        List<Split> archivedSplits = splitChange.getArchivedSplits();
        List<String> removedSplitNames = splitNameList(archivedSplits);

        SplitToSplitEntityTransformer transformer = (SplitToSplitEntityTransformer) mSplitToEntityTransformer;
        SplitToSplitEntityTransformer.TransformResult result = transformer.transformWithMetadata(splitChange.getActiveSplits());
        List<SplitEntity> splitEntities = result.getEntities();
        
        mDatabase.runInTransaction(new Runnable() {
            @Override
            public void run() {
                mDatabase.generalInfoDao().update(
                        new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, splitChange.getChangeNumber()));
                mDatabase.splitDao().insert(splitEntities);
                mDatabase.splitDao().delete(removedSplitNames);
                mDatabase.generalInfoDao().update(
                        new GeneralInfoEntity(GeneralInfoEntity.SPLITS_UPDATE_TIMESTAMP, splitChange.getUpdateTimestamp()));
            }
        });

        // Get current metadata
        Map<String, Integer> trafficTypesMap = getTrafficTypesMap();
        Map<String, Set<String>> flagSetsMap = getFlagSetsMap();
        
        // Update with new metadata from active splits
        updateMetadataWithActiveSplits(trafficTypesMap, flagSetsMap, result.getTrafficTypesMap(), result.getFlagSetsMap());
        
        // Update with archived splits (remove their contributions)
        if (archivedSplits != null && !archivedSplits.isEmpty()) {
            updateMetadataWithArchivedSplits(trafficTypesMap, flagSetsMap, archivedSplits);
        }
        
        // Store updated metadata
        updateTrafficTypesAndFlagSets(trafficTypesMap, flagSetsMap);

        return true;
    }

    /**
     * Updates metadata maps with contributions from active splits
     */
    private void updateMetadataWithActiveSplits(Map<String, Integer> trafficTypesMap, 
                                              Map<String, Set<String>> flagSetsMap,
                                              Map<String, Integer> newTrafficTypes,
                                              Map<String, Set<String>> newFlagSets) {
        // Update traffic types
        for (Map.Entry<String, Integer> entry : newTrafficTypes.entrySet()) {
            String ttName = entry.getKey();
            Integer count = entry.getValue();
            Integer currentCount = trafficTypesMap.get(ttName);
            trafficTypesMap.put(ttName, (currentCount == null) ? count : currentCount + count);
        }
        
        // Update flag sets
        for (Map.Entry<String, Set<String>> entry : newFlagSets.entrySet()) {
            String setName = entry.getKey();
            Set<String> splitNames = entry.getValue();
            
            Set<String> currentSplits = flagSetsMap.get(setName);
            if (currentSplits == null) {
                flagSetsMap.put(setName, new HashSet<>(splitNames));
            } else {
                currentSplits.addAll(splitNames);
            }
        }
    }

    /**
     * Updates metadata maps by removing contributions from archived splits
     */
    private void updateMetadataWithArchivedSplits(Map<String, Integer> trafficTypesMap, 
                                                Map<String, Set<String>> flagSetsMap,
                                                List<Split> archivedSplits) {
        // Process each archived split
        for (Split split : archivedSplits) {
            // Remove from traffic types
            if (split.trafficTypeName != null) {
                String ttLower = split.trafficTypeName.toLowerCase();
                Integer count = trafficTypesMap.get(ttLower);
                if (count != null) {
                    if (count <= 1) {
                        trafficTypesMap.remove(ttLower);
                    } else {
                        trafficTypesMap.put(ttLower, count - 1);
                    }
                }
            }
            
            // Remove from flag sets
            if (split.sets != null && !split.sets.isEmpty()) {
                for (String setName : split.sets) {
                    Set<String> splits = flagSetsMap.get(setName);
                    if (splits != null) {
                        splits.remove(split.name);
                        // Remove the set if it's now empty
                        if (splits.isEmpty()) {
                            flagSetsMap.remove(setName);
                        }
                    }
                }
            }
        }
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
    
    /**
     * Gets the stored traffic types map from GeneralInfoDao
     */
    public Map<String, Integer> getTrafficTypesMap() {
        try {
            // Check if migration is needed
            migrateMetadataIfNeeded();
            
            GeneralInfoEntity entity = mDatabase.generalInfoDao().getByName(GeneralInfoEntity.TRAFFIC_TYPES_MAP);
            if (entity != null && entity.getStringValue() != null) {
                Type mapType = new TypeToken<Map<String, Integer>>(){}.getType();
                return Json.fromJson(entity.getStringValue(), mapType);
            }
        } catch (Exception e) {
            System.out.println("Error loading traffic types map: " + e.getMessage());
        }
        return new HashMap<>();
    }
    
    /**
     * Gets the stored flag sets map from GeneralInfoDao
     */
    public Map<String, Set<String>> getFlagSetsMap() {
        try {
            // Check if migration is needed
            migrateMetadataIfNeeded();
            
            GeneralInfoEntity entity = mDatabase.generalInfoDao().getByName(GeneralInfoEntity.FLAG_SETS_MAP);
            if (entity != null && entity.getStringValue() != null) {
                Type mapType = new TypeToken<Map<String, Set<String>>>(){}.getType();
                return Json.fromJson(entity.getStringValue(), mapType);
            }
        } catch (Exception e) {
            System.out.println("Error loading flag sets map: " + e.getMessage());
        }
        return new HashMap<>();
    }

    /**
     * Ensures traffic types and flag sets metadata is populated in GeneralInfoDao.
     * If the metadata is missing but splits exist, it will extract and store the metadata.
     */
    private void migrateMetadataIfNeeded() {
        try {
            // Use a static flag to ensure we only try to migrate once per session
            if (sMetadataMigrationAttempted) {
                return;
            }
            sMetadataMigrationAttempted = true;
            
            // Check if metadata exists
            GeneralInfoEntity ttEntity = mDatabase.generalInfoDao().getByName(GeneralInfoEntity.TRAFFIC_TYPES_MAP);
            GeneralInfoEntity setsEntity = mDatabase.generalInfoDao().getByName(GeneralInfoEntity.FLAG_SETS_MAP);
            
            if (ttEntity != null && ttEntity.getStringValue() != null && 
                setsEntity != null && setsEntity.getStringValue() != null) {
                // Metadata already exists, no need to migrate
                return;
            }
            
            System.out.println(StartupTimeTracker.getElapsedTimeLog("SqLitePersistentSplitsStorage: Migrating metadata from existing splits"));
            long startTime = System.currentTimeMillis();
            
            // Load all splits
            List<Split> splits = loadSplits();
            
            if (splits == null || splits.isEmpty()) {
                System.out.println(StartupTimeTracker.getElapsedTimeLog("SqLitePersistentSplitsStorage: No splits to migrate metadata from"));
                return;
            }
            
            // Create maps to populate
            Map<String, Integer> migratedTrafficTypes = new HashMap<String, Integer>();
            Map<String, Set<String>> migratedFlagSets = new HashMap<String, Set<String>>();
            
            // Extract metadata from splits
            extractMetadataFromSplits(splits, migratedTrafficTypes, migratedFlagSets);
            
            System.out.println(StartupTimeTracker.getElapsedTimeLog("SqLitePersistentSplitsStorage: Migration completed in " + 
                    (System.currentTimeMillis() - startTime) + "ms, migrated " + 
                    migratedTrafficTypes.size() + " traffic types and " + migratedFlagSets.size() + " flag sets"));
            
        } catch (Exception e) {
            System.out.println("Error migrating metadata: " + e.getMessage());
        }
    }

    @Override
    public SplitsSnapshot getSnapshot() {
        System.out.println(StartupTimeTracker.getElapsedTimeLog("SqLitePersistentSplitsStorage.getSnapshot: Starting"));
        long startTime = System.currentTimeMillis();
        
        // Load splits
        List<Split> splits = loadSplits();
        
        // Run the snapshot loader to get metadata
        SplitsSnapshotLoader loader = new SplitsSnapshotLoader(mDatabase);
        loader.run();
        
        // Get traffic types and flag sets
        Map<String, Integer> trafficTypesMap = new HashMap<>();
        Map<String, Set<String>> flagSetsMap = new HashMap<>();
        
        // Load traffic types and flag sets from GeneralInfoDao
        boolean loadedMetadata = loadMetadataFromGeneralInfo(trafficTypesMap, flagSetsMap);
        
        // If metadata wasn't loaded from GeneralInfoDao, extract it from splits
        if (!loadedMetadata) {
            extractMetadataFromSplits(splits, trafficTypesMap, flagSetsMap);
        }
        
        SplitsSnapshot snapshot = new SplitsSnapshot(
            splits,
            loader.getChangeNumber(),
            loader.getUpdateTimestamp(),
            loader.getSplitsFilterQueryString(),
            loader.getFlagsSpec(),
            trafficTypesMap,
            flagSetsMap
        );
        
        System.out.println(StartupTimeTracker.getElapsedTimeLog("SqLitePersistentSplitsStorage.getSnapshot: Completed in " + 
                (System.currentTimeMillis() - startTime) + "ms"));
        
        return snapshot;
    }
    
    /**
     * Loads metadata from GeneralInfoDao
     * @return true if metadata was successfully loaded, false otherwise
     */
    private boolean loadMetadataFromGeneralInfo(Map<String, Integer> trafficTypesMap, Map<String, Set<String>> flagSetsMap) {
        GeneralInfoEntity ttEntity = mDatabase.generalInfoDao().getByName(GeneralInfoEntity.TRAFFIC_TYPES_MAP);
        GeneralInfoEntity setsEntity = mDatabase.generalInfoDao().getByName(GeneralInfoEntity.FLAG_SETS_MAP);
        
        if (ttEntity != null && ttEntity.getStringValue() != null && 
            setsEntity != null && setsEntity.getStringValue() != null) {
            // Load from GeneralInfoDao
            System.out.println(StartupTimeTracker.getElapsedTimeLog("SqLitePersistentSplitsStorage: Loading metadata from GeneralInfoDao"));
            long metadataStartTime = System.currentTimeMillis();
            
            try {
                Type ttMapType = new TypeToken<Map<String, Integer>>(){}.getType();
                Map<String, Integer> loadedTrafficTypes = Json.fromJson(ttEntity.getStringValue(), ttMapType);
                trafficTypesMap.putAll(loadedTrafficTypes);
                
                Type setsMapType = new TypeToken<Map<String, Set<String>>>(){}.getType();
                Map<String, Set<String>> loadedFlagSets = Json.fromJson(setsEntity.getStringValue(), setsMapType);
                for (Map.Entry<String, Set<String>> entry : loadedFlagSets.entrySet()) {
                    flagSetsMap.put(entry.getKey(), new HashSet<>(entry.getValue()));
                }
                
                System.out.println(StartupTimeTracker.getElapsedTimeLog("SqLitePersistentSplitsStorage: Loaded metadata in " + 
                        (System.currentTimeMillis() - metadataStartTime) + "ms"));
                return true;
            } catch (Exception e) {
                System.out.println("Error loading metadata: " + e.getMessage());
                return false;
            }
        }
        return false;
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

    /**
     * Extracts traffic types and flag sets from splits
     */
    private void extractMetadataFromSplits(List<Split> splits, Map<String, Integer> trafficTypesMap, Map<String, Set<String>> flagSetsMap) {
        if (splits == null || splits.isEmpty()) {
            return;
        }
        
        System.out.println(StartupTimeTracker.getElapsedTimeLog("SqLitePersistentSplitsStorage: Extracting metadata from splits"));
        long startTime = System.currentTimeMillis();
        
        // Pre-allocate collections for better performance
        if (trafficTypesMap.isEmpty()) {
            trafficTypesMap = new HashMap<>((int)(splits.size() * 0.75));
        }
        if (flagSetsMap.isEmpty()) {
            flagSetsMap = new HashMap<>((int)(splits.size() * 0.25));
        }
        
        for (Split split : splits) {
            // Process traffic type
            if (split.trafficTypeName != null) {
                String ttLower = split.trafficTypeName.toLowerCase();
                Integer count = trafficTypesMap.get(ttLower);
                trafficTypesMap.put(ttLower, (count == null) ? 1 : count + 1);
            }
            
            // Process flag sets
            if (split.sets != null && !split.sets.isEmpty()) {
                for (String set : split.sets) {
                    Set<String> splitNames = flagSetsMap.get(set);
                    if (splitNames == null) {
                        splitNames = new HashSet<>();
                        flagSetsMap.put(set, splitNames);
                    }
                    splitNames.add(split.name);
                }
            }
        }
        
        // Store extracted metadata for future use
        updateTrafficTypesAndFlagSets(trafficTypesMap, flagSetsMap);
        
        System.out.println(StartupTimeTracker.getElapsedTimeLog("SqLitePersistentSplitsStorage: Extracted metadata in " + 
                (System.currentTimeMillis() - startTime) + "ms, found " + 
                trafficTypesMap.size() + " traffic types and " + flagSetsMap.size() + " flag sets"));
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
