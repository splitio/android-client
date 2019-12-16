package io.split.android.client.storage.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

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

    public abstract EventDao trackEventDao();

    public abstract ImpressionDao impressionDao();

    public abstract GeneralInfoDao generalInfoDao();

    private static volatile SplitRoomDatabase mInstance;

    public static SplitRoomDatabase getDatabase(final Context context, final String databaseName) {
        checkNotNull(context);
        checkNotNull(databaseName);
        checkArgument(!databaseName.isEmpty());

        if (mInstance == null) {
            synchronized (SplitRoomDatabase.class) {
                if (mInstance == null) {
                    mInstance = Room.databaseBuilder(context.getApplicationContext(),
                            SplitRoomDatabase.class, databaseName)
                            // TODO: Temproal to test with current setup. REMOVE!!
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return mInstance;
    }
}