package io.split.android.client.service.synchronizer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import io.split.android.client.storage.db.GeneralInfoDao;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.utils.logger.Logger;

public class LastUpdateTimestampProviderImpl implements LastUpdateTimestampProvider {

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final AtomicLong mTimestamp = new AtomicLong(-1);
    private final Object mLock = new Object();
    private final GeneralInfoDao mGeneralInfoDao;

    public LastUpdateTimestampProviderImpl(GeneralInfoDao generalInfoDao) {
        mGeneralInfoDao = generalInfoDao;
        mExecutor.submit(() -> {
            synchronized (mLock) {
                Logger.e("Retrieving timestamp for initialization");
                mTimestamp.set(mGeneralInfoDao.getByName(GeneralInfoEntity.SPLITS_UPDATE_TIMESTAMP).getLongValue());
                Logger.e("Initialized timestamp to " + mTimestamp.get());
            }
        });
    }

    @Override
    public long getLastUpdateTimestamp() {
        synchronized (mLock) {
            return mTimestamp.get();
        }
    }

    @Override
    public void setLastUpdateTimestamp(long timestamp) {
        mTimestamp.set(timestamp);
        mExecutor.submit(() -> {
            synchronized (mLock) {
                mGeneralInfoDao.update(new GeneralInfoEntity(GeneralInfoEntity.SPLITS_UPDATE_TIMESTAMP, timestamp));
            }
        });
    }
}
