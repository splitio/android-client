package io.split.android.client.storage.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static java.util.Arrays.copyOfRange;

import android.database.Cursor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.OngoingStubbing;

import java.util.Map;

public class SplitQueryDaoImplTest {

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
        verify(mockDatabase, times(1)).query(anyString(), any());
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
