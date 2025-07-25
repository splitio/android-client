package tests.storage;

import static org.junit.Assert.assertEquals;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import helper.DatabaseHelper;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.general.GeneralInfoStorage;
import io.split.android.client.storage.general.GeneralInfoStorageImpl;

public class GeneralInfoStorageTest {

    private SplitRoomDatabase mDb;
    private GeneralInfoStorage mGeneralInfoStorage;

    @Before
    public void setUp() {
        mDb = DatabaseHelper.getTestDatabase(InstrumentationRegistry.getInstrumentation().getContext());
        mGeneralInfoStorage = new GeneralInfoStorageImpl(mDb.generalInfoDao(), null);
    }

    @After
    public void tearDown() {
        mDb.clearAllTables();
        mDb.close();
    }

    @Test
    public void setSplitsUpdateTimestamp() {
        long initialValue = mGeneralInfoStorage.getSplitsUpdateTimestamp();
        mGeneralInfoStorage.setSplitsUpdateTimestamp(100L);
        long finalValue = mGeneralInfoStorage.getSplitsUpdateTimestamp();

        assertEquals(0L, initialValue);
        assertEquals(100L, finalValue);
    }

    @Test
    public void setFlagsChangeNumber() {
        long initialValue = mGeneralInfoStorage.getFlagsChangeNumber();
        mGeneralInfoStorage.setFlagsChangeNumber(100L);
        long finalValue = mGeneralInfoStorage.getFlagsChangeNumber();

        assertEquals(-1L, initialValue);
        assertEquals(100L, finalValue);
    }

    @Test
    public void setSplitsFilterQueryString() {
        String initialValue = mGeneralInfoStorage.getSplitsFilterQueryString();
        mGeneralInfoStorage.setSplitsFilterQueryString("queryString");
        String finalValue = mGeneralInfoStorage.getSplitsFilterQueryString();

        assertEquals("", initialValue);
        assertEquals("queryString", finalValue);
    }

    @Test
    public void setDatabaseEncryptionMode() {
        String initialValue = mGeneralInfoStorage.getDatabaseEncryptionMode();
        mGeneralInfoStorage.setDatabaseEncryptionMode("MODE");
        String finalValue = mGeneralInfoStorage.getDatabaseEncryptionMode();

        assertEquals("", initialValue);
        assertEquals("MODE", finalValue);
    }

    @Test
    public void setFlagsSpec() {
        String initialValue = mGeneralInfoStorage.getFlagsSpec();
        mGeneralInfoStorage.setFlagsSpec("4.4");
        String finalValue = mGeneralInfoStorage.getFlagsSpec();

        assertEquals("", initialValue);
        assertEquals("4.4", finalValue);
    }

    @Test
    public void setRolloutCacheLastClearTimestamp() {
        long initialValue = mGeneralInfoStorage.getRolloutCacheLastClearTimestamp();
        mGeneralInfoStorage.setRolloutCacheLastClearTimestamp(100L);
        long finalValue = mGeneralInfoStorage.getRolloutCacheLastClearTimestamp();

        assertEquals(0L, initialValue);
        assertEquals(100L, finalValue);
    }

    @Test
    public void setRbsChangeNumber() {
        long initialValue = mGeneralInfoStorage.getRbsChangeNumber();
        mGeneralInfoStorage.setRbsChangeNumber(100L);
        long finalValue = mGeneralInfoStorage.getRbsChangeNumber();

        assertEquals(-1L, initialValue);
        assertEquals(100L, finalValue);
    }
}
