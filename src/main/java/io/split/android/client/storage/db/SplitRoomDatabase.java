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
                        .setQueryExecutor(Executors.newFixedThreadPool(2))
                        .fallbackToDestructiveMigration()
                        .setQueryCallback(new RoomDatabase.QueryCallback() {
                            @Override
                            public void onQuery(@NonNull String sqlQuery, @NonNull List<Object> bindArgs) {
                                // This is just for logging/debugging if needed
                            }
                        }, Executors.newFixedThreadPool(4))
                        .build();
                
                // Get the underlying SQLite database and optimize it
                try {
                    SupportSQLiteDatabase db = instance.getOpenHelper().getWritableDatabase();
                    // These pragmas should be safe to execute on an open database

                    db.execSQL("PRAGMA cache_size = -3000");
                    db.execSQL("PRAGMA automatic_index = ON");
                    db.execSQL("PRAGMA foreign_keys = OFF");
                    
                    // Preload the splitDao().getAll() query to warm up the SQLite cache
                    System.out.println("[SPLIT-PERF] Preloading splitDao().getAll() to warm up SQLite cache");
                    long startTime = System.currentTimeMillis();
                    Executors.newSingleThreadExecutor().execute(() -> {
                        try {
                            // Execute the query that will be used later to load splits
                            db.query("SELECT name, body FROM splits");
                            db.query("SELECT user_key, segment_list, updated_at FROM my_segments");
                            db.query("SELECT user_key, segment_list, updated_at FROM my_large_segments");
                            db.query("SELECT user_key, attributes, updated_at FROM attributes");
                            long endTime = System.currentTimeMillis();
                            System.out.println("[SPLIT-PERF] Preloaded splitDao().getAll() in " + (endTime - startTime) + "ms");
                        } catch (Exception e) {
                            System.out.println("[SPLIT-PERF] Failed to preload splitDao().getAll(): " + e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    // Log the error but don't crash
                    System.out.println("Failed to set database pragmas: " + e.getMessage());
                }
                
                mInstances.put(databaseName, instance);
            }
        }
        return instance;
    }

    public SplitQueryDao splitQueryDao() {
        if (mSplitQueryDao != null) {
            return mSplitQueryDao;
        }
        synchronized (this) {
            mSplitQueryDao = new SplitQueryDaoImpl(this);
            return mSplitQueryDao;
        }
    }
}
