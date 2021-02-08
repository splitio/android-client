package io.split.android.client.storage.events;

import androidx.annotation.NonNull;

import com.google.common.collect.Lists;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.storage.db.EventDao;
import io.split.android.client.storage.db.EventEntity;
import io.split.android.client.storage.db.ImpressionEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageRecordStatus;
import io.split.android.client.storage.impressions.SqLitePersistentImpressionsStorage;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class SqLitePersistentEventsStorage implements PersistentEventsStorage {

    final SplitRoomDatabase mDatabase;
    final EventDao mEventDao;
    final long mExpirationPeriod;
    private static  final int MAX_ROWS_PER_QUERY = ServiceConstants.MAX_ROWS_PER_QUERY;

    public SqLitePersistentEventsStorage(@NonNull SplitRoomDatabase database, long expirationPeriod) {
        mDatabase = checkNotNull(database);

        mEventDao = mDatabase.eventDao();
        mExpirationPeriod = expirationPeriod;
    }

    @Override
    public void push(@NonNull Event event) {
        if (event == null) {
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
        int lastSize = -1;

        while (lastSize != entities.size() && entities.size() < count) {
            lastSize = entities.size();
            int pendingCount = count - lastSize;
            int finalCount = MAX_ROWS_PER_QUERY <= pendingCount ? MAX_ROWS_PER_QUERY : pendingCount;
            List<EventEntity> newEntityChunk = new ArrayList<>();
            mDatabase.runInTransaction(
                    new GetAndUpdateTransaction(mEventDao, newEntityChunk, finalCount, mExpirationPeriod)
            );
            entities.addAll(newEntityChunk);
        }
        return entitiesToEvents(entities);
    }

    @Override
    public void setActive(@NonNull List<Event> events) {
        checkNotNull(events);
        if (events.size() == 0) {
            return;
        }
        List<List<Long>> chunks = getEventsIdInChunks(events);
        for(List<Long> ids : chunks) {
            mEventDao.updateStatus(ids, StorageRecordStatus.ACTIVE);
        }
    }

    @Override
    public List<Event> getCritical() {
        return new ArrayList<>();
    }

    @Override
    public void delete(@NonNull List<Event> events) {
        checkNotNull(events);
        if (events.size() == 0) {
            return;
        }
        List<List<Long>> chunks = getEventsIdInChunks(events);
        for(List<Long> ids : chunks) {
            mEventDao.delete(ids);
        }
    }

    @Override
    public void deleteInvalid(long maxTimestamp) {
        int deleted = 1;
        while(deleted > 0) {
            deleted = mEventDao.deleteByStatus(
                    StorageRecordStatus.DELETED, maxTimestamp, MAX_ROWS_PER_QUERY);
        }
        mEventDao.deleteOutdated(expirationTime());
    }

    private long expirationTime() {
        return (System.currentTimeMillis() / 1000) - mExpirationPeriod;
    }
    private List<Event> entitiesToEvents(List<EventEntity> entities) {
        List<Event> events = new ArrayList<>();
        for (EventEntity entity : entities) {
            try {
                Event event = Json.fromJson(entity.getBody(), Event.class);
                event.storageId = entity.getId();
                events.add(event);
            } catch (JsonSyntaxException e) {
                Logger.e("Unable to parse event entity: " +
                        entity.getBody() + " Error: " + e.getLocalizedMessage());

                continue;
            }
        }
        return events;
    }

    private List<List<Long>> getEventsIdInChunks(List<Event> events) {
        List<Long> ids = new ArrayList<>();
        if (events == null) {
            return new ArrayList<>();
        }
        for (Event event : events) {
            ids.add(event.storageId);
        }
        return Lists.partition(ids, MAX_ROWS_PER_QUERY);
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
            for (EventEntity entity : entities) {
                ids.add(entity.getId());
            }
            return ids;
        }
    }
}