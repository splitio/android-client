package io.split.android.client.storage.cipher;

import androidx.annotation.NonNull;

import java.util.Collections;
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
import io.split.android.client.storage.db.MySegmentDao;
import io.split.android.client.storage.db.MySegmentEntity;
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
            updateSplits(mSplitDatabase.splitDao());
            updateSegments(mSplitDatabase.mySegmentDao());
            updateImpressions(mSplitDatabase.impressionDao());
            updateEvents(mSplitDatabase.eventDao());
            updateImpressionsCount(mSplitDatabase.impressionsCountDao());
            updateUniqueKeys(mSplitDatabase.uniqueKeysDao());
            updateAttributes(mSplitDatabase.attributesDao());

            return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
        } catch (Exception e) {
            return SplitTaskExecutionInfo.error(SplitTaskType.GENERIC_TASK);
        }
    }

    private void updateAttributes(AttributesDao attributesDao) {
        List<AttributesEntity> items = attributesDao.getAll();

        for (AttributesEntity item : items) {
            String body = mFromCipher.decrypt(item.getAttributes());

            String decryptedBody = mToCipher.encrypt(body);

            if (decryptedBody != null) {
                item.setAttributes(decryptedBody);
                attributesDao.update(item);
            } else {
                Logger.e("Error applying cipher to attributes storage");
            }
        }
    }

    private void updateUniqueKeys(UniqueKeysDao uniqueKeysDao) {
        List<UniqueKeyEntity> items = uniqueKeysDao.getAll();

        for (UniqueKeyEntity item : items) {
            String featureList = mFromCipher.decrypt(item.getFeatureList());

            String decryptedFeatureList = mToCipher.encrypt(featureList);

            if (decryptedFeatureList != null) {
                item.setFeatureList(decryptedFeatureList);
                uniqueKeysDao.insert(item);
            } else {
                Logger.e("Error applying cipher to unique keys storage");
            }
        }
    }

    private void updateImpressionsCount(ImpressionsCountDao impressionsCountDao) {
        List<ImpressionsCountEntity> items = impressionsCountDao.getAll();

        for (ImpressionsCountEntity item : items) {
            String body = mFromCipher.decrypt(item.getBody());

            String decryptedBody = mToCipher.encrypt(body);

            if (decryptedBody != null) {
                item.setBody(decryptedBody);
                impressionsCountDao.insert(item);
            } else {
                Logger.e("Error applying cipher to impression count storage");
            }
        }
    }

    private void updateImpressions(ImpressionDao impressionDao) {
        List<ImpressionEntity> items = impressionDao.getAll();

        for (ImpressionEntity item : items) {
            String name = mFromCipher.decrypt(item.getTestName());
            String body = mFromCipher.decrypt(item.getBody());

            String decryptedName = mToCipher.encrypt(name);
            String decryptedBody = mToCipher.encrypt(body);

            if (decryptedName != null && decryptedBody != null) {
                item.setTestName(decryptedName);
                item.setBody(decryptedBody);
                impressionDao.insert(item);
            } else {
                Logger.e("Error applying cipher to impression storage");
            }
        }
    }

    private void updateSegments(MySegmentDao mySegmentDao) {
        List<MySegmentEntity> items = mySegmentDao.getAll();

        for (MySegmentEntity item : items) {
            String body = mFromCipher.decrypt(item.getSegmentList());

            String decryptedBody = mToCipher.encrypt(body);

            if (decryptedBody != null) {
                item.setSegmentList(decryptedBody);
                mySegmentDao.update(item);
            } else {
                Logger.e("Error applying cipher to my segment");
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
            String fromBody = mFromCipher.decrypt(item.getBody());
            String toBody = mToCipher.encrypt(fromBody);

            if (toBody != null) {
                item.setBody(toBody);
                dao.insert(Collections.singletonList(item));
            } else {
                Logger.e("Error applying cipher to split storage");
            }
        }
    }
}
