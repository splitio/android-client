package io.split.android.client.storage.impressions;

import androidx.annotation.NonNull;

import com.google.gson.JsonParseException;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import io.split.android.client.service.impressions.ImpressionsCountPerFeature;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.common.SqLitePersistentStorage;
import io.split.android.client.storage.db.ImpressionsCountDao;
import io.split.android.client.storage.db.ImpressionsCountEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageRecordStatus;
import io.split.android.client.utils.Json;

import static com.google.common.base.Preconditions.checkNotNull;

public class SqLitePersistentImpressionsCountStorage
        extends SqLitePersistentStorage<ImpressionsCountEntity, ImpressionsCountPerFeature>
        implements PersistentImpressionsCountStorage {

    private final SplitRoomDatabase mDatabase;
    private final ImpressionsCountDao mDao;
    private final SplitCipher mSplitCipher;

    public SqLitePersistentImpressionsCountStorage(@NonNull SplitRoomDatabase database,
                                                   long expirationPeriod,
                                                   @NonNull SplitCipher splitCipher) {
        super(expirationPeriod);
        mDatabase = checkNotNull(database);
        mDao = mDatabase.impressionsCountDao();
        mSplitCipher = checkNotNull(splitCipher);
    }

    @Override
    protected void insert(@NonNull ImpressionsCountEntity entity) {
        mDao.insert(entity);
    }

    @Override
    protected void insert(@NonNull List<ImpressionsCountEntity> entities) {
        mDao.insert(entities);
    }

    @NonNull
    @NotNull
    @Override
    protected ImpressionsCountEntity entityForModel(@NonNull ImpressionsCountPerFeature model) {
        ImpressionsCountEntity entity = new ImpressionsCountEntity();
        String body = mSplitCipher.encrypt(Json.toJson(model));
        if (body != null) {
            entity.setBody(body);
            entity.setStatus(StorageRecordStatus.ACTIVE);
            entity.setCreatedAt(System.currentTimeMillis() / 1000);
        }
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
    protected void updateStatus(@NonNull @NotNull List<Long> ids, int status) {
        mDao.updateStatus(ids, status);
    }

    @Override
    protected void runInTransaction(List<ImpressionsCountEntity> entities, int finalCount, long expirationPeriod) {
        mDatabase.runInTransaction(new GetAndUpdate(mDao, entities, finalCount, expirationPeriod));
    }

    @Override
    protected ImpressionsCountPerFeature entityToModel(ImpressionsCountEntity entity) throws JsonParseException {
        ImpressionsCountPerFeature count = Json.fromJson(mSplitCipher.decrypt(entity.getBody()),
                ImpressionsCountPerFeature.class);
        count.storageId = entity.getId();
        return count;
    }

    static class GetAndUpdate extends
            SqLitePersistentStorage.GetAndUpdateTransaction<ImpressionsCountEntity,
                    ImpressionsCountPerFeature> {

        final ImpressionsCountDao mDao;

        public GetAndUpdate(ImpressionsCountDao dao, List<ImpressionsCountEntity> entities, int count, long expirationPeriod) {
            super(entities, count, expirationPeriod);
            mDao = dao;
        }

        @Override
        protected List<ImpressionsCountEntity> getBy(long timestamp, int status, int rowCount) {
            return mDao.getBy(timestamp, status, rowCount);
        }

        @Override
        protected void updateStatus(List<Long> ids, int status) {
            mDao.updateStatus(ids, status);
        }
    }
}
