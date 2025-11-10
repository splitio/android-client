package io.split.android.client.storage.impressions;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import com.google.gson.JsonParseException;

import java.util.List;
import java.util.Set;

import io.split.android.client.service.impressions.unique.UniqueKey;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.common.SqLitePersistentStorage;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageRecordStatus;
import io.split.android.client.storage.db.impressions.unique.UniqueKeyEntity;
import io.split.android.client.storage.db.impressions.unique.UniqueKeysDao;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;

public class SqlitePersistentUniqueStorage
        extends SqLitePersistentStorage<UniqueKeyEntity, UniqueKey>
        implements PersistentImpressionsUniqueStorage {

    private final SplitRoomDatabase mDatabase;
    private final UniqueKeysDao mDao;

    private final SplitCipher mSplitCipher;

    public SqlitePersistentUniqueStorage(SplitRoomDatabase database, long expirationPeriod,
                                         SplitCipher splitCipher) {
        super(expirationPeriod);
        mDatabase = checkNotNull(database);
        mDao = mDatabase.uniqueKeysDao();
        mSplitCipher = checkNotNull(splitCipher);
    }

    @Override
    protected void insert(@NonNull UniqueKeyEntity entity) {
        mDao.insert(entity);
    }

    @Override
    protected void insert(@NonNull List<UniqueKeyEntity> entities) {
        mDao.insert(entities);
    }

    @Override
    protected UniqueKeyEntity entityForModel(@NonNull UniqueKey model) {
        String key = mSplitCipher.encrypt(model.getKey());
        String featureList = mSplitCipher.encrypt(Json.toJson(model.getFeatures()));
        if (key == null || featureList == null) {
            Logger.e("Error encrypting unique key");
            return null;
        }

        return new UniqueKeyEntity(
                key,
                featureList,
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
        Set<String> features = Json.fromJson(mSplitCipher.decrypt(entity.getFeatureList()), Set.class);
        UniqueKey model = new UniqueKey(mSplitCipher.decrypt(entity.getUserKey()), features);
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
