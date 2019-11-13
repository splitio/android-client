package io.split.android.client.storage.splits;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.Status;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class SqLitePersistentSplitsStorage implements PersistentSplitsStorage {

    SplitRoomDatabase mDatabase;

    public SqLitePersistentSplitsStorage(@NonNull SplitRoomDatabase database) {
        checkNotNull(database);

        mDatabase = database;
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
                mDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, splitChange.getChangeNumber()));
                mDatabase.splitDao().insert(splitEntities);
                mDatabase.splitDao().delete(removedSplits);
            }
        });

        return true;
    }

    @Override
    public SplitsSnapshot getSnapshot() {
        Long changeNumber = -1L;
        GeneralInfoEntity info = mDatabase.generalInfoDao().getByName(GeneralInfoEntity.CHANGE_NUMBER_INFO);
        if (info != null) {
            changeNumber = info.getLongValue();
        }

        return new SplitsSnapshot(convertEntitiesToSplitList(mDatabase.splitDao().getAll()), changeNumber);
    }

    @Override
    public void close() {
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
}
