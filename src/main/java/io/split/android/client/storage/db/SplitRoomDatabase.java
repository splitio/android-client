package io.split.android.client.storage.db;

import static io.split.android.client.utils.Utils.checkArgument;
import static io.split.android.client.utils.Utils.checkNotNull;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.split.android.client.storage.db.attributes.AttributesDao;
import io.split.android.client.storage.db.attributes.AttributesEntity;
import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheDao;
import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheEntity;
import io.split.android.client.storage.db.impressions.unique.UniqueKeyEntity;
import io.split.android.client.storage.db.impressions.unique.UniqueKeysDao;
import io.split.android.client.utils.logger.Logger;
import io.split.android.client.storage.db.rbs.RuleBasedSegmentDao;
import io.split.android.client.storage.db.rbs.RuleBasedSegmentEntity;

@Database(
        entities = {
                MySegmentEntity.class, SplitEntity.class, EventEntity.class,
                ImpressionEntity.class, GeneralInfoEntity.class, ImpressionsCountEntity.class,
                AttributesEntity.class, UniqueKeyEntity.class, ImpressionsObserverCacheEntity.class,
                MyLargeSegmentEntity.class, RuleBasedSegmentEntity.class
        },
        version = 7
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

    public abstract RuleBasedSegmentDao ruleBasedSegmentDao();

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

                try {
                    SupportSQLiteDatabase db = instance.getOpenHelper().getWritableDatabase();

                    db.execSQL("PRAGMA cache_size = -3000");
                    db.execSQL("PRAGMA automatic_index = ON");
                    db.execSQL("PRAGMA foreign_keys = OFF");
                } catch (Exception e) {
                    Logger.i("Failed to set optimized pragma");
                }

                mInstances.put(databaseName, instance);
                // Ensure Room is fully initialized before starting preload thread
                try {
                    instance.getOpenHelper().getWritableDatabase(); // Block until schema is validated
                } catch (Exception e) {
                    Logger.i("Failed to force Room initialization: " + e.getMessage());
                }
                new Thread(() -> {
                    try {
                        mInstances.get(databaseName).getSplitQueryDao();
                    } catch (Exception e) {
                        Logger.i("Failed to preload query DAO");
                    }
                }).start();
            }
        }
        return instance;
    }
}
