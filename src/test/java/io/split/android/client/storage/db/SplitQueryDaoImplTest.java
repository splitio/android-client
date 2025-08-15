package io.split.android.client.storage.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

public class SplitQueryDaoImplTest {

    @Mock
    private SplitRoomDatabase mockDatabase;
    
    @Mock
    private SplitDao mockSplitDao;
    
    private SplitQueryDaoImpl splitQueryDao;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mockDatabase.splitDao()).thenReturn(mockSplitDao);
        
        splitQueryDao = spy(new SplitQueryDaoImpl(mockDatabase));
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @Test
    public void testGetAllAsMapWithSuccessfulInitialization() throws InterruptedException {
        Map<String, SplitEntity> expectedResult = new HashMap<>();
        SplitEntity entity = new SplitEntity();
        entity.setName("test_split");
        entity.setBody("test_body");
        expectedResult.put("test_split", entity);
        
        doReturn(expectedResult).when(splitQueryDao).loadSplitsMap();
        
        Thread.sleep(200);
        
        Map<String, SplitEntity> result = splitQueryDao.getAllAsMap();
        
        assertNotNull("Result should not be null", result);
        assertEquals("Result should contain expected data", expectedResult.size(), result.size());
        assertTrue("Result should contain test_split", result.containsKey("test_split"));
        assertEquals("Split name should match", "test_split", result.get("test_split").getName());
    }
    
    @Test
    public void testGetAllAsMapWithFailedInitialization() throws InterruptedException {
        doThrow(new RuntimeException("Database error")).when(splitQueryDao).loadSplitsMap();
        
        Thread.sleep(200);
        
        Map<String, SplitEntity> result = splitQueryDao.getAllAsMap();
        
        assertNotNull("Result should not be null even after failed initialization", result);
        assertTrue("Result should be empty after failed initialization", result.isEmpty());
    }
    
    @Test
    public void testInvalidate() throws InterruptedException {
        Map<String, SplitEntity> initialResult = new HashMap<>();
        SplitEntity entity = new SplitEntity();
        entity.setName("test_split");
        initialResult.put("test_split", entity);
        
        doReturn(initialResult).when(splitQueryDao).loadSplitsMap();
        
        Thread.sleep(200);
        
        Map<String, SplitEntity> beforeInvalidate = splitQueryDao.getAllAsMap();
        assertFalse("Should have data before invalidate", beforeInvalidate.isEmpty());
        
        splitQueryDao.invalidate();
        
        Map<String, SplitEntity> afterInvalidate = splitQueryDao.getAllAsMap();
        assertNotNull("Result should not be null after invalidate", afterInvalidate);
    }

    @Test
    public void testSettersAndGetters() {
        Map<String, SplitEntity> testMap = new HashMap<>();
        SplitEntity entity = new SplitEntity();
        entity.setName("setter_test");
        testMap.put("setter_test", entity);
        
        splitQueryDao.setCachedSplitsMap(testMap);
        splitQueryDao.setInitialized(true);
        
        SplitMapInitializationTask task = splitQueryDao.getInitializationTask();
        assertNotNull("Initialization task should not be null", task);
        
        Map<String, SplitEntity> result = splitQueryDao.getAllAsMap();
        assertTrue("Result should contain setter test data", result.containsKey("setter_test"));
    }
    
    @Test
    public void testConcurrentAccess() throws InterruptedException {
        Map<String, SplitEntity> testData = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            SplitEntity entity = new SplitEntity();
            entity.setName("split_" + i);
            entity.setBody("body_" + i);
            testData.put("split_" + i, entity);
        }
        
        doReturn(testData).when(splitQueryDao).loadSplitsMap();
        
        Thread[] threads = new Thread[10];
        Map[] results = new Map[10];
        
        for (int i = 0; i < 10; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                results[index] = splitQueryDao.getAllAsMap();
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join(2000);
        }
        
        for (int i = 0; i < 10; i++) {
            assertNotNull("Result " + i + " should not be null", results[i]);
            assertEquals("All results should have same size", testData.size(), results[i].size());
        }
    }
}
