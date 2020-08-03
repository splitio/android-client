package io.split.android.client.storage.splits;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.Split;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class SqLitePersistentSplitsStorage implements PersistentSplitsStorage {

    private static final int SQL_PARAM_BIND_SIZE = 20;
    SplitRoomDatabase mDatabase;

    public SqLitePersistentSplitsStorage(@NonNull SplitRoomDatabase database) {
        mDatabase = checkNotNull(database);
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
        return new SplitsSnapshot(
                convertEntitiesToSplitList(mDatabase.splitDao().getAll()), loader.getChangeNumber(),
                loader.getUpdateTimestamp(), loader.getSplitsFilterQueryString());
    }

    @Override
    public void update(Split split) {
        // Using this all.database method
        // to avoid introducing breaking changes in db schema
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
        List<List<String>> deleteChunk = Lists.partition(splitNames, SQL_PARAM_BIND_SIZE);
        for(List<String> splits : deleteChunk) {
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
        return convertEntitiesToSplitList(mDatabase.splitDao().getAll());
    }

    @Override
    @Nullable
    public String getFilterQueryString() {
        GeneralInfoEntity generalInfoEntity = mDatabase.generalInfoDao().getByName(GeneralInfoEntity.SPLITS_FILTER_QUERY_STRING);
        return generalInfoEntity != null ? generalInfoEntity.getStringValue() : null;
    }

    private List<SplitEntity> convertSplitListToEntities(List<Split> splits) {
        List<SplitEntity> splitEntities = new ArrayList<>();
        if (splits == null) {
            return splitEntities;
        }
        for (Split split : splits) {
            SplitEntity entity = new SplitEntity();
            entity.setName(split.name);
            entity.setBody(Json.toJson(split));
            entity.setUpdatedAt(System.currentTimeMillis() / 1000);
            splitEntities.add(entity);
        }
        return splitEntities;
    }

    private List<Split> convertEntitiesToSplitList(List<SplitEntity> entities) {
        List<Split> splits = new ArrayList<>();

        if (entities == null) {
            return splits;
        }

        for (SplitEntity entity : entities) {
            try {
                splits.add(Json.fromJson(entity.getBody(), Split.class));
            } catch (JsonSyntaxException e) {
                Logger.e("Could not parse entity to split: " + entity.getName());
            }
        }
        return splits;
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
        private List<SplitEntity> mSplitEntities;

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

        public List<SplitEntity> getSplitEntities() {
            return mSplitEntities;
        }
    }
}
