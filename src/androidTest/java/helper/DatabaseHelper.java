package helper;

import android.content.Context;

import androidx.room.Room;
import androidx.test.platform.app.InstrumentationRegistry;

import java.io.File;

import io.split.android.client.storage.db.SplitRoomDatabase;

public class DatabaseHelper {
    public static boolean removeDatabaseFile(String name) {
        File databases = new File(InstrumentationRegistry.getInstrumentation().getContext().getApplicationInfo().dataDir + "/databases");
        File db = new File(databases, name);
        return db.delete();
    }

    public static SplitRoomDatabase getTestDatabase(Context context) {
        return Room.inMemoryDatabaseBuilder(context, SplitRoomDatabase.class).build();
    }
}