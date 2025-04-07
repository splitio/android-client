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

    // Eagerly loaded DAOs (directly exposed)
    public abstract AttributesDao attributesDao();
    public abstract GeneralInfoDao generalInfoDao();
    public abstract MySegmentDao mySegmentDao();
    public abstract MyLargeSegmentDao myLargeSegmentDao();
    
    // Lazily loaded DAOs (cached in volatile fields)
    private volatile SplitDao mSplitDao;
    private volatile EventDao mEventDao;
    private volatile ImpressionDao mImpressionDao;
    private volatile ImpressionsCountDao mImpressionsCountDao;
    private volatile UniqueKeysDao mUniqueKeysDao;
    private volatile ImpressionsObserverCacheDao mImpressionsObserverCacheDao;
    private volatile SplitQueryDao mSplitQueryDao;

    // Abstract methods for lazy-loaded DAOs
    protected abstract SplitDao splitDaoInternal();
    protected abstract EventDao eventDaoInternal();
    protected abstract ImpressionDao impressionDaoInternal();
    protected abstract ImpressionsCountDao impressionsCountDaoInternal();
    protected abstract UniqueKeysDao uniqueKeysDaoInternal();
    protected abstract ImpressionsObserverCacheDao impressionsObserverCacheDaoInternal();

    // Lazy-loaded public methods
    public SplitDao splitDao() {
        if (mSplitDao == null) {
            synchronized (this) {
                if (mSplitDao == null) {
                    mSplitDao = splitDaoInternal();
                }
            }
        }
        return mSplitDao;
    }

    public EventDao eventDao() {
        if (mEventDao == null) {
            synchronized (this) {
                if (mEventDao == null) {
                    mEventDao = eventDaoInternal();
                }
            }
        }
        return mEventDao;
    }

    public ImpressionDao impressionDao() {
        if (mImpressionDao == null) {
            synchronized (this) {
                if (mImpressionDao == null) {
                    mImpressionDao = impressionDaoInternal();
                }
            }
        }
        return mImpressionDao;
    }

    public ImpressionsCountDao impressionsCountDao() {
        if (mImpressionsCountDao == null) {
            synchronized (this) {
                if (mImpressionsCountDao == null) {
                    mImpressionsCountDao = impressionsCountDaoInternal();
                }
            }
        }
        return mImpressionsCountDao;
    }

    public UniqueKeysDao uniqueKeysDao() {
        if (mUniqueKeysDao == null) {
            synchronized (this) {
                if (mUniqueKeysDao == null) {
                    mUniqueKeysDao = uniqueKeysDaoInternal();
                }
            }
        }
        return mUniqueKeysDao;
    }

    public ImpressionsObserverCacheDao impressionsObserverCacheDao() {
        if (mImpressionsObserverCacheDao == null) {
            synchronized (this) {
                if (mImpressionsObserverCacheDao == null) {
                    mImpressionsObserverCacheDao = impressionsObserverCacheDaoInternal();
                }
            }
        }
        return mImpressionsObserverCacheDao;
    }

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

    private static volatile Map<String, SplitRoomDatabase> mInstances = new ConcurrentHashMap<>();
}
