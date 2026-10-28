package tests.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.room.Room;

import io.split.android.client.dtos.Split;
import io.split.android.client.storage.cipher.NoOpCipher;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitQueryDaoImpl;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.splits.SqLitePersistentSplitsStorage;
import io.split.android.client.utils.Utils;
import io.split.android.client.utils.logger.LogPrinter;
import io.split.android.client.utils.logger.LogPrinterImpl;
import io.split.android.client.utils.logger.Logger;
import io.split.android.client.utils.logger.SplitLogLevel;

/**
 * Loads flags from a real Room database file, so that SQLite and the CursorWindow limit are exercised
 * rather than mocked. The oversized row body carries a marker that must never reach the log.
 */
public class OversizedSplitRowTest {

    private static final String DATABASE_NAME = "oversized-row-test";
    private static final String BODY_MARKER = "BODYMARKER";
    private static final int OVERSIZED_BODY_LENGTH = 3_000_000;
    private static final int CAP_LENGTH = 1_000_000;
    private static final Set<String> NORMAL_NAMES = new HashSet<>(
            Arrays.asList("flag_0", "flag_1", "flag_2", "flag_3", "flag_4"));
    // The marker is only in the body, so a log line that echoed the body would contain it.
    private static final String OVERSIZED_BODY = filler(
            "{\"name\":\"big_flag\",\"marker\":\"" + BODY_MARKER + "\"", OVERSIZED_BODY_LENGTH);

    private SplitRoomDatabase database;
    private CapturingPrinter printer;

    @Before
    public void setUp() {
        ApplicationProvider.getApplicationContext().deleteDatabase(DATABASE_NAME);
        database = Room.databaseBuilder(ApplicationProvider.getApplicationContext(),
                        SplitRoomDatabase.class, DATABASE_NAME)
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build();

        printer = new CapturingPrinter();
        Logger.instance().setPrinter(printer);
        Logger.instance().setLevel(SplitLogLevel.VERBOSE);
    }

    @After
    public void tearDown() {
        Logger.instance().setLevel(SplitLogLevel.NONE);
        Logger.instance().setPrinter(new LogPrinterImpl());
        database.close();
        ApplicationProvider.getApplicationContext().deleteDatabase(DATABASE_NAME);
    }

    @Test
    public void queryDao_oversizedAmongNormal() {
        seedNormalRows();
        database.splitDao().insert(Arrays.asList(oversizedEntity()));

        SplitQueryDaoImpl dao = new SplitQueryDaoImpl(database);

        Map<String, SplitEntity> map = dao.getAllAsMap();
        assertEquals(NORMAL_NAMES, map.keySet());
        assertEquals(NORMAL_NAMES, dao.getAllAsMap().keySet());

        List<String> bigFlagWarnings = warningsContaining("big_flag");
        assertEquals(1, bigFlagWarnings.size());
        assertTrue(bigFlagWarnings.get(0).contains(String.valueOf(OVERSIZED_BODY_LENGTH)));

        for (String message : printer.allMessages()) {
            assertFalse(message, message.contains(BODY_MARKER));
        }
    }

    @Test
    public void queryDao_capBoundary() {
        seedNormalRows();
        database.splitDao().insert(Arrays.asList(
                entity("at_cap", filler("{\"name\":\"at_cap\"", CAP_LENGTH)),
                entity("over_cap", filler("{\"name\":\"over_cap\"", CAP_LENGTH + 1))));

        SplitQueryDaoImpl dao = new SplitQueryDaoImpl(database);

        Map<String, SplitEntity> map = dao.getAllAsMap();
        assertEquals(NORMAL_NAMES.size() + 1, map.size());
        assertTrue(map.containsKey("at_cap"));
        assertEquals(CAP_LENGTH, map.get("at_cap").getBody().length());
        assertFalse(map.containsKey("over_cap"));
    }

    @Test
    public void queryDao_capIsInBytesNotCharacters() {
        seedNormalRows();
        // 500_001 two-byte characters: under the cap in characters, over it in UTF-8 bytes
        String multiByteBody = "{\"name\":\"multi_byte\",\"v\":\"" + Utils.repeat("\u00e9", CAP_LENGTH / 2 + 1) + "\"}";
        assertTrue(multiByteBody.length() < CAP_LENGTH);
        database.splitDao().insert(Arrays.asList(entity("multi_byte", multiByteBody)));

        Map<String, SplitEntity> map = new SplitQueryDaoImpl(database).getAllAsMap();

        assertEquals(NORMAL_NAMES.size(), map.size());
        assertFalse(map.containsKey("multi_byte"));
    }

    @Test
    public void storage_updateReplacesOversizedRow() {
        seedNormalRows();
        database.splitDao().insert(Arrays.asList(oversizedEntity()));

        SqLitePersistentSplitsStorage storage = new SqLitePersistentSplitsStorage(database, new NoOpCipher());

        Set<String> loaded = namesOf(storage.getAll());
        assertEquals(NORMAL_NAMES, loaded);
        assertFalse(loaded.contains("big_flag"));

        Split split = new Split();
        split.name = "big_flag";
        storage.update(split);

        List<Split> afterUpdate = storage.getAll();
        assertEquals(NORMAL_NAMES.size() + 1, afterUpdate.size());
        assertTrue(namesOf(afterUpdate).contains("big_flag"));
    }

    @Test
    public void queryDao_failedLoadNotCached() {
        seedNormalRows();
        database.getOpenHelper().getWritableDatabase().execSQL("ALTER TABLE splits RENAME TO splits_bak");

        SplitQueryDaoImpl dao = new SplitQueryDaoImpl(database);
        assertTrue(dao.getAllAsMap().isEmpty());

        database.getOpenHelper().getWritableDatabase().execSQL("ALTER TABLE splits_bak RENAME TO splits");

        // A non-empty partial failed result needs a mid-cursor fault; that case is covered only by
        // the JVM mock tests in SplitQueryDaoImplTest.
        assertEquals(NORMAL_NAMES, dao.getAllAsMap().keySet());
    }

    private void seedNormalRows() {
        List<SplitEntity> rows = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String name = "flag_" + i;
            rows.add(entity(name, filler("{\"name\":\"" + name + "\",\"killed\":false}", 200)));
        }
        database.splitDao().insert(rows);
    }

    private static SplitEntity oversizedEntity() {
        return entity("big_flag", OVERSIZED_BODY);
    }

    private List<String> warningsContaining(String text) {
        List<String> matches = new ArrayList<>();
        for (String warning : printer.warnings()) {
            if (warning.contains(text)) {
                matches.add(warning);
            }
        }
        return matches;
    }

    private static SplitEntity entity(String name, String body) {
        SplitEntity entity = new SplitEntity();
        entity.setName(name);
        entity.setBody(body);
        entity.setUpdatedAt(0L);
        return entity;
    }

    private static Set<String> namesOf(List<Split> splits) {
        Set<String> names = new HashSet<>();
        for (Split split : splits) {
            names.add(split.name);
        }
        return names;
    }

    /**
     * Returns a string of exactly {@code length} chars that starts with {@code prefix} and is padded with 'x'.
     */
    private static String filler(String prefix, int length) {
        assertTrue(prefix.length() <= length);
        String filled = prefix + Utils.repeat("x", length - prefix.length());
        assertEquals(length, filled.length());
        return filled;
    }

    private static final class CapturingPrinter implements LogPrinter {
        private final List<String> all = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        @Override
        public synchronized void v(String tag, String msg, Throwable tr) {
            all.add(msg);
        }

        @Override
        public synchronized void d(String tag, String msg, Throwable tr) {
            all.add(msg);
        }

        @Override
        public synchronized void i(String tag, String msg, Throwable tr) {
            all.add(msg);
        }

        @Override
        public synchronized void w(String tag, String msg, Throwable tr) {
            all.add(msg);
            warnings.add(msg);
        }

        @Override
        public synchronized void e(String tag, String msg, Throwable tr) {
            all.add(msg);
        }

        @Override
        public synchronized void wtf(String tag, String msg, Throwable tr) {
            all.add(msg);
        }

        synchronized List<String> allMessages() {
            return new ArrayList<>(all);
        }

        synchronized List<String> warnings() {
            return new ArrayList<>(warnings);
        }
    }
}
