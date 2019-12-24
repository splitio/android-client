package io.split.android.client.storage.db.migrator;

import android.content.Context;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

import io.split.android.client.cache.MySegmentsCache;
import io.split.android.client.cache.MySegmentsCacheMigrator;
import io.split.android.client.storage.db.GeneralInfoDao;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.MySegmentDao;
import io.split.android.client.storage.db.MySegmentEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.legacy.FileStorage;
import io.split.android.client.storage.legacy.IStorage;
import io.split.android.client.utils.StringHelper;

import static com.google.common.base.Preconditions.checkNotNull;

public class StorageMigrator {

    private final SplitRoomDatabase mSqLiteDatabase;
    private final Context mContext;
    private final String mDatabaseName;
    private final StringHelper mStringHelper;
    private final SplMigratorHelper mMySegmentsMigratorHelper;
    private final SplitsMigratorHelper mSplitsMigratorHelper;
    private final EventsMigratorHelper mEventsMigratorHelper;
    private final ImpressionsMigratorHelper mImpressionsMigratorHelper;

    public StorageMigrator(@NotNull Context context,
                           @NotNull String databaseName,
                           @NotNull SplMigratorHelper mySegmentsMigratorHelper,
                           @NotNull SplitsMigratorHelper splitsMigratorHelper,
                           @NotNull EventsMigratorHelper eventsMigratorHelper,
                           @NotNull ImpressionsMigratorHelper impressionsMigratorHelper
                           ) {

        mContext = checkNotNull(context);
        mDatabaseName = checkNotNull(databaseName);
        mMySegmentsMigratorHelper = checkNotNull(mySegmentsMigratorHelper);
        mSplitsMigratorHelper = checkNotNull(splitsMigratorHelper);
        mEventsMigratorHelper = checkNotNull(eventsMigratorHelper);
        mImpressionsMigratorHelper = checkNotNull(impressionsMigratorHelper);

        mSqLiteDatabase = SplitRoomDatabase.getDatabase(context, databaseName);
        mStringHelper = new StringHelper();
    }

    public void checkAndMigrateIfNeeded() {

        if(!isMigrationNeeded()) {
            return;
        }

        File legacyCacheDir = mContext.getCacheDir();
        IStorage legacyFileStorage = new FileStorage(legacyCacheDir, mDatabaseName);
        MySegmentsCacheMigrator legacyMySegmentsCache = new MySegmentsCache(legacyFileStorage);
    }

    private boolean isMigrationNeeded() {
        GeneralInfoDao generalInfoDao = mSqLiteDatabase.generalInfoDao();
        GeneralInfoEntity migrationStatus = generalInfoDao.getByName(GeneralInfoEntity.DATBASE_MIGRATION_STATUS);
        return migrationStatus.getLongValue() == GeneralInfoEntity.DATBASE_MIGRATION_STATUS_PENDING;
    }

    private void runMigration(List<MySegmentEntity> mySegmentEntities) {
        MySegmentDao mySegmentDao = mSqLiteDatabase.mySegmentDao();

        for(MySegmentEntity entity : mySegmentEntities) {
            mySegmentDao.update(entity);
        }
    }
}
