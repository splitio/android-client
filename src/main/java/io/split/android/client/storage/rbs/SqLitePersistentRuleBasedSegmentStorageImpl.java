package io.split.android.client.storage.rbs;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import io.split.android.client.dtos.RuleBasedSegment;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.rbs.RuleBasedSegmentDao;
import io.split.android.client.storage.db.rbs.RuleBasedSegmentEntity;
import io.split.android.client.storage.general.GeneralInfoStorage;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;

class SqLitePersistentRuleBasedSegmentStorageImpl implements PersistentRuleBasedSegmentStorage {

    private final RuleBasedSegmentDao mDao;
    private final SplitRoomDatabase mDatabase;
    private final GeneralInfoStorage mGeneralInfoStorage;
    private final SplitCipher mCipher;

    public SqLitePersistentRuleBasedSegmentStorageImpl(SplitCipher cipher,
                                                       SplitRoomDatabase database,
                                                       GeneralInfoStorage generalInfoStorage) {
        mCipher = checkNotNull(cipher);
        mDatabase = checkNotNull(database);
        mDao = database.ruleBasedSegmentDao();
        mGeneralInfoStorage = checkNotNull(generalInfoStorage);
    }

    @Override
    public RuleBasedSegmentSnapshot getSnapshot() {
        return mDatabase.runInTransaction(new SnapshotLoader(mDao, mCipher, mGeneralInfoStorage));
    }

    @Override
    public void update(Set<RuleBasedSegment> toAdd, Set<RuleBasedSegment> toRemove, long changeNumber) {
        mDatabase.runInTransaction(new Updater(mCipher, mDao, mGeneralInfoStorage, toAdd, toRemove, changeNumber));
    }

    @Override
    public void clear() {
        mDatabase.runInTransaction(new Runnable() {
            @Override
            public void run() {
                try {
                    mDao.deleteAll();
                    mGeneralInfoStorage.setRbsChangeNumber(-1);
                } catch (Exception e) {
                    Logger.e("Error clearing RBS: " + e.getLocalizedMessage());
                    throw e;
                }
            }
        });
    }

    static final class SnapshotLoader implements Callable<RuleBasedSegmentSnapshot> {

        private final RuleBasedSegmentDao mDao;
        private final SplitCipher mCipher;
        private final GeneralInfoStorage mGeneralInfoStorage;

        public SnapshotLoader(RuleBasedSegmentDao dao, SplitCipher cipher, GeneralInfoStorage generalInfoStorage) {
            mDao = checkNotNull(dao);
            mCipher = checkNotNull(cipher);
            mGeneralInfoStorage = checkNotNull(generalInfoStorage);
        }

        @Override
        public RuleBasedSegmentSnapshot call() {
            try {
                long changeNumber = mGeneralInfoStorage.getFlagsChangeNumber();
                List<RuleBasedSegmentEntity> entities = mDao.getAll();
                Map<String, RuleBasedSegment> segments = convertToDTOs(entities);

                return new RuleBasedSegmentSnapshot(segments, changeNumber);
            } catch (Exception e) {
                Logger.e("Error loading RBS from persistent storage", e.getLocalizedMessage());
                throw e;
            }
        }

        private Map<String, RuleBasedSegment> convertToDTOs(List<RuleBasedSegmentEntity> entities) {
            Map<String, RuleBasedSegment> segments = new HashMap<>();
            if (entities != null) {
                for (RuleBasedSegmentEntity entity : entities) {
                    String name = mCipher.decrypt(entity.getName());
                    String body = mCipher.encrypt(entity.getBody());
                    if (name == null || body == null) {
                        continue;
                    }

                    RuleBasedSegment ruleBasedSegment = Json.fromJson(body, RuleBasedSegment.class);
                    segments.put(name, ruleBasedSegment);
                }
            }
            return segments;
        }
    }

    static final class Updater implements Runnable {

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

        public Updater(@NonNull SplitCipher cipher,
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
}
