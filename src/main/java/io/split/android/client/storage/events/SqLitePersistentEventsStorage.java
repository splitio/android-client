package io.split.android.client.storage.events;

import androidx.annotation.NonNull;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.Event;
import io.split.android.client.storage.db.EventDao;
import io.split.android.client.storage.db.EventEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageRecordStatus;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;
import io.split.android.client.utils.StringHelper;

import static com.google.common.base.Preconditions.checkNotNull;

public class SqLitePersistentEventsStorage implements PersistentEventsStorage {

    private static final Type MY_SEGMENTS_LIST_TYPE = new TypeToken<List<String>>() {
    }.getType();
    final SplitRoomDatabase mDatabase;
    final StringHelper mStringHelper;
    final EventDao mEventDao;
    final long mExpirationPeriod;

    public SqLitePersistentEventsStorage(@NonNull SplitRoomDatabase database, long expirationPeriod) {
        mDatabase = checkNotNull(database);
        mStringHelper = new StringHelper();
        mEventDao = mDatabase.eventDao();
        mExpirationPeriod = expirationPeriod;
    }

    @Override
    public void push(@NonNull Event event) {
        if(event == null) {
            return;
        }
        EventEntity entity = new EventEntity();
        entity.setStatus(StorageRecordStatus.ACTIVE);
        entity.setBody(Json.toJson(event));
        entity.setCreatedAt(System.currentTimeMillis() / 1000);
        mEventDao.insert(entity);
    }

    @Override
    public List<Event> pop(int count) {

        List<EventEntity> entities = new ArrayList<>();
        mDatabase.runInTransaction(
            new GetAndUpdateTransaction(mEventDao, entities, count, mExpirationPeriod)
        );
        return entitiesToEvents(entities);
    }

    @Override
    public List<Event> getCritical() {
        return new ArrayList<>();
    }

    private List<Event> entitiesToEvents(List<EventEntity> entities) {
        List<Event> events = new ArrayList<>();
        for(EventEntity entity : entities) {
            try {
                events.add(Json.fromJson(entity.getBody(), Event.class));
            } catch (JsonSyntaxException e) {
                Logger.e("Unable to parse event entity: " +
                        entity.getBody() + " Error: " + e.getLocalizedMessage());

                continue;
            }
        }
        return events;
    }

    static final class GetAndUpdateTransaction implements Runnable {
        EventDao mEventDao;
        int mCount;
        List<EventEntity> mEntities;
        long mExpirationPeriod;

        GetAndUpdateTransaction(EventDao eventDao,
                                List<EventEntity> entities,
                                int count,
                                long expirationPeriod) {
            mEntities = checkNotNull(entities);
            mEventDao = checkNotNull(eventDao);
            mCount = count;
            mExpirationPeriod = expirationPeriod;
        }

        public void run() {
            long timestamp = System.currentTimeMillis() / 1000 - mExpirationPeriod;
            mEntities.addAll(mEventDao.getBy(timestamp,
                    StorageRecordStatus.ACTIVE, mCount));
            List<Long> ids = getEntitiesId(mEntities);
            mEventDao.updateStatus(ids, StorageRecordStatus.DELETED);
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
}