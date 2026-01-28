package io.split.android.client.storage.rbs;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import io.split.android.client.dtos.RuleBasedSegment;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.rbs.RuleBasedSegmentDao;
import io.split.android.client.storage.db.rbs.RuleBasedSegmentEntity;
import io.split.android.client.storage.general.GeneralInfoStorage;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;

final class SnapshotLoader implements Callable<RuleBasedSegmentSnapshot> {

    private final RuleBasedSegmentDao mDao;
    private final SplitCipher mCipher;
    private final GeneralInfoStorage mGeneralInfoStorage;

    SnapshotLoader(RuleBasedSegmentDao dao, SplitCipher cipher, GeneralInfoStorage generalInfoStorage) {
        mDao = checkNotNull(dao);
        mCipher = checkNotNull(cipher);
        mGeneralInfoStorage = checkNotNull(generalInfoStorage);
    }

    @Override
    public RuleBasedSegmentSnapshot call() {
        try {
            long changeNumber = mGeneralInfoStorage.getRbsChangeNumber();
            List<RuleBasedSegmentEntity> entities = mDao.getAll();
            Map<String, RuleBasedSegment> segments = convertToDTOs(entities);

            return new RuleBasedSegmentSnapshot(segments, changeNumber);
        } catch (Exception e) {
            Logger.e("Error loading RBS from persistent storage", e.getLocalizedMessage());
            throw e;
        }
    }

    @NonNull
    private Map<String, RuleBasedSegment> convertToDTOs(@Nullable List<RuleBasedSegmentEntity> entities) {
        Map<String, RuleBasedSegment> segments = new HashMap<>();
        if (entities != null) {
            for (RuleBasedSegmentEntity entity : entities) {
                String name = mCipher.decrypt(entity.getName());
                String body = mCipher.decrypt(entity.getBody());
                if (name == null || body == null) {
                    continue;
                }

                try {
                    RuleBasedSegment ruleBasedSegment = Json.fromJson(body, RuleBasedSegment.class);
                    segments.put(name, ruleBasedSegment);
                } catch (Exception e) {
                    Logger.e("Error parsing RBS with name " + name + ": " + e.getLocalizedMessage());
                }
            }
        }
        return segments;
    }
}
