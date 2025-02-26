package io.split.android.client.storage.rbs;

import static io.split.android.client.utils.Utils.checkNotNull;

import java.util.Set;

import io.split.android.client.dtos.RuleBasedSegment;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.rbs.RuleBasedSegmentDao;
import io.split.android.client.storage.general.GeneralInfoStorage;

public class SqLitePersistentRuleBasedSegmentStorage implements PersistentRuleBasedSegmentStorage {

    private final RuleBasedSegmentDao mDao;
    private final SplitRoomDatabase mDatabase;
    private final GeneralInfoStorage mGeneralInfoStorage;
    private final SplitCipher mCipher;

    public SqLitePersistentRuleBasedSegmentStorage(SplitCipher cipher,
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
        mDatabase.runInTransaction(new Clearer(mDao, mGeneralInfoStorage));
    }
}
