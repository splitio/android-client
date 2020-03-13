package helper;

import androidx.test.platform.app.InstrumentationRegistry;

import java.io.File;

public class DatabaseHelper {
    public static boolean removeDatabaseFile(String name) {
        File databases = new File(InstrumentationRegistry.getInstrumentation().getContext().getApplicationInfo().dataDir + "/databases");
        File db = new File(databases, name);
        return db.delete();
    }
}