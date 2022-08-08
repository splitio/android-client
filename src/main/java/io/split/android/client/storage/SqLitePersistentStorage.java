package io.split.android.client.storage;

import androidx.annotation.NonNull;

import com.google.common.collect.Lists;
import com.google.gson.JsonParseException;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.Identifiable;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.storage.db.StorageRecordStatus;
import io.split.android.client.utils.logger.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class SqLitePersistentStorage<E extends Identifiable, M extends Identifiable> {

    protected long mExpirationPeriod;
    protected static final int MAX_ROWS_PER_QUERY = ServiceConstants.MAX_ROWS_PER_QUERY;

    public SqLitePersistentStorage(long expirationPeriod) {
        mExpirationPeriod = expirationPeriod;
    }

    public void push(@NonNull M model) {
        if (model == null) {
            return;
        }
        insert(entityForModel(model));
    }

    public void pushMany(@NonNull List<M> models) {
        if (models == null || models.size() == 0) {
            return;
        }
        List<E> entities = new ArrayList<>();
        for (M model : models) {
            entities.add(entityForModel(model));
        }
        insert(entities);
    }

    public List<M> pop(int count) {
        List<E> entities = new ArrayList<>();
        int lastSize = -1;
        int rowCount = count;
        do {
            int finalCount = Math.min(MAX_ROWS_PER_QUERY, rowCount);
            List<E> newEntityChunk = new ArrayList<>();
            runInTransaction(newEntityChunk, finalCount, mExpirationPeriod);
            lastSize = newEntityChunk.size();
            rowCount -= lastSize;
            entities.addAll(newEntityChunk);
        } while (lastSize > 0 && rowCount > 0);
        return entitiesToModels(entities);
    }

    public void setActive(@NonNull List<M> models) {
        checkNotNull(models);
        if (models.size() == 0) {
            return;
        }
        List<List<Long>> chunks = getIdInChunks(models);
        for (List<Long> ids : chunks) {
            updateStatus(ids, StorageRecordStatus.ACTIVE);
        }
    }

    public List<KeyImpression> getCritical() {
        return new ArrayList<>();
    }

    public void delete(@NonNull List<M> models) {
        checkNotNull(models);
        if (models.size() == 0) {
            return;
        }
        List<List<Long>> chunks = getIdInChunks(models);
        for (List<Long> ids : chunks) {
            deleteById(ids);
        }
    }

    public void deleteInvalid(long maxTimestamp) {
        int deleted = 1;
        while (deleted > 0) {
            deleted = deleteByStatus(
                    StorageRecordStatus.DELETED, maxTimestamp);
        }
        deleteOutdated(expirationTime());
    }

    private long expirationTime() {
        return (System.currentTimeMillis() / 1000) - mExpirationPeriod;
    }

    private List<M> entitiesToModels(List<E> entities) {
        List<M> models = new ArrayList<>();
        for (E entity : entities) {
            try {
                models.add(entityToModel(entity));
            } catch (JsonParseException e) {
                Logger.e("Error parsing stored entity: " + e.getLocalizedMessage());
            } catch (Exception e) {
                Logger.e("Unknown error parsing stored entity: " + e.getLocalizedMessage());
            }
        }
        return models;
    }

    private List<List<Long>> getIdInChunks(List<M> models) {
        List<Long> ids = new ArrayList<>();
        if (models == null) {
            return new ArrayList<>();
        }
        for (Identifiable model : models) {
            ids.add(model.getId());
        }
        return Lists.partition(ids, MAX_ROWS_PER_QUERY);
    }

    protected abstract void insert(@NonNull E entity);

    protected abstract void insert(@NonNull List<E> entities);

    protected abstract @NonNull
    E entityForModel(@NonNull M model);

    protected abstract int deleteByStatus(int status, long maxTimestamp);

    protected abstract void deleteOutdated(long expirationTime);

    protected abstract void deleteById(@NonNull List<Long> ids);

    protected abstract void updateStatus(@NonNull List<Long> ids, int status);

    protected abstract void runInTransaction(List<E> entities, int finalCount, long expirationPeriod);

    protected abstract M entityToModel(E entity) throws JsonParseException;

    public static abstract class GetAndUpdateTransaction<E extends Identifiable, M> implements Runnable {

        int mCount;
        List<E> mEntities;
        long mExpirationPeriod;

        public GetAndUpdateTransaction(List<E> entities,
                                int count,
                                long expirationPeriod) {
            mEntities = checkNotNull(entities);
            mCount = count;
            mExpirationPeriod = expirationPeriod;
        }

        public void run() {
            long timestamp = System.currentTimeMillis() / 1000 - mExpirationPeriod;
            mEntities.addAll(getBy(timestamp, StorageRecordStatus.ACTIVE, mCount));
            List<Long> ids = getEntitiesId(mEntities);
            updateStatus(ids, StorageRecordStatus.DELETED);
        }

        protected abstract List<E> getBy(long timestamp, int status, int rowCount);

        protected abstract void updateStatus(List<Long> ids, int status);

        private List<Long> getEntitiesId(List<E> entities) {
            List<Long> ids = new ArrayList<>();
            if (entities == null) {
                return ids;
            }
            for (Identifiable entity : entities) {
                ids.add(entity.getId());
            }
            return ids;
        }
    }

}