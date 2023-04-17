package io.split.android.client.storage.impressions;

import androidx.annotation.NonNull;
import com.google.gson.JsonParseException;

import java.util.List;

import io.split.android.client.dtos.DeprecatedKeyImpression;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.common.SqLitePersistentStorage;
import io.split.android.client.storage.db.ImpressionDao;
import io.split.android.client.storage.db.ImpressionEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageRecordStatus;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class SqLitePersistentImpressionsStorage
        extends SqLitePersistentStorage<ImpressionEntity, KeyImpression>
        implements PersistentImpressionsStorage {

    private final SplitRoomDatabase mDatabase;
    private final ImpressionDao mDao;
    private final SplitCipher mSplitCipher;

    public SqLitePersistentImpressionsStorage(@NonNull SplitRoomDatabase database,
                                              long expirationPeriod,
                                              @NonNull SplitCipher splitCipher) {
        super(expirationPeriod);
        mDatabase = checkNotNull(database);
        mDao = mDatabase.impressionDao();
        mSplitCipher = checkNotNull(splitCipher);
    }

    @Override
    protected void insert(@NonNull ImpressionEntity entity) {
        mDao.insert(entity);
    }

    @Override
    protected void insert(@NonNull List<ImpressionEntity> entities) {
        mDao.insert(entities);
    }

    @Override
    protected ImpressionEntity entityForModel(@NonNull KeyImpression model) {
        ImpressionEntity entity = new ImpressionEntity();
        try {
            String body = Json.toJson(model);
            String encryptedBody = mSplitCipher.encrypt(body);
            if (encryptedBody == null) {
                Logger.e("Error encrypting impression");
                return null;
            }
            entity.setStatus(StorageRecordStatus.ACTIVE);
            entity.setBody(encryptedBody);
            entity.setTestName(model.feature);
            entity.setCreatedAt(System.currentTimeMillis() / 1000);

            return entity;
        } catch (JsonParseException e) {
            Logger.e("Error parsing impression: " + e.getMessage());

            return null;
        }
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
            String encryptedBody = entity.getBody();
            String body = mSplitCipher.decrypt(encryptedBody);
            if (body != null) {
                impression = Json.fromJson(body, KeyImpression.class);
                impression.feature = entity.getTestName();
            }
        } catch (JsonParseException e) {
            // Try deprecated serialization
            String encryptedBody = entity.getBody();
            String body = mSplitCipher.decrypt(encryptedBody);
            DeprecatedKeyImpression deprecatedImp = Json.fromJson(body,
                    DeprecatedKeyImpression.class);
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
