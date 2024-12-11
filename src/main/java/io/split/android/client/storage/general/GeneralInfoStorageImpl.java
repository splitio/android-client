package io.split.android.client.storage.general;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.split.android.client.storage.db.GeneralInfoDao;
import io.split.android.client.storage.db.GeneralInfoEntity;

public class GeneralInfoStorageImpl implements GeneralInfoStorage{

    private static final String ROLLOUT_CACHE_LAST_CLEAR_TIMESTAMP = "rolloutCacheLastClearTimestamp";

    private final GeneralInfoDao mGeneralInfoDao;

    public GeneralInfoStorageImpl(GeneralInfoDao generalInfoDao) {
        mGeneralInfoDao = checkNotNull(generalInfoDao);
    }

    @Override
    public long getSplitsUpdateTimestamp() {
        GeneralInfoEntity entity = mGeneralInfoDao.getByName(GeneralInfoEntity.SPLITS_UPDATE_TIMESTAMP);
        return entity != null ? entity.getLongValue() : 0L;
    }

    @Override
    public void setSplitsUpdateTimestamp(long timestamp) {
        mGeneralInfoDao.update(new GeneralInfoEntity(GeneralInfoEntity.SPLITS_UPDATE_TIMESTAMP, timestamp));
    }

    @Override
    public long getChangeNumber() {
        GeneralInfoEntity entity = mGeneralInfoDao.getByName(GeneralInfoEntity.CHANGE_NUMBER_INFO);
        return entity != null ? entity.getLongValue() : -1L;
    }

    @Override
    public void setChangeNumber(long changeNumber) {
        mGeneralInfoDao.update(new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, changeNumber));
    }

    @Override
    @NonNull
    public String getSplitsFilterQueryString() {
        GeneralInfoEntity entity = mGeneralInfoDao.getByName(GeneralInfoEntity.SPLITS_FILTER_QUERY_STRING);
        return entity != null ? entity.getStringValue() : "";
    }

    @Override
    public void setSplitsFilterQueryString(String queryString) {
        mGeneralInfoDao.update(new GeneralInfoEntity(GeneralInfoEntity.SPLITS_FILTER_QUERY_STRING, queryString));
    }

    @Override
    public String getDatabaseEncryptionMode() {
        GeneralInfoEntity entity = mGeneralInfoDao.getByName(GeneralInfoEntity.DATABASE_ENCRYPTION_MODE);
        return entity != null ? entity.getStringValue() : "";
    }

    @Override
    public void setDatabaseEncryptionMode(String value) {
        mGeneralInfoDao.update(new GeneralInfoEntity(GeneralInfoEntity.DATABASE_ENCRYPTION_MODE, value));
    }

    @Override
    @Nullable
    public String getFlagsSpec() {
        GeneralInfoEntity entity = mGeneralInfoDao.getByName(GeneralInfoEntity.FLAGS_SPEC);
        return entity != null ? entity.getStringValue() : "";
    }

    @Override
    public void setFlagsSpec(String value) {
        mGeneralInfoDao.update(new GeneralInfoEntity(GeneralInfoEntity.FLAGS_SPEC, value));
    }

    @Override
    public long getRolloutCacheLastClearTimestamp() {
        GeneralInfoEntity entity = mGeneralInfoDao.getByName(ROLLOUT_CACHE_LAST_CLEAR_TIMESTAMP);
        return entity != null ? entity.getLongValue() : 0L;
    }

    @Override
    public void setRolloutCacheLastClearTimestamp(long timestamp) {
        mGeneralInfoDao.update(new GeneralInfoEntity(ROLLOUT_CACHE_LAST_CLEAR_TIMESTAMP, timestamp));
    }
}
