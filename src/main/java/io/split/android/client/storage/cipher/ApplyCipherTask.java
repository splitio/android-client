package io.split.android.client.storage.cipher;

import androidx.annotation.NonNull;

import java.util.List;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.db.EventDao;
import io.split.android.client.storage.db.EventEntity;
import io.split.android.client.storage.db.ImpressionDao;
import io.split.android.client.storage.db.ImpressionEntity;
import io.split.android.client.storage.db.ImpressionsCountDao;
import io.split.android.client.storage.db.ImpressionsCountEntity;
import io.split.android.client.storage.db.MyLargeSegmentDao;
import io.split.android.client.storage.db.MyLargeSegmentEntity;
import io.split.android.client.storage.db.MySegmentDao;
import io.split.android.client.storage.db.MySegmentEntity;
import io.split.android.client.storage.db.SegmentDao;
import io.split.android.client.storage.db.SegmentEntity;
import io.split.android.client.storage.db.SplitDao;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.attributes.AttributesDao;
import io.split.android.client.storage.db.attributes.AttributesEntity;
import io.split.android.client.storage.db.impressions.unique.UniqueKeyEntity;
import io.split.android.client.storage.db.impressions.unique.UniqueKeysDao;
import io.split.android.client.utils.logger.Logger;

public class ApplyCipherTask implements SplitTask {

    private final SplitRoomDatabase mSplitDatabase;
    private final SplitCipher mFromCipher;
    private final SplitCipher mToCipher;

    public ApplyCipherTask(SplitRoomDatabase splitDatabase,
                           SplitCipher fromCipher,
                           SplitCipher toCipher) {
        mSplitDatabase = splitDatabase;
        mFromCipher = fromCipher;
        mToCipher = toCipher;
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        try {
            mSplitDatabase.runInTransaction(new Runnable() {
                @Override
                public void run() {
                    updateSplits(mSplitDatabase.splitDao());
                    updateSegments(mSplitDatabase.mySegmentDao());
                    updateLargeSegments(mSplitDatabase.myLargeSegmentDao());
                    updateImpressions(mSplitDatabase.impressionDao());
                    updateEvents(mSplitDatabase.eventDao());
                    updateImpressionsCount(mSplitDatabase.impressionsCountDao());
                    updateUniqueKeys(mSplitDatabase.uniqueKeysDao());
                    updateAttributes(mSplitDatabase.attributesDao());
                }
            });

            return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
        } catch (Exception e) {
            return SplitTaskExecutionInfo.error(SplitTaskType.GENERIC_TASK);
        }
    }

    private void updateAttributes(AttributesDao attributesDao) {
        List<AttributesEntity> items = attributesDao.getAll();

        for (AttributesEntity item : items) {
            String userKey = item.getUserKey();
            String fromUserKey = mFromCipher.decrypt(userKey);
            String fromBody = mFromCipher.decrypt(item.getAttributes());

            String toUserKey = mToCipher.encrypt(fromUserKey);
            String toBody = mToCipher.encrypt(fromBody);

            if (toUserKey != null && toBody != null) {
                attributesDao.update(userKey, toUserKey, toBody);
            } else {
                Logger.e("Error applying cipher to attributes storage");
            }
        }
    }

    private void updateUniqueKeys(UniqueKeysDao uniqueKeysDao) {
        List<UniqueKeyEntity> items = uniqueKeysDao.getAll();

        for (UniqueKeyEntity item : items) {
            String fromUserKey = mFromCipher.decrypt(item.getUserKey());
            String fromFeatureList = mFromCipher.decrypt(item.getFeatureList());

            String toUserKey = mToCipher.encrypt(fromUserKey);
            String toFeatureList = mToCipher.encrypt(fromFeatureList);

            if (toFeatureList != null) {
                item.setUserKey(toUserKey);
                item.setFeatureList(toFeatureList);
                uniqueKeysDao.insert(item);
            } else {
                Logger.e("Error applying cipher to unique keys storage");
            }
        }
    }

    private void updateImpressionsCount(ImpressionsCountDao impressionsCountDao) {
        List<ImpressionsCountEntity> items = impressionsCountDao.getAll();

        for (ImpressionsCountEntity item : items) {
            String fromBody = mFromCipher.decrypt(item.getBody());

            String toBody = mToCipher.encrypt(fromBody);

            if (toBody != null) {
                item.setBody(toBody);
                impressionsCountDao.insert(item);
            } else {
                Logger.e("Error applying cipher to impression count storage");
            }
        }
    }

    private void updateImpressions(ImpressionDao impressionDao) {
        List<ImpressionEntity> items = impressionDao.getAll();

        for (ImpressionEntity item : items) {
            String fromName = mFromCipher.decrypt(item.getTestName());
            String fromBody = mFromCipher.decrypt(item.getBody());

            String toName = mToCipher.encrypt(fromName);
            String toBody = mToCipher.encrypt(fromBody);

            if (toName != null && toBody != null) {
                item.setTestName(toName);
                item.setBody(toBody);
                impressionDao.insert(item);
            } else {
                Logger.e("Error applying cipher to impression storage");
            }
        }
    }

    private void updateSegments(MySegmentDao mySegmentDao) {
        List<MySegmentEntity> items = mySegmentDao.getAll();

        updateSegments(mySegmentDao, items);
    }

    private void updateLargeSegments(MyLargeSegmentDao myLargeSegmentDao) {
        List<MyLargeSegmentEntity> items = myLargeSegmentDao.getAll();

        updateSegments(myLargeSegmentDao, items);
    }

    private void updateSegments(SegmentDao<? extends SegmentEntity> mySegmentDao, List<? extends SegmentEntity> items) {
        for (SegmentEntity item : items) {
            String userKey = item.getUserKey();
            String fromUserKey = mFromCipher.decrypt(userKey);
            String fromBody = mFromCipher.decrypt(item.getSegmentList());

            String toUserKey = mToCipher.encrypt(fromUserKey);
            String toBody = mToCipher.encrypt(fromBody);

            if (toUserKey != null && toBody != null) {
                mySegmentDao.update(userKey, toUserKey, toBody);
            } else {
                Logger.e("Error applying cipher to my " + (item instanceof MyLargeSegmentEntity ? "large" : "") + " segment");
            }
        }
    }

    private void updateEvents(EventDao eventDao) {
        List<EventEntity> items = eventDao.getAll();

        for (EventEntity item : items) {
            String body = mFromCipher.decrypt(item.getBody());

            String decryptedBody = mToCipher.encrypt(body);

            if (decryptedBody != null) {
                item.setBody(decryptedBody);
                eventDao.insert(item);
            } else {
                Logger.e("Error applying cipher to event");
            }
        }
    }

    private void updateSplits(SplitDao dao) {
        List<SplitEntity> items = dao.getAll();

        for (SplitEntity item : items) {
            String name = item.getName();
            String fromName = mFromCipher.decrypt(name);
            String fromBody = mFromCipher.decrypt(item.getBody());

            String toName = mToCipher.encrypt(fromName);
            String toBody = mToCipher.encrypt(fromBody);

            if (toName != null && toBody != null) {
                dao.update(name, toName, toBody);
            } else {
                Logger.e("Error applying cipher to split storage");
            }
        }
    }
}
