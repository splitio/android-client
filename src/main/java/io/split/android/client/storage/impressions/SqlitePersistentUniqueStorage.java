package io.split.android.client.storage.impressions;

import androidx.annotation.NonNull;

import com.google.gson.JsonParseException;

import java.util.List;
import java.util.Set;

import io.split.android.client.service.impressions.unique.UniqueKey;
import io.split.android.client.storage.common.SqLitePersistentStorage;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageRecordStatus;
import io.split.android.client.storage.db.impressions.unique.UniqueKeyEntity;
import io.split.android.client.storage.db.impressions.unique.UniqueKeysDao;
import io.split.android.client.utils.Json;

public class SqlitePersistentUniqueStorage
        extends SqLitePersistentStorage<UniqueKeyEntity, UniqueKey>
        implements PersistentImpressionsUniqueStorage {

    private final SplitRoomDatabase mDatabase;
    private final UniqueKeysDao mDao;

    public SqlitePersistentUniqueStorage(SplitRoomDatabase database, long expirationPeriod) {
        super(expirationPeriod);
        mDatabase = database;
        mDao = mDatabase.uniqueKeysDao();
    }

    @Override
    protected void insert(@NonNull UniqueKeyEntity entity) {
        mDao.insert(entity);
    }

    @Override
    protected void insert(@NonNull List<UniqueKeyEntity> entities) {
        mDao.insert(entities);
    }

    @NonNull
    @Override
    protected UniqueKeyEntity entityForModel(@NonNull UniqueKey model) {
        return new UniqueKeyEntity(
                model.getKey(),
                Json.toJson(model.getFeatures()),
                System.currentTimeMillis() / 1000,
                StorageRecordStatus.ACTIVE
        );
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
        mDao.deleteById(ids);
    }

    @Override
    protected void updateStatus(@NonNull List<Long> ids, int status) {
        mDao.updateStatus(ids, status);
    }

    @Override
    protected void runInTransaction(List<UniqueKeyEntity> entities, int finalCount, long expirationPeriod) {
        mDatabase.runInTransaction(new GetAndUpdate(mDao, entities, finalCount, expirationPeriod));
    }

    @Override
    protected UniqueKey entityToModel(UniqueKeyEntity entity) throws JsonParseException {
        Set<String> features = Json.fromJson(entity.getFeatureList(), Set.class);
        UniqueKey model = new UniqueKey(entity.getUserKey(), features);
        model.setStorageId(entity.getId());

        return model;
    }

    static class GetAndUpdate extends
            SqLitePersistentStorage.GetAndUpdateTransaction<UniqueKeyEntity,
                    UniqueKey> {

        private final UniqueKeysDao mDao;

        public GetAndUpdate(UniqueKeysDao dao, List<UniqueKeyEntity> entities, int count, long expirationPeriod) {
            super(entities, count, expirationPeriod);
            mDao = dao;
        }

        @Override
        protected List<UniqueKeyEntity> getBy(long timestamp, int status, int rowCount) {
            return mDao.getBy(timestamp, status, rowCount);
        }

        @Override
        protected void updateStatus(List<Long> ids, int status) {
            mDao.updateStatus(ids, status);
        }
    }
}
