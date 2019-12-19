package io.split.android.client.storage.impressions;

import androidx.annotation.NonNull;

import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.impressions.Impression;
import io.split.android.client.storage.db.EventDao;
import io.split.android.client.storage.db.EventEntity;
import io.split.android.client.storage.db.ImpressionDao;
import io.split.android.client.storage.db.ImpressionEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageRecordStatus;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;
import io.split.android.client.utils.StringHelper;

import static com.google.common.base.Preconditions.checkNotNull;

public class SqLitePersistentImpressionsStorage implements PersistentImpressionsStorage {

    final SplitRoomDatabase mDatabase;
    final StringHelper mStringHelper;
    final ImpressionDao mImpressionDao;
    final long mExpirationPeriod;

    public SqLitePersistentImpressionsStorage(@NonNull SplitRoomDatabase database, long expirationPeriod) {
        mDatabase = checkNotNull(database);
        mStringHelper = new StringHelper();
        mImpressionDao = mDatabase.impressionDao();
        mExpirationPeriod = expirationPeriod;
    }

    @Override
    public void push(@NonNull KeyImpression impression) {
        if(impression == null) {
            return;
        }
        ImpressionEntity entity = new ImpressionEntity();
        entity.setStatus(StorageRecordStatus.ACTIVE);
        entity.setBody(Json.toJson(impression));
        entity.setCreatedAt(System.currentTimeMillis() / 1000);
        mImpressionDao.insert(entity);
    }

    @Override
    public void pushMany(@NonNull List<KeyImpression> impressions) {
        if(impressions == null) {
            return;
        }
        List<ImpressionEntity> entities = new ArrayList<>();
        for(KeyImpression keyImpression : impressions) {
            ImpressionEntity entity = new ImpressionEntity();
            entity.setStatus(StorageRecordStatus.ACTIVE);
            entity.setBody(Json.toJson(keyImpression));
            entity.setCreatedAt(System.currentTimeMillis() / 1000);
            entities.add(entity);
        }
        mImpressionDao.insert(entities);
    }

    @Override
    public List<KeyImpression> pop(int count) {

        List<ImpressionEntity> entities = new ArrayList<>();
        mDatabase.runInTransaction(
            new GetAndUpdateTransaction(mImpressionDao, entities, count, mExpirationPeriod)
        );
        return entitiesToImpressions(entities);
    }

    @Override
    public List<KeyImpression> getCritical() {
        return new ArrayList<>();
    }

    private List<KeyImpression> entitiesToImpressions(List<ImpressionEntity> entities) {
        List<KeyImpression> impressions = new ArrayList<>();
        for(ImpressionEntity entity : entities) {
            try {
                impressions.add(Json.fromJson(entity.getBody(), KeyImpression.class));
            } catch (JsonSyntaxException e) {
                Logger.e("Unable to parse impression entity: " +
                        entity.getBody() + " Error: " + e.getLocalizedMessage());

                continue;
            }
        }
        return impressions;
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
            for(ImpressionEntity entity : entities) {
                ids.add(entity.getId());
            }
            return ids;
        }
    }
}