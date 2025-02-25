package io.split.android.client.storage.rbs;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.split.android.client.dtos.RuleBasedSegment;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.rbs.RuleBasedSegmentDao;
import io.split.android.client.storage.db.rbs.RuleBasedSegmentEntity;
import io.split.android.client.storage.general.GeneralInfoStorage;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;

final class Updater implements Runnable {

    @NonNull
    private final SplitCipher mCipher;
    @NonNull
    private final GeneralInfoStorage mGeneralInfoStorage;
    @NonNull
    private final RuleBasedSegmentDao mDao;
    @NonNull
    private final Set<RuleBasedSegment> mToAdd;
    @NonNull
    private final Set<RuleBasedSegment> mToRemove;
    private final long mChangeNumber;

    Updater(@NonNull SplitCipher cipher,
            @NonNull RuleBasedSegmentDao dao,
            @NonNull GeneralInfoStorage generalInfoStorage,
            @NonNull Set<RuleBasedSegment> toAdd,
            @NonNull Set<RuleBasedSegment> toRemove,
            long changeNumber) {
        mCipher = checkNotNull(cipher);
        mDao = checkNotNull(dao);
        mGeneralInfoStorage = checkNotNull(generalInfoStorage);
        mToAdd = checkNotNull(toAdd);
        mToRemove = checkNotNull(toRemove);
        mChangeNumber = changeNumber;
    }

    @Override
    public void run() {
        try {
            List<String> toDelete = new ArrayList<>();
            for (RuleBasedSegment segment : mToRemove) {
                toDelete.add(mCipher.encrypt(segment.getName()));
            }

            List<RuleBasedSegmentEntity> toAdd = new ArrayList<>();
            for (RuleBasedSegment segment : mToAdd) {
                String name = mCipher.encrypt(segment.getName());
                String body = mCipher.encrypt(Json.toJson(segment));
                toAdd.add(new RuleBasedSegmentEntity(name, body, System.currentTimeMillis()));
            }

            mDao.delete(toDelete);
            mDao.insert(toAdd);
            mGeneralInfoStorage.setRbsChangeNumber(mChangeNumber);
        } catch (Exception e) {
            Logger.e("Error updating RBS: " + e.getLocalizedMessage());
            throw e;
        }
    }
}
