package io.split.android.client.storage.db;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Database(
        entities = {
                MySegmentEntity.class, SplitEntity.class, EventEntity.class,
                ImpressionEntity.class, GeneralInfoEntity.class
        },
        version = 1
)
public abstract class SplitRoomDatabase extends RoomDatabase {

    public abstract MySegmentDao mySegmentDao();

    public abstract SplitDao splitDao();

    public abstract EventDao eventDao();

    public abstract ImpressionDao impressionDao();

    public abstract GeneralInfoDao generalInfoDao();

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