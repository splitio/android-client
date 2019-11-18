package io.split.android.client.storage.db;

import android.content.Context;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.FileStore;

import io.split.android.client.storage.legacy.FileStorage;
import io.split.android.client.storage.legacy.IStorage;
import io.split.android.client.storage.legacy.ImpressionsFileStorage;

import static com.google.common.base.Preconditions.checkNotNull;

public class StorageMigrator {

    private SplitRoomDatabase mSqLiteDatabase;
    private Context mContext;
    private String mDatabaseName;

    public StorageMigrator(@NotNull Context context, @NotNull String databaseName) {

        checkNotNull(context);
        checkNotNull(databaseName);

        mSqLiteDatabase = SplitRoomDatabase.getDatabase(context, databaseName);
        mContext = context;
        mDatabaseName = databaseName;
    }

    public void checkAndMigrateIfNeeded() {

        if(!isMigrationNeeded()) {
            return;
        }

        File legacyCacheDir = mContext.getCacheDir();
        IStorage legacyFileStorage = new FileStorage(legacyCacheDir, mDatabaseName);
    }

    private boolean isMigrationNeeded() {
        GeneralInfoDao generalInfoDao = mSqLiteDatabase.generalInfoDao();
        GeneralInfoEntity migrationStatus = generalInfoDao.getByName(GeneralInfoEntity.DATBASE_MIGRATION_STATUS);
        return migrationStatus.getLongValue() == GeneralInfoEntity.DATBASE_MIGRATION_STATUS_PENDING;
    }

    private void migrateMySegments(IStorage legacyFileStorage) {

    }



    private Map<String, String> private void loadSegmentsFromDisk(IStorage legacyFileStorage){

        try {
            String storedMySegments = legacyFileStorage.read(getMySegmentsFileName());
            if(storedMySegments == null || storedMySegments.trim().equals("")) return;
            Type listType = new TypeToken<Map<String, List<MySegment>>>() {
            }.getType();

            Map<String, List<MySegment>> segments = Json.fromJson(storedMySegments, listType);
            Set<String> keys = segments.keySet();
            for (String key : keys) {
                List<MySegment> keySegments = segments.get(key);
                if(keySegments != null) {
                    mSegments.put(key, Collections.synchronizedList(keySegments));
                }
            }

        } catch (IOException e) {
            Logger.e(e, "Unable to get my segments");
        } catch (JsonSyntaxException syntaxException) {
            Logger.e(syntaxException, "Unable to parse saved segments");
        }
    }
}
