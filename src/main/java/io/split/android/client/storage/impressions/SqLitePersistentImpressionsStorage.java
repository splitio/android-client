package io.split.android.client.storage.impressions;

import androidx.annotation.NonNull;
import com.google.gson.JsonParseException;

import java.util.List;

import io.split.android.client.dtos.DeprecatedKeyImpression;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.storage.SqLitePersistentStorage;
import io.split.android.client.storage.db.ImpressionDao;
import io.split.android.client.storage.db.ImpressionEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageRecordStatus;
import io.split.android.client.utils.Json;

import static com.google.common.base.Preconditions.checkNotNull;

public class SqLitePersistentImpressionsStorage
        extends SqLitePersistentStorage<ImpressionEntity, KeyImpression>
        implements PersistentImpressionsStorage {

    final SplitRoomDatabase mDatabase;
    final ImpressionDao mDao;

    public SqLitePersistentImpressionsStorage(@NonNull SplitRoomDatabase database, long expirationPeriod) {
        super(expirationPeriod);
        mDatabase = checkNotNull(database);
        mDao = mDatabase.impressionDao();
    }

    @Override
    protected void insert(@NonNull ImpressionEntity entity) {
        mDao.insert(entity);
    }

    @Override
    protected void insert(@NonNull List<ImpressionEntity> entities) {
        mDao.insert(entities);
    }

    @NonNull
    @Override
    protected ImpressionEntity entityForModel(@NonNull KeyImpression model) {
        ImpressionEntity entity = new ImpressionEntity();
        entity.setStatus(StorageRecordStatus.ACTIVE);
        entity.setBody(Json.toJson(model));
        entity.setTestName(model.feature);
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
    protected void runInTransaction(List<ImpressionEntity> entities, int finalCount, long expirationPeriod) {
        mDatabase.runInTransaction(new GetAndUpdate(mDao, entities, finalCount, expirationPeriod));
    }

    @Override
    protected KeyImpression entityToModel(ImpressionEntity entity) throws JsonParseException {
        KeyImpression impression = null;
        try {
            impression = Json.fromJson(entity.getBody(), KeyImpression.class);
            impression.feature = entity.getTestName();
        } catch (JsonParseException e) {
            // Try deprecated serialization
            DeprecatedKeyImpression deprecatedImp = Json.fromJson(entity.getBody(), DeprecatedKeyImpression.class);
            impression = updateImpression(deprecatedImp);
        }
        if (impression == null) {
            throw new JsonParseException("Error parsing stored impression");
        }
        impression.storageId = entity.getId();
        return impression;
    }

    private KeyImpression updateImpression(DeprecatedKeyImpression deprecated) {
        KeyImpression impression = new KeyImpression();
        impression.feature = deprecated.feature;
        impression.bucketingKey = deprecated.bucketingKey;
        impression.changeNumber = deprecated.changeNumber;
        impression.keyName = deprecated.keyName;
        impression.label = deprecated.label;
        impression.time = deprecated.time;
        impression.treatment = deprecated.treatment;
        return impression;
    }
    static class GetAndUpdate extends
            SqLitePersistentStorage.GetAndUpdateTransaction<ImpressionEntity, KeyImpression> {

        final ImpressionDao mDao;

        public GetAndUpdate(ImpressionDao dao, List<ImpressionEntity> entities, int count, long expirationPeriod) {
            super(entities, count, expirationPeriod);
            mDao = dao;
        }

        @Override
        protected List<ImpressionEntity> getBy(long timestamp, int status, int rowCount) {
            return mDao.getBy(timestamp, status, rowCount);
        }

        @Override
        protected void updateStatus(List<Long> ids, int status) {
            mDao.updateStatus(ids, status);
        }
    }
}