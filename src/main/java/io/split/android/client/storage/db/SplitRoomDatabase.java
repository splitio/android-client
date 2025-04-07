package io.split.android.client.storage.db;

import static io.split.android.client.utils.Utils.checkArgument;
import static io.split.android.client.utils.Utils.checkNotNull;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import io.split.android.client.storage.db.attributes.AttributesDao;
import io.split.android.client.storage.db.attributes.AttributesEntity;
import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheDao;
import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheEntity;
import io.split.android.client.storage.db.impressions.unique.UniqueKeyEntity;
import io.split.android.client.storage.db.impressions.unique.UniqueKeysDao;

@Database(
        entities = {
                MySegmentEntity.class, SplitEntity.class, EventEntity.class,
                ImpressionEntity.class, GeneralInfoEntity.class, ImpressionsCountEntity.class,
                AttributesEntity.class, UniqueKeyEntity.class, ImpressionsObserverCacheEntity.class,
                MyLargeSegmentEntity.class
        },
        version = 6
)
public abstract class SplitRoomDatabase extends RoomDatabase {

    public abstract MySegmentDao mySegmentDao();

    public abstract MyLargeSegmentDao myLargeSegmentDao();

    public abstract SplitDao splitDao();

    public abstract EventDao eventDao();

    public abstract ImpressionDao impressionDao();

    public abstract GeneralInfoDao generalInfoDao();

    public abstract ImpressionsCountDao impressionsCountDao();

    public abstract AttributesDao attributesDao();

    public abstract UniqueKeysDao uniqueKeysDao();

    public abstract ImpressionsObserverCacheDao impressionsObserverCacheDao();

    private volatile SplitQueryDao mSplitQueryDao;

    private static volatile Map<String, SplitRoomDatabase> mInstances = new ConcurrentHashMap<>();

    /**
     * Get the SplitQueryDao instance for optimized split queries.
     * This uses direct cursor access for better performance.
     */
    public SplitQueryDao getSplitQueryDao() {
        if (mSplitQueryDao == null) {
            synchronized (this) {
                if (mSplitQueryDao == null) {
                    mSplitQueryDao = new SplitQueryDaoImpl(this);
                }
            }
        }
        return mSplitQueryDao;
    }

    public static SplitRoomDatabase getDatabase(final Context context, final String databaseName) {
        checkNotNull(context);
        checkNotNull(databaseName);
        checkArgument(!databaseName.isEmpty());
        SplitRoomDatabase instance = null;
        synchronized (SplitRoomDatabase.class) {
            instance = mInstances.get(databaseName);
            if (instance == null) {
                instance = Room.databaseBuilder(context.getApplicationContext(),
                        SplitRoomDatabase.class, databaseName)
                        .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                        .fallbackToDestructiveMigration()
                        .build();
                
                // Get the underlying SQLite database and optimize it
                try {
                    SupportSQLiteDatabase db = instance.getOpenHelper().getWritableDatabase();
                    // These pragmas should be safe to execute on an open database

                    db.execSQL("PRAGMA cache_size = -3000");
                    db.execSQL("PRAGMA automatic_index = ON");
                    db.execSQL("PRAGMA foreign_keys = OFF");
                } catch (Exception e) {
                    // Log the error but don't crash
                    System.out.println("Failed to set database pragmas: " + e.getMessage());
                }
                
                mInstances.put(databaseName, instance);
            }
        }
        return instance;
    }
}
