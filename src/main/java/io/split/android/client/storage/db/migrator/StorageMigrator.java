package io.split.android.client.storage.db.migrator;

import androidx.core.util.Pair;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import io.split.android.client.storage.db.EventEntity;
import io.split.android.client.storage.db.GeneralInfoDao;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.ImpressionEntity;
import io.split.android.client.storage.db.MySegmentEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Logger;
import io.split.android.client.utils.StringHelper;

import static com.google.common.base.Preconditions.checkNotNull;

public class StorageMigrator {

    private final SplitRoomDatabase mSqLiteDatabase;
    private final StringHelper mStringHelper;
    private final MySegmentsMigratorHelper mMySegmentsMigratorHelper;
    private final SplitsMigratorHelper mSplitsMigratorHelper;
    private final EventsMigratorHelper mEventsMigratorHelper;
    private final ImpressionsMigratorHelper mImpressionsMigratorHelper;
    private final GeneralInfoDao mGeneralInfoDao;

    public StorageMigrator(@NotNull SplitRoomDatabase sqLiteDatabase,
                           @NotNull MySegmentsMigratorHelper mySegmentsMigratorHelper,
                           @NotNull SplitsMigratorHelper splitsMigratorHelper,
                           @NotNull EventsMigratorHelper eventsMigratorHelper,
                           @NotNull ImpressionsMigratorHelper impressionsMigratorHelper
    ) {
        mSqLiteDatabase = checkNotNull(sqLiteDatabase);
        mMySegmentsMigratorHelper = checkNotNull(mySegmentsMigratorHelper);
        mSplitsMigratorHelper = checkNotNull(splitsMigratorHelper);
        mEventsMigratorHelper = checkNotNull(eventsMigratorHelper);
        mImpressionsMigratorHelper = checkNotNull(impressionsMigratorHelper);
        mGeneralInfoDao = mSqLiteDatabase.generalInfoDao();
        mStringHelper = new StringHelper();
    }

    public void checkAndMigrateIfNeeded() {
        if (isMigrationNeeded()) {
            // If migration fails, data is erased and
            // new storage is used anyway to avoid trying to migrate
            // every time sdk is initialized
            runMigration();
            mGeneralInfoDao.update(new GeneralInfoEntity(
                            GeneralInfoEntity.DATBASE_MIGRATION_STATUS, 1));
        }
    }

    private boolean isMigrationNeeded() {
        GeneralInfoEntity migrationStatus = mGeneralInfoDao.getByName(GeneralInfoEntity.DATBASE_MIGRATION_STATUS);
        return migrationStatus == null;
    }

    private void runMigration() {

        // Migration data is loaded within the transaction to limit its scope
        // to its own function and that way try to use as less memory as possible
        mSqLiteDatabase.runInTransaction(new Runnable() {
            @Override
            public void run() {
                try {
                    migrateMySegments();
                    migrateSplits();
                    migrateEvents();
                    migrateImpressions();
                } catch (Exception e) {
                    Logger.e("Couldn't migrate legacy data. Reason: " +
                            e.getLocalizedMessage());
                }
            }
        });
    }

    private void migrateMySegments() {
        List<MySegmentEntity> mySegmentEntities = mMySegmentsMigratorHelper.loadLegacySegmentsAsEntities();
        for (MySegmentEntity entity : mySegmentEntities) {
            mSqLiteDatabase.mySegmentDao().update(entity);
        }
    }

    private void migrateSplits() {
        Pair<Long, List<SplitEntity>> splitsSnapshot = mSplitsMigratorHelper.loadLegacySplitsAsEntities();
        mSqLiteDatabase.splitDao().insert(splitsSnapshot.second);
        GeneralInfoEntity changeNumberInfo = new GeneralInfoEntity(
                GeneralInfoEntity.CHANGE_NUMBER_INFO,
                splitsSnapshot.first);
        mSqLiteDatabase.generalInfoDao().update(changeNumberInfo);
    }

    private void migrateEvents() {
        List<EventEntity> eventEntities = mEventsMigratorHelper.loadLegacyEventsAsEntities();
        for (EventEntity entity : eventEntities) {
            mSqLiteDatabase.eventDao().insert(entity);
        }
    }

    private void migrateImpressions() {
        List<ImpressionEntity> impressionEntities = mImpressionsMigratorHelper.loadLegacyImpressionsAsEntities();
        for (ImpressionEntity entity : impressionEntities) {
            mSqLiteDatabase.impressionDao().insert(entity);
        }
    }
}
