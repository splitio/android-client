package io.split.android.client.storage.db;

import static io.split.android.client.utils.Utils.checkArgument;
import static io.split.android.client.utils.Utils.checkNotNull;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.split.android.client.storage.db.attributes.AttributesDao;
import io.split.android.client.storage.db.attributes.AttributesEntity;
import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverDao;
import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverEntity;
import io.split.android.client.storage.db.impressions.unique.UniqueKeyEntity;
import io.split.android.client.storage.db.impressions.unique.UniqueKeysDao;

@Database(
        entities = {
                MySegmentEntity.class, SplitEntity.class, EventEntity.class,
                ImpressionEntity.class, GeneralInfoEntity.class, ImpressionsCountEntity.class,
                AttributesEntity.class, UniqueKeyEntity.class, ImpressionsObserverEntity.class
        },
        version = 5
)
public abstract class SplitRoomDatabase extends RoomDatabase {

    public abstract MySegmentDao mySegmentDao();

    public abstract SplitDao splitDao();

    public abstract EventDao eventDao();

    public abstract ImpressionDao impressionDao();

    public abstract GeneralInfoDao generalInfoDao();

    public abstract ImpressionsCountDao impressionsCountDao();

    public abstract AttributesDao attributesDao();

    public abstract UniqueKeysDao uniqueKeysDao();

    public abstract ImpressionsObserverDao impressionsObserverDao();

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
                        .setJournalMode(JournalMode.TRUNCATE)
                        .fallbackToDestructiveMigration()
                        .build();
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
