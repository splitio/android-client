package io.split.android.client.storage.events;

import androidx.annotation.NonNull;

import com.google.common.base.Strings;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import io.split.android.client.dtos.Event;
import io.split.android.client.storage.db.EventDao;
import io.split.android.client.storage.db.EventEntity;
import io.split.android.client.storage.db.MySegmentEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageRecordStatus;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.StringHelper;

import static com.google.common.base.Preconditions.checkNotNull;

public class SqLitePersistentEventsStorage implements PersistentEventsStorage {

    private static final Type MY_SEGMENTS_LIST_TYPE = new TypeToken<List<String>>() {
    }.getType();
    final SplitRoomDatabase mDatabase;
    final StringHelper mStringHelper;

    public SqLitePersistentEventsStorage(@NonNull SplitRoomDatabase database) {
        mDatabase = checkNotNull(database);
        mStringHelper = new StringHelper();
    }

    @Override
    public void push(@NonNull Event event) {
        if(event == null) {
            return;
        }
        EventEntity entity = new EventEntity();
        entity.setStatus(StorageRecordStatus.ACTIVE);
        entity.setBody(Json.toJson(event));
        entity.setUpdatedAt(System.currentTimeMillis() / 1000);
        mDatabase.eventDao().insert(entity);
    }

    @Override
    public List<Event> pop(int count) {
        EventDao eventDao = mDatabase.eventDao();
        mDatabase.runInTransaction(
                new Runnable() {
                    @Override
                    public void run() {
                        List<EventEntity> entities = eventDao.getBy(0000,
                                StorageRecordStatus.ACTIVE, count);
                        List<Long> ids = getEntitiesId(entities);
                        eventDao.updateStatus(ids, StorageRecordStatus.DELETED);
                    }
                }

        );
        return new ArrayList<>();
    }

    @Override
    public List<Event> getCritical() {
        return null;
    }


    private List<Long> getEntitiesId(List<EventEntity> entities) {
        List<Long> ids = new ArrayList<>();
        if (entities == null) {
            return ids;
        }
        for(EventEntity entity : entities) {
            ids.add(entity.getId());
        }
        return ids;
    }

}