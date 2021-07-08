package io.split.android.client.storage.events;

import androidx.annotation.NonNull;

import com.google.gson.JsonParseException;

import java.util.List;

import io.split.android.client.dtos.Event;
import io.split.android.client.storage.db.EventDao;
import io.split.android.client.storage.db.EventEntity;
import io.split.android.client.storage.db.ImpressionEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageRecordStatus;
import io.split.android.client.storage.SqLitePersistentStorage;
import io.split.android.client.utils.Json;

import static com.google.common.base.Preconditions.checkNotNull;

public class SqLitePersistentEventsStorage
        extends SqLitePersistentStorage<EventEntity, Event>
        implements PersistentEventsStorage {

    final SplitRoomDatabase mDatabase;
    final EventDao mDao;

    public SqLitePersistentEventsStorage(@NonNull SplitRoomDatabase database, long expirationPeriod) {
        super(expirationPeriod);
        mDatabase = checkNotNull(database);
        mDao = mDatabase.eventDao();
    }

    @Override
    protected void insert(@NonNull EventEntity entity) {
        mDao.insert(entity);
    }

    @Override
    protected void insert(@NonNull List<EventEntity> entities) {
        // No need to implement this for now
    }

    @NonNull
    @Override
    protected EventEntity entityForModel(@NonNull Event model) {
        EventEntity entity = new EventEntity();
        entity.setStatus(StorageRecordStatus.ACTIVE);
        entity.setBody(Json.toJson(model));
        entity.setCreatedAt(System.currentTimeMillis() / 1000);
        return entity;
    }

    @Override
    protected int deleteByStatus(int status, long maxTimestamp) {
        return mDao.deleteByStatus(status, maxTimestamp, MAX_ROWS_PER_QUERY);
    }

    @Override
    protected void deleteOutdated(long expirationTime) {
        mDao.deleteOutdated(expirationTime);
    }

    @Override
    protected void deleteById(@NonNull List<Long> ids) {
        mDao.delete(ids);
    }

    @Override
    protected void updateStatus(@NonNull List<Long> ids, int status) {
        mDao.updateStatus(ids, status);
    }

    @Override
    protected void runInTransaction(List<EventEntity> entities, int finalCount, long expirationPeriod) {
        mDatabase.runInTransaction(new io.split.android.client.storage.events.SqLitePersistentEventsStorage.GetAndUpdate(mDao, entities, finalCount, expirationPeriod));
    }

    @Override
    protected Event entityToModel(EventEntity entity) throws JsonParseException {
        Event count = Json.fromJson(entity.getBody(), Event.class);
        count.storageId = entity.getId();
        return count;
    }

    static class GetAndUpdate extends
            SqLitePersistentStorage.GetAndUpdateTransaction<EventEntity, Event> {

        final EventDao mDao;

        GetAndUpdate(EventDao dao, List<EventEntity> entities, int count, long expirationPeriod) {
            super(entities, count, expirationPeriod);
            mDao = dao;
        }

        @Override
        protected List<EventEntity> getBy(long timestamp, int status, int rowCount) {
            return mDao.getBy(timestamp, status, rowCount);
        }

        @Override
        protected void updateStatus(List<Long> ids, int status) {
            mDao.updateStatus(ids, status);
        }
    }
}