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

public class RoomSqLitePersistentSplitsStorage implements PersistentSplitsStorage {

    SplitRoomDatabase mDatabase;

    public RoomSqLitePersistentSplitsStorage(SplitRoomDatabase database) {
        mDatabase = database;
    }

    @Override
    public boolean update(@NonNull List<Split> splits, long changeNumber) {

        if(splits == null) {
            return false;
        }
        List<String> removedSplits = new ArrayList<>();
        List<Split> newOrUpdatedSplits = new ArrayList<>();

        for (Split split : splits) {
            if(split.name == null) {
                continue;
            }
            if(split.status == Status.ACTIVE) {
                newOrUpdatedSplits.add(split);
            } else {
                removedSplits.add(split.name);
            }
        }
        List<SplitEntity> splitEntities = convertSplitListToEntities(newOrUpdatedSplits);

        mDatabase.runInTransaction(new Runnable() {
            @Override
            public void run() {
                mDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, changeNumber));
                mDatabase.splitDao().insert(splitEntities);
                mDatabase.splitDao().delete(removedSplits);
            }
        });

        return true;
    }

    @Override
    public Pair<List<Split>, Long> getSnapshot() {
        Long changeNumber = -1L;
        GeneralInfoEntity info = mDatabase.generalInfoDao().getByName(GeneralInfoEntity.CHANGE_NUMBER_INFO);
        if(info != null) {
            changeNumber = info.getLongValue();
        }
        List<Split> splits = convertEntitiesToSplitList(mDatabase.splitDao().getAll());

        return new Pair(splits, changeNumber);
    }

    @Override
    public void close() {
    }

    private List<SplitEntity> convertSplitListToEntities(List<Split> splits) {
        List<SplitEntity> splitEntities = new ArrayList<>();
        for(Split split : splits) {
            SplitEntity entity = new SplitEntity();
            entity.setName(split.name);
            entity.setBody(Json.toJson(split));
            entity.setUpdatedAt(split.changeNumber);
            splitEntities.add(entity);
        }
        return splitEntities;
    }

    private List<Split> convertEntitiesToSplitList(List<SplitEntity> entities) {
        List<Split> splits = new ArrayList<>();
        for(SplitEntity entity : entities) {
            try {
                splits.add(Json.fromJson(entity.getBody(), Split.class));
            } catch (JsonSyntaxException e) {
                Logger.e("Could not parse entity to split: " + entity.getName());
            }
        }
        return splits;
    }

}
