package io.split.android.client.storage.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static java.util.Arrays.copyOfRange;

import android.database.Cursor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.OngoingStubbing;

import io.split.android.client.utils.logger.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SplitQueryDaoImplTest {

    private static final String MAIN_SQL = "SELECT name, body FROM splits WHERE length(CAST(body AS BLOB)) <= ?";
    private static final String OVERSIZE_SQL = "SELECT name, length(CAST(body AS BLOB)) FROM splits WHERE length(CAST(body AS BLOB)) > ?";
    private static final String PREFILL_THREAD_NAME = "SplitMapPrefill";

    @Mock
    private SplitRoomDatabase mockDatabase;
    
    @Mock
    private Cursor mockCursor;

    private SplitQueryDaoImpl splitQueryDao;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void shouldInitializeSuccessfully() {
        setupMockCursor(new String[]{"split1", "split2"}, new String[]{"body1", "body2"});
        when(mockDatabase.query(anyString(), any())).thenReturn(mockCursor);

        splitQueryDao = new SplitQueryDaoImpl(mockDatabase);

        assertNotNull("SplitQueryDao should be initialized", splitQueryDao);
        // Allow some time for background initialization
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void shouldReturnAllSplitsAsMap() {
        setupMockCursor(new String[]{"split1", "split2"}, new String[]{"body1", "body2"});
        when(mockDatabase.query(anyString(), any())).thenReturn(mockCursor);
        splitQueryDao = new SplitQueryDaoImpl(mockDatabase);

        // Wait for initialization
        waitForInitialization();

        Map<String, SplitEntity> result = splitQueryDao.getAllAsMap();

        assertNotNull("Result should not be null", result);
        assertEquals("Should return 2 splits", 2, result.size());
        assertTrue("Should contain split1", result.containsKey("split1"));
        assertTrue("Should contain split2", result.containsKey("split2"));
        assertEquals("Split1 body should match", "body1", result.get("split1").getBody());
        assertEquals("Split2 body should match", "body2", result.get("split2").getBody());
    }

    @Test
    public void shouldReturnEmptyMapWhenNoSplits() {
        setupMockCursor(new String[]{}, new String[]{});
        when(mockDatabase.query(anyString(), any())).thenReturn(mockCursor);
        splitQueryDao = new SplitQueryDaoImpl(mockDatabase);

        // Wait for initialization
        waitForInitialization();

        Map<String, SplitEntity> result = splitQueryDao.getAllAsMap();

        assertNotNull("Result should not be null", result);
        assertTrue("Result should be empty", result.isEmpty());
    }

    @Test
    public void shouldHandleDatabaseError() {
        when(mockDatabase.query(anyString(), any())).thenThrow(new RuntimeException("Database error"));
        splitQueryDao = new SplitQueryDaoImpl(mockDatabase);

        // Wait for initialization
        waitForInitialization();

        Map<String, SplitEntity> result = splitQueryDao.getAllAsMap();

        assertNotNull("Result should not be null", result);
        assertTrue("Result should be empty when error occurs", result.isEmpty());
    }

    @Test
    public void shouldInvalidateCache() {
        setupMockCursor(new String[]{"split1"}, new String[]{"body1"});
        when(mockDatabase.query(anyString(), any())).thenReturn(mockCursor);
        splitQueryDao = new SplitQueryDaoImpl(mockDatabase);

        // Wait for initialization
        waitForInitialization();

        // Get initial result
        Map<String, SplitEntity> initialResult = splitQueryDao.getAllAsMap();
        assertFalse("Initial result should not be empty", initialResult.isEmpty());

        splitQueryDao.invalidate();

        // Setup new mock data for subsequent calls
        setupMockCursor(new String[]{"split2"}, new String[]{"body2"});

        Map<String, SplitEntity> resultAfterInvalidation = splitQueryDao.getAllAsMap();
        assertNotNull("Result after invalidation should not be null", resultAfterInvalidation);
    }

    @Test
    public void shouldReturnCopyOfMap() {
        setupMockCursor(new String[]{"split1"}, new String[]{"body1"});
        when(mockDatabase.query(anyString(), any())).thenReturn(mockCursor);
        splitQueryDao = new SplitQueryDaoImpl(mockDatabase);

        waitForInitialization();

        Map<String, SplitEntity> result1 = splitQueryDao.getAllAsMap();
        Map<String, SplitEntity> result2 = splitQueryDao.getAllAsMap();

        assertNotNull("First result should not be null", result1);
        assertNotNull("Second result should not be null", result2);
        assertEquals("Both results should have same content", result1.size(), result2.size());
        result1.clear();
        assertFalse("Second result should not be affected by clearing first", result2.isEmpty());
    }

    @Test
    public void shouldHandleCursorCloseException() {
        setupMockCursor(new String[]{"split1"}, new String[]{"body1"});
        when(mockDatabase.query(anyString(), any())).thenReturn(mockCursor);
        doThrow(new RuntimeException("Cursor close error")).when(mockCursor).close();

        splitQueryDao = new SplitQueryDaoImpl(mockDatabase);

        // Wait for initialization to complete (even with error)
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Should not crash and should return result from direct load
        Map<String, SplitEntity> result = splitQueryDao.getAllAsMap();
        assertNotNull("Result should not be null", result);
    }

    @Test
    public void shouldGetColumnIndexCorrectly() {
        setupMockCursor(new String[]{"split1"}, new String[]{"body1"});
        when(mockDatabase.query(anyString(), any())).thenReturn(mockCursor);
        splitQueryDao = new SplitQueryDaoImpl(mockDatabase);

        when(mockCursor.getColumnIndex("name")).thenReturn(0);
        int nameIndex = splitQueryDao.getColumnIndexOrThrow(mockCursor, "name");

        assertEquals("Should return correct column index", 0, nameIndex);
    }

    @Test
    public void shouldHandleLargeBatchProcessing() {
        String[] names = new String[250]; // More than BATCH_SIZE (100)
        String[] bodies = new String[250];
        for (int i = 0; i < 250; i++) {
            names[i] = "split" + i;
            bodies[i] = "body" + i;
        }
        
        setupMockCursor(names, bodies);
        when(mockDatabase.query(anyString(), any())).thenReturn(mockCursor);
        splitQueryDao = new SplitQueryDaoImpl(mockDatabase);

        waitForInitialization();

        Map<String, SplitEntity> result = splitQueryDao.getAllAsMap();

        assertNotNull("Result should not be null", result);
        assertEquals("Should return all 250 splits", 250, result.size());
        assertTrue("Should contain first split", result.containsKey("split0"));
        assertTrue("Should contain last split", result.containsKey("split249"));
    }

    @Test
    public void shouldWaitForInitializationWhenAccessedImmediately() {
        setupMockCursor(new String[]{"split1"}, new String[]{"body1"});
        when(mockDatabase.query(anyString(), any())).thenReturn(mockCursor);

        splitQueryDao = new SplitQueryDaoImpl(mockDatabase);
        Map<String, SplitEntity> result = splitQueryDao.getAllAsMap();

        assertNotNull("Result should not be null", result);
        // The method should either return the initialized map or load directly
        verify(mockDatabase, times(1)).query(eq(MAIN_SQL), any());
    }

    @Test
    public void threadsShouldNotBlock() throws InterruptedException {
        setupMockCursor(new String[]{"split1"}, new String[]{"body1"});
        when(mockDatabase.query(anyString(), any())).thenReturn(mockCursor);
        splitQueryDao = new SplitQueryDaoImpl(mockDatabase);

        // Create a thread that will be interrupted while waiting
        Thread testThread = new Thread(() -> {
            Thread.currentThread().interrupt(); // Set interrupt flag
            Map<String, SplitEntity> result = splitQueryDao.getAllAsMap();
            assertNotNull("Result should not be null even when interrupted", result);
        });

        testThread.start();
        testThread.join(1000); // Wait up to 1 second

        assertFalse("Test thread should have completed", testThread.isAlive());
    }

    @Test
    public void getAllAsMapDoesNotWaitForPrefillWhenInvalidated() throws Exception {
        Map<String, String> table = new LinkedHashMap<>();
        table.put("f", "v1");
        CountDownLatch release = createDaoWithBlockedPrefill(table);

        splitQueryDao.invalidate();
        long start = System.nanoTime();
        Map<String, SplitEntity> result = splitQueryDao.getAllAsMap();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        release.countDown();

        assertTrue("getAllAsMap waited for the prefill: " + elapsedMs + "ms", elapsedMs < 1000);
        assertEquals("v1", result.get("f").getBody());
    }

    @Test
    public void prefillResultDiscardedWhenInvalidatedMidLoad() throws Exception {
        Map<String, String> table = new LinkedHashMap<>();
        table.put("f", "v1");
        CountDownLatch release = createDaoWithBlockedPrefill(table);

        splitQueryDao.invalidate();
        synchronized (table) {
            table.put("f", "v2");
        }
        assertEquals("v2", splitQueryDao.getAllAsMap().get("f").getBody());

        release.countDown();
        joinPrefillThread();

        assertEquals("prefill result must not replace data written after invalidate",
                "v2", splitQueryDao.getAllAsMap().get("f").getBody());
        synchronized (table) {
            table.put("f", "v3");
        }
        assertEquals("nothing may be cached after invalidate",
                "v3", splitQueryDao.getAllAsMap().get("f").getBody());
    }

    @Test
    public void prefillWakesAWaitingGetAllAsMapWithoutWaitingForTheTimeout() throws Exception {
        Map<String, String> table = new LinkedHashMap<>();
        table.put("f", "v1");
        CountDownLatch release = createDaoWithBlockedPrefill(table);

        AtomicReference<Map<String, SplitEntity>> result = new AtomicReference<>();
        Thread waiter = new Thread(() -> result.set(splitQueryDao.getAllAsMap()));
        waiter.start();
        awaitWaiterBlocked(waiter);
        long start = System.nanoTime();
        release.countDown();
        waiter.join(10000);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertFalse("waiter still blocked", waiter.isAlive());
        assertTrue("waiter was not woken by the prefill: " + elapsedMs + "ms", elapsedMs < 2000);
        assertEquals("v1", result.get().get("f").getBody());
    }

    @Test
    public void oversizedRowIsLeftOutAndNormalRowsAreLoaded() throws InterruptedException {
        stubQueriesWithOversizedRow(new String[]{"split1", "split2"}, new String[]{"body1", "body2"},
                "big_flag", 1_500_000L);
        splitQueryDao = new SplitQueryDaoImpl(mockDatabase);
        joinPrefillThread();

        Map<String, SplitEntity> result = splitQueryDao.getAllAsMap();

        assertEquals("Should return only the 2 normal splits", 2, result.size());
        assertEquals("body1", result.get("split1").getBody());
        assertEquals("body2", result.get("split2").getBody());
        assertFalse("Oversized split must not be in the map", result.containsKey("big_flag"));
        verify(mockDatabase, atLeastOnce()).query(eq(MAIN_SQL), capArgs());
        verify(mockDatabase, atLeastOnce()).query(eq(OVERSIZE_SQL), capArgs());
    }

    @Test
    public void oversizedRowLogsOneWarningWithNameAndSizeAndNoBody() throws InterruptedException {
        CountDownLatch release = new CountDownLatch(1);
        stubQueriesWithOversizedRow(release, new String[]{"split1"}, new String[]{"body1"},
                new String[]{"big_flag"}, new long[]{1_500_000L});
        try (MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            splitQueryDao = new SplitQueryDaoImpl(mockDatabase);
            try {
                // Forces the direct load on this thread, where the static Logger mock applies
                splitQueryDao.invalidate();
                splitQueryDao.getAllAsMap();

                ArgumentCaptor<String> warning = ArgumentCaptor.forClass(String.class);
                logger.verify(() -> Logger.w(warning.capture()), times(1));
                String message = warning.getValue();
                assertTrue("warning should name the flag: " + message, message.contains("big_flag"));
                assertTrue("warning should carry the size: " + message, message.contains("1500000"));
                assertFalse("warning must not carry the body: " + message, message.contains("body"));
            } finally {
                release.countDown();
                joinPrefillThread();
            }
        }
    }

    @Test
    public void oversizedRowWarningIsLoggedOnlyOncePerDaoAcrossRepeatedLoads() throws InterruptedException {
        CountDownLatch release = new CountDownLatch(1);
        stubQueriesWithOversizedRow(release, new String[]{"split1"}, new String[]{"body1"},
                new String[]{"big_flag"}, new long[]{1_500_000L});
        try (MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            splitQueryDao = new SplitQueryDaoImpl(mockDatabase);
            try {
                // Each call after invalidate() reloads the table, so each load sees the same oversized row
                splitQueryDao.invalidate();
                splitQueryDao.getAllAsMap();
                splitQueryDao.getAllAsMap();

                logger.verify(() -> Logger.w(anyString()), times(1));
            } finally {
                release.countDown();
                joinPrefillThread();
            }
        }
    }

    @Test
    public void rowsReadBeforeMidIterationFaultAreReturnedAndNotCached() throws Exception {
        List<String[]> rows = rowsOf(200);
        AtomicBoolean faulty = new AtomicBoolean(true);
        List<Cursor> mainCursors = Collections.synchronizedList(new ArrayList<>());
        when(mockDatabase.query(anyString(), any())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.equals(OVERSIZE_SQL)) {
                return cursorOver(new ArrayList<>(), -1);
            }
            Cursor cursor = cursorOver(rows, faulty.get() ? 150 : -1);
            mainCursors.add(cursor);
            return cursor;
        });
        splitQueryDao = new SplitQueryDaoImpl(mockDatabase);
        joinPrefillThread();

        Map<String, SplitEntity> faultyResult = splitQueryDao.getAllAsMap();

        assertEquals("Should return the 150 rows read before the fault", 150, faultyResult.size());
        assertTrue("Should contain the first row", faultyResult.containsKey("split0"));
        assertTrue("Should contain the row read just before the fault", faultyResult.containsKey("split149"));
        assertFalse("Should not contain the row the fault replaced", faultyResult.containsKey("split150"));

        faulty.set(false);
        Map<String, SplitEntity> healthyResult = splitQueryDao.getAllAsMap();

        assertEquals("A failed load must not be cached: the fault-free call re-queries all 200 rows",
                200, healthyResult.size());

        assertFalse("Main cursors should have been opened", mainCursors.isEmpty());
        for (Cursor cursor : mainCursors) {
            verify(cursor, atLeastOnce()).close();
        }
    }

    @Test
    public void failingOversizeProbeDoesNotAffectMainResultAndIsLogged() {
        when(mockDatabase.query(anyString(), any())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.equals(OVERSIZE_SQL)) {
                throw new RuntimeException("probe failed");
            }
            return cursorOver(rowsOf(new String[]{"split1", "split2"}, new String[]{"body1", "body2"}), -1);
        });
        try (MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            splitQueryDao = new SplitQueryDaoImpl(mockDatabase);
            splitQueryDao.invalidate();

            Map<String, SplitEntity> result = splitQueryDao.getAllAsMap();

            assertEquals("Probe failure must not change the main result", 2, result.size());
            assertEquals("body1", result.get("split1").getBody());
            logger.verify(() -> Logger.e(anyString()), atLeastOnce());
        }
    }

    @Test
    public void afterInvalidateEachGetAllAsMapQueriesAndNothingIsCached() throws Exception {
        Map<String, String> table = new LinkedHashMap<>();
        table.put("f", "v1");
        when(mockDatabase.query(anyString(), any())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.equals(OVERSIZE_SQL)) {
                return cursorOver(new ArrayList<>(), -1);
            }
            List<String[]> rows = new ArrayList<>();
            synchronized (table) {
                for (Map.Entry<String, String> e : table.entrySet()) {
                    rows.add(new String[]{e.getKey(), e.getValue()});
                }
            }
            return cursorOver(rows, -1);
        });
        splitQueryDao = new SplitQueryDaoImpl(mockDatabase);
        joinPrefillThread();
        clearInvocations(mockDatabase);

        splitQueryDao.invalidate();
        assertEquals("v1", splitQueryDao.getAllAsMap().get("f").getBody());
        assertEquals("v1", splitQueryDao.getAllAsMap().get("f").getBody());
        verify(mockDatabase, times(2)).query(eq(MAIN_SQL), any());

        synchronized (table) {
            table.put("f", "v2");
        }
        assertEquals("A call after invalidate must read the table, not a cache", "v2",
                splitQueryDao.getAllAsMap().get("f").getBody());
    }

    @Test
    public void eachOversizedRowLogsItsOwnWarningWithNameAndSizeAndNoBody() throws InterruptedException {
        CountDownLatch release = new CountDownLatch(1);
        stubQueriesWithOversizedRow(release, new String[]{"split1"}, new String[]{"body1"},
                new String[]{"big_a", "big_b"}, new long[]{1_500_000L, 2_000_000L});
        try (MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            splitQueryDao = new SplitQueryDaoImpl(mockDatabase);
            try {
                splitQueryDao.invalidate();
                splitQueryDao.getAllAsMap();

                ArgumentCaptor<String> warnings = ArgumentCaptor.forClass(String.class);
                logger.verify(() -> Logger.w(warnings.capture()), times(2));
                List<String> messages = warnings.getAllValues();
                String first = messages.get(0);
                String second = messages.get(1);
                assertTrue("first warning should name big_a and its size: " + first,
                        first.contains("big_a") && first.contains("1500000"));
                assertTrue("second warning should name big_b and its size: " + second,
                        second.contains("big_b") && second.contains("2000000"));
                for (String message : messages) {
                    assertFalse("warning must not carry the body: " + message, message.contains("body"));
                }
            } finally {
                release.countDown();
                joinPrefillThread();
            }
        }
    }

    /**
     * Routes by SQL: the capped main query returns the normal rows, the oversize probe
     * returns one row with name and length(CAST(body AS BLOB)) only.
     */
    private void stubQueriesWithOversizedRow(String[] names, String[] bodies,
                                             String oversizedName, long oversizedSize) {
        stubQueriesWithOversizedRow(new CountDownLatch(0), names, bodies,
                new String[]{oversizedName}, new long[]{oversizedSize});
    }

    /**
     * Same routing as above, but every query made on the prefill thread first waits for
     * {@code prefillRelease}, so the prefill cannot probe the oversized rows while the test thread
     * loads them. Release it after the assertions.
     */
    private void stubQueriesWithOversizedRow(CountDownLatch prefillRelease, String[] names, String[] bodies,
                                             String[] oversizedNames, long[] oversizedSizes) {
        when(mockDatabase.query(anyString(), any())).thenAnswer(invocation -> {
            if (PREFILL_THREAD_NAME.equals(Thread.currentThread().getName())) {
                prefillRelease.await();
            }
            String sql = invocation.getArgument(0);
            if (sql.equals(OVERSIZE_SQL)) {
                return sizeCursorOver(oversizedNames, oversizedSizes);
            }
            return cursorOver(rowsOf(names, bodies), -1);
        });
    }

    /** Matches the cap argument: exactly one bind value, MAX_BODY_BYTES (1_000_000). */
    private static Object[] capArgs() {
        return argThat((Object[] args) ->
                args != null && args.length == 1 && Integer.valueOf(1_000_000).equals(args[0]));
    }

    private static List<String[]> rowsOf(String[] names, String[] bodies) {
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            rows.add(new String[]{names[i], bodies[i]});
        }
        return rows;
    }

    private static List<String[]> rowsOf(int count) {
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            rows.add(new String[]{"split" + i, "body" + i});
        }
        return rows;
    }

    /**
     * Cursor over name/body rows. With throwAfter >= 0, moveToNext() returns true throwAfter
     * times and then throws, like a CursorWindow overflow mid-iteration. -1 means never throw.
     */
    private static Cursor cursorOver(List<String[]> rows, int throwAfter) {
        Cursor cursor = mock(Cursor.class);
        when(cursor.getColumnIndex("name")).thenReturn(0);
        when(cursor.getColumnIndex("body")).thenReturn(1);
        final int[] pos = {-1};
        when(cursor.moveToNext()).thenAnswer(invocation -> {
            pos[0]++;
            if (throwAfter >= 0 && pos[0] == throwAfter) {
                throw new RuntimeException("Row too big to fit into CursorWindow");
            }
            return pos[0] < rows.size();
        });
        when(cursor.getString(0)).thenAnswer(invocation -> rows.get(pos[0])[0]);
        when(cursor.getString(1)).thenAnswer(invocation -> rows.get(pos[0])[1]);
        return cursor;
    }

    /** Cursor for SELECT name, length(CAST(body AS BLOB)) over several rows: name at column 0, size at column 1. */
    private static Cursor sizeCursorOver(String[] names, long[] sizes) {
        Cursor cursor = mock(Cursor.class);
        when(cursor.getColumnIndex("name")).thenReturn(0);
        final int[] pos = {-1};
        when(cursor.moveToNext()).thenAnswer(invocation -> ++pos[0] < names.length);
        when(cursor.getString(0)).thenAnswer(invocation -> names[pos[0]]);
        when(cursor.getLong(1)).thenAnswer(invocation -> sizes[pos[0]]);
        return cursor;
    }

    private void joinPrefillThread() throws InterruptedException {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (PREFILL_THREAD_NAME.equals(t.getName())) {
                t.join(5000);
            }
        }
    }

    /**
     * Stubs a table holding one flag whose prefill blocks until the returned latch is released,
     * then creates the DAO (which starts that prefill).
     */
    private CountDownLatch createDaoWithBlockedPrefill(Map<String, String> table) {
        CountDownLatch release = new CountDownLatch(1);
        stubQueryOverTable(table, release);
        splitQueryDao = new SplitQueryDaoImpl(mockDatabase);
        return release;
    }

    /** Polls up to 1 s until the waiter thread is parked on the prefill's monitor. */
    private static void awaitWaiterBlocked(Thread waiter) throws InterruptedException {
        long deadline = System.nanoTime() + 1_000_000_000L;
        while (!isParked(waiter.getState()) && System.nanoTime() < deadline) {
            Thread.sleep(5);
        }
    }

    private static boolean isParked(Thread.State state) {
        return state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING;
    }

    /**
     * Each query() snapshots the table first and then (only on the prefill thread)
     * blocks on the latch, so the prefill holds stale data.
     */
    private void stubQueryOverTable(Map<String, String> table, CountDownLatch firstCallLatch) {
        when(mockDatabase.query(anyString(), any())).thenAnswer(invocation -> {
            final List<String[]> rows = new ArrayList<>();
            synchronized (table) {
                for (Map.Entry<String, String> e : table.entrySet()) {
                    rows.add(new String[]{e.getKey(), e.getValue()});
                }
            }
            if (PREFILL_THREAD_NAME.equals(Thread.currentThread().getName())) {
                firstCallLatch.await();
            }
            return cursorOver(rows, -1);
        });
    }

    private void setupMockCursor(String[] names, String[] bodies) {
        when(mockCursor.getColumnIndex("name")).thenReturn(0);
        when(mockCursor.getColumnIndex("body")).thenReturn(1);
        
        // Cursor movement
        if (names.length == 0) {
            when(mockCursor.moveToNext()).thenReturn(false);
        } else {
            Boolean[] moveResults = new Boolean[names.length + 1];
            for (int i = 0; i < names.length; i++) {
                moveResults[i] = true;
            }
            moveResults[names.length] = false; // Final call returns false
            
            when(mockCursor.moveToNext()).thenReturn(moveResults[0], copyOfRange(moveResults, 1, moveResults.length));
        }

        if (names.length > 0) {
            // Set up getString(0) for name column
            OngoingStubbing<String> nameStubbing = when(mockCursor.getString(0));
            for (String name : names) {
                nameStubbing = nameStubbing.thenReturn(name);
            }
            
            // Set up getString(1) for body column
            OngoingStubbing<String> bodyStubbing = when(mockCursor.getString(1));
            for (String body : bodies) {
                bodyStubbing = bodyStubbing.thenReturn(body);
            }
        }
    }

    private void waitForInitialization() {
        try {
            Thread.sleep(200); // Give time for background initialization
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
