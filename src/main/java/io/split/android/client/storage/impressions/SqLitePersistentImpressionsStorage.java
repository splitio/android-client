package io.split.android.client.storage.impressions;

import androidx.annotation.NonNull;

import com.google.common.collect.Lists;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.storage.db.EventEntity;
import io.split.android.client.storage.db.ImpressionDao;
import io.split.android.client.storage.db.ImpressionEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageRecordStatus;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class SqLitePersistentImpressionsStorage implements PersistentImpressionsStorage {

    final SplitRoomDatabase mDatabase;
    final ImpressionDao mImpressionDao;
    final long mExpirationPeriod;
    private static  final int MAX_ROWS_PER_QUERY = ServiceConstants.MAX_ROWS_PER_QUERY;

    public SqLitePersistentImpressionsStorage(@NonNull SplitRoomDatabase database, long expirationPeriod) {
        mDatabase = checkNotNull(database);
        mImpressionDao = mDatabase.impressionDao();
        mExpirationPeriod = expirationPeriod;
    }

    @Override
    public void push(@NonNull KeyImpression impression) {
        if (impression == null) {
            return;
        }
        mImpressionDao.insert(entityForImpression(impression));
    }

    @Override
    public void pushMany(@NonNull List<KeyImpression> impressions) {
        if (impressions == null || impressions.size() == 0) {
            return;
        }
        List<ImpressionEntity> entities = new ArrayList<>();
        for (KeyImpression keyImpression : impressions) {
            entities.add(entityForImpression(keyImpression));
        }
        mImpressionDao.insert(entities);
    }

    @Override
    public List<KeyImpression> pop(int count) {
        List<ImpressionEntity> entities = new ArrayList<>();
        int lastSize = -1;
        int rowCount = count;
        do {
            int finalCount = MAX_ROWS_PER_QUERY <= rowCount ? MAX_ROWS_PER_QUERY : rowCount;
            List<ImpressionEntity> newEntityChunk = new ArrayList<>();
            mDatabase.runInTransaction(
                    new GetAndUpdateTransaction(mImpressionDao, newEntityChunk, finalCount, mExpirationPeriod)
            );
            lastSize = entities.size();
            rowCount -= lastSize;
            entities.addAll(newEntityChunk);
        }  while (lastSize > 0 && rowCount > 0);
        return entitiesToImpressions(entities);
    }

    @Override
    public void setActive(@NonNull List<KeyImpression> impressions) {
        checkNotNull(impressions);
        if (impressions.size() == 0) {
            return;
        }
        List<List<Long>> chunks = getImpressionsIdInChunks(impressions);
        for(List<Long> ids : chunks) {
            mImpressionDao.updateStatus(ids, StorageRecordStatus.ACTIVE);
        }
    }

    @Override
    public List<KeyImpression> getCritical() {
        return new ArrayList<>();
    }

    @Override
    public void delete(@NonNull List<KeyImpression> impressions) {
        checkNotNull(impressions);
        if (impressions.size() == 0) {
            return;
        }
        List<List<Long>> chunks = getImpressionsIdInChunks(impressions);
        for(List<Long> ids : chunks) {
            mImpressionDao.delete(ids);
        }
    }

    @Override
    public void deleteInvalid(long maxTimestamp) {
        int deleted = 1;
        while(deleted > 0) {
            deleted = mImpressionDao.deleteByStatus(
                    StorageRecordStatus.DELETED, maxTimestamp, MAX_ROWS_PER_QUERY);
        }
        mImpressionDao.deleteOutdated(expirationTime());
    }

    private long expirationTime() {
        return (System.currentTimeMillis() / 1000) - mExpirationPeriod;
    }

    private List<KeyImpression> entitiesToImpressions(List<ImpressionEntity> entities) {
        List<KeyImpression> impressions = new ArrayList<>();
        for (ImpressionEntity entity : entities) {
            try {
                KeyImpression impression = Json.fromJson(entity.getBody(), KeyImpression.class);
                impression.storageId = entity.getId();
                impressions.add(impression);
            } catch (JsonSyntaxException e) {
                Logger.e("Unable to parse impression entity: " +
                        entity.getBody() + " Error: " + e.getLocalizedMessage());

                continue;
            }
        }
        return impressions;
    }

    private ImpressionEntity entityForImpression(KeyImpression impression) {
        ImpressionEntity entity = new ImpressionEntity();
        entity.setStatus(StorageRecordStatus.ACTIVE);
        entity.setBody(Json.toJson(impression));
        entity.setTestName(impression.feature);
        entity.setCreatedAt(System.currentTimeMillis() / 1000);
        return entity;
    }

    private List<List<Long>> getImpressionsIdInChunks(List<KeyImpression> impressions) {
        List<Long> ids = new ArrayList<>();
        if (impressions == null) {
            return new ArrayList<>();
        }
        for (KeyImpression impression : impressions) {
            ids.add(impression.storageId);
        }
        return Lists.partition(ids, MAX_ROWS_PER_QUERY);
    }

    static final class GetAndUpdateTransaction implements Runnable {
        ImpressionDao mImpressionDao;
        int mCount;
        List<ImpressionEntity> mEntities;
        long mExpirationPeriod;

        GetAndUpdateTransaction(ImpressionDao eventDao,
                                List<ImpressionEntity> entities,
                                int count,
                                long expirationPeriod) {
            mEntities = checkNotNull(entities);
            mImpressionDao = checkNotNull(eventDao);
            mCount = count;
            mExpirationPeriod = expirationPeriod;
        }

        public void run() {
            long timestamp = System.currentTimeMillis() / 1000 - mExpirationPeriod;
            mEntities.addAll(mImpressionDao.getBy(timestamp,
                    StorageRecordStatus.ACTIVE, mCount));
            List<Long> ids = getEntitiesId(mEntities);
            mImpressionDao.updateStatus(ids, StorageRecordStatus.DELETED);
        }

        private List<Long> getEntitiesId(List<ImpressionEntity> entities) {
            List<Long> ids = new ArrayList<>();
            if (entities == null) {
                return ids;
            }
            for (ImpressionEntity entity : entities) {
                ids.add(entity.getId());
            }
            return ids;
        }
    }
}