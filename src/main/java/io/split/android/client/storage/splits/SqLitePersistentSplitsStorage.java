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

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.Status;
import io.split.android.client.service.executor.parallel.SplitParallelTaskExecutorFactory;
import io.split.android.client.service.executor.parallel.SplitParallelTaskExecutorFactoryImpl;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;

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

        List<String> removedSplits = splitNameList(splitChange.getArchivedSplits());
        List<SplitEntity> splitEntities = convertSplitListToEntities(splitChange.getActiveSplits());

        mDatabase.runInTransaction(new Runnable() {
            @Override
            public void run() {
                mDatabase.generalInfoDao().update(
                        new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, splitChange.getChangeNumber()));
                if (!splitEntities.isEmpty()) {
                    mDatabase.splitDao().insert(splitEntities);
                }
                if (!removedSplits.isEmpty()) {
                    mDatabase.splitDao().delete(removedSplits);
                }
                if (!mTrafficTypes.isEmpty()) {
                    String encryptedTrafficTypes = mCipher.encrypt(Json.toJson(mTrafficTypes));
                    mDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.TRAFFIC_TYPES_MAP,
                            encryptedTrafficTypes));
                }
                if (!mFlagSets.isEmpty()) {
                    String encryptedFlagSets = mCipher.encrypt(Json.toJson(mFlagSets));
                    mDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.FLAG_SETS_MAP,
                            encryptedFlagSets));
                }
                mDatabase.generalInfoDao().update(
                        new GeneralInfoEntity(GeneralInfoEntity.SPLITS_UPDATE_TIMESTAMP, splitChange.getUpdateTimestamp()));
            }
        });

        return true;
    }

    @Override
    public SplitsSnapshot getSnapshot() {
        SplitsSnapshotLoader loader = new SplitsSnapshotLoader(mDatabase, loadSplits(), mCipher);
        loader.run();
        return new SplitsSnapshot(loader.getSplits(), loader.getChangeNumber(),
                loader.getUpdateTimestamp(), loader.getSplitsFilterQueryString(), loader.getFlagsSpec(),
                loader.getTrafficTypes(), loader.getFlagSets());
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
                mDatabase.getSplitQueryDao().invalidate();
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
        Map<String, SplitEntity> allNamesAndBodies = mDatabase.getSplitQueryDao().getAllAsMap();

        return mEntityToSplitTransformer.transform(allNamesAndBodies);
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
        private final SplitCipher mCipher;

        public SplitsSnapshotLoader(SplitRoomDatabase database, List<Split> splits, SplitCipher cipher) {
            mDatabase = database;
            mSplits = splits;
            mCipher = cipher;
        }

        @Override
        public void run() {
            GeneralInfoEntity timestampEntity = mDatabase.generalInfoDao().getByName(GeneralInfoEntity.SPLITS_UPDATE_TIMESTAMP);
            GeneralInfoEntity changeNumberEntity = mDatabase.generalInfoDao().getByName(GeneralInfoEntity.CHANGE_NUMBER_INFO);
            GeneralInfoEntity filterQueryStringEntity = mDatabase.generalInfoDao().getByName(GeneralInfoEntity.SPLITS_FILTER_QUERY_STRING);
            GeneralInfoEntity flagsSpecEntity = mDatabase.generalInfoDao().getByName(GeneralInfoEntity.FLAGS_SPEC);
            GeneralInfoEntity trafficTypesEntity = mDatabase.generalInfoDao().getByName(GeneralInfoEntity.TRAFFIC_TYPES_MAP);
            GeneralInfoEntity flagSetsEntity = mDatabase.generalInfoDao().getByName(GeneralInfoEntity.FLAG_SETS_MAP);

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

            boolean splitsAreNotEmpty = !mSplits.isEmpty();
            boolean trafficTypesEntityIsEmpty = trafficTypesEntity == null || trafficTypesEntity.getStringValue().isEmpty();
            boolean flagSetsEntityIsEmpty = flagSetsEntity == null || flagSetsEntity.getStringValue().isEmpty();
            boolean trafficTypesAndSetsMigrationRequired = splitsAreNotEmpty &&
                    (trafficTypesEntityIsEmpty || flagSetsEntityIsEmpty);
            if (trafficTypesAndSetsMigrationRequired) {
                migrateTrafficTypesAndSetsFromStoredData();
            }

            parseTrafficTypesAndSets(trafficTypesEntity, flagSetsEntity);
        }

        private synchronized void parseTrafficTypesAndSets(@Nullable GeneralInfoEntity trafficTypesEntity, @Nullable GeneralInfoEntity flagSetsEntity) {
            Logger.v("Parsing traffic types and sets");
            if (trafficTypesEntity != null && !trafficTypesEntity.getStringValue().isEmpty()) {
                Type mapType = new TypeToken<Map<String, Integer>>(){}.getType();
                String encryptedTrafficTypes = trafficTypesEntity.getStringValue();
                String decryptedTrafficTypes = mCipher.decrypt(encryptedTrafficTypes);
                mTrafficTypes = Json.fromJson(decryptedTrafficTypes, mapType);
            }

            if (flagSetsEntity != null && !flagSetsEntity.getStringValue().isEmpty()) {
                Type flagsMapType = new TypeToken<Map<String, Set<String>>>(){}.getType();
                String encryptedFlagSets = flagSetsEntity.getStringValue();
                String decryptedFlagSets = mCipher.decrypt(encryptedFlagSets);
                mFlagSets = Json.fromJson(decryptedFlagSets, flagsMapType);
            }
        }

        private void migrateTrafficTypesAndSetsFromStoredData() {
            Logger.i("Migration required for cached traffic types and flag sets. Migrating now.");
            try {
                for (Split split : mSplits) {
                    Split parsedSplit = Json.fromJson(split.json, Split.class);
                    if (parsedSplit != null) {
                        if (parsedSplit.status == Status.ACTIVE) {
                            increaseTrafficTypeCount(parsedSplit.trafficTypeName, mTrafficTypes);
                            addOrUpdateFlagSets(parsedSplit, mFlagSets);
                        } else {
                            decreaseTrafficTypeCount(parsedSplit.trafficTypeName, mTrafficTypes);
                            deleteFromFlagSetsIfNecessary(parsedSplit, mFlagSets);
                        }
                    }
                }

                // persist TTs
                if (mTrafficTypes != null && !mTrafficTypes.isEmpty()) {
                    String decryptedTrafficTypes = Json.toJson(mTrafficTypes);
                    String encryptedTrafficTypes = mCipher.encrypt(decryptedTrafficTypes);
                    mDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.TRAFFIC_TYPES_MAP, encryptedTrafficTypes));
                }

                if (mFlagSets != null && !mFlagSets.isEmpty()) {
                    // persist flag sets
                    String decryptedFlagSets = Json.toJson(mFlagSets);
                    String encryptedFlagSets = mCipher.encrypt(decryptedFlagSets);

                    mDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.FLAG_SETS_MAP, encryptedFlagSets));
                }
            } catch (Exception e) {
                Logger.e("Failed to migrate traffic types and flag sets", e);
            }
        }

        public List<Split> getSplits() {
            return mSplits;
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
    }
}
