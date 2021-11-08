package tests.database;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;

import helper.DatabaseHelper;
import io.split.android.client.storage.db.SplitRoomDatabase;

public abstract class GenericDaoTest {

    protected SplitRoomDatabase mRoomDb;
    protected Context mContext;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mRoomDb = DatabaseHelper.getTestDatabase(mContext);
        mRoomDb.clearAllTables();
    }

    @After
    public void tearDown() {
        mRoomDb.clearAllTables();
    }
}
