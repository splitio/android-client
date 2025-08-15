package io.split.android.client.storage.db;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

public class SplitMapInitializationTaskTest {

    @Mock
    private SplitQueryDaoImpl mMockDao;

    private Object mLock;
    private SplitMapInitializationTask initializationTask;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLock = new Object();
        initializationTask = new SplitMapInitializationTask(mMockDao, mLock);
    }
    
    @Test
    public void testSuccessfulInitialization() {
        Map<String, SplitEntity> expectedResult = new HashMap<>();
        SplitEntity entity1 = new SplitEntity();
        entity1.setName("split1");
        entity1.setBody("body1");
        expectedResult.put("split1", entity1);
        
        when(mMockDao.loadSplitsMap()).thenReturn(expectedResult);
        
        initializationTask.run();
        
        verify(mMockDao).loadSplitsMap();
        verify(mMockDao).setCachedSplitsMap(expectedResult);
        verify(mMockDao).setInitialized(true);
    }
    
    @Test
    public void testInitializationWithEmptyResult() {
        Map<String, SplitEntity> emptyResult = new HashMap<>();
        when(mMockDao.loadSplitsMap()).thenReturn(emptyResult);
        
        initializationTask.run();
        
        verify(mMockDao).loadSplitsMap();
        verify(mMockDao).setCachedSplitsMap(emptyResult);
        verify(mMockDao).setInitialized(true);
    }
    
    @Test
    public void testInitializationFailure() {
        RuntimeException testException = new RuntimeException("Database error");
        when(mMockDao.loadSplitsMap()).thenThrow(testException);
        
        initializationTask.run();
        
        verify(mMockDao).loadSplitsMap();
        verify(mMockDao).setCachedSplitsMap(argThat(map -> map != null && map.isEmpty()));
        verify(mMockDao).setInitialized(true);
    }
    
    @Test
    public void testInitializationWithNullResult() {
        when(mMockDao.loadSplitsMap()).thenReturn(null);
        
        initializationTask.run();
        
        verify(mMockDao).loadSplitsMap();
        verify(mMockDao).setCachedSplitsMap(null);
        verify(mMockDao).setInitialized(true);
    }
    
    @Test
    public void testSynchronizationBehavior() throws InterruptedException {
        Map<String, SplitEntity> result = new HashMap<>();
        when(mMockDao.loadSplitsMap()).thenReturn(result);
        
        Object testLock = new Object();
        SplitMapInitializationTask taskWithTestLock = new SplitMapInitializationTask(mMockDao, testLock);
        
        boolean[] synchronizedBlockEntered = {false};
        
        Thread waitingThread = new Thread(() -> {
            synchronized (testLock) {
                try {
                    testLock.wait(1000); // Wait for notification
                    synchronizedBlockEntered[0] = true;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        
        waitingThread.start();
        Thread.sleep(100); // Give waiting thread time to start waiting
        
        taskWithTestLock.run();
        
        waitingThread.join(2000);
        
        assertTrue("Synchronized block should have been entered", synchronizedBlockEntered[0]);
        verify(mMockDao).setCachedSplitsMap(result);
        verify(mMockDao).setInitialized(true);
    }
    
    @Test
    public void testMultipleEntitiesInitialization() {
        Map<String, SplitEntity> result = new HashMap<>();
        
        for (int i = 1; i <= 5; i++) {
            SplitEntity entity = new SplitEntity();
            entity.setName("split" + i);
            entity.setBody("body" + i);
            result.put("split" + i, entity);
        }
        
        when(mMockDao.loadSplitsMap()).thenReturn(result);
        
        initializationTask.run();
        
        verify(mMockDao).loadSplitsMap();
        verify(mMockDao).setCachedSplitsMap(result);
        verify(mMockDao).setInitialized(true);
    }
    
    @Test
    public void testExceptionInFinally() {
        Map<String, SplitEntity> result = new HashMap<>();
        when(mMockDao.loadSplitsMap()).thenReturn(result);
        
        doThrow(new RuntimeException("Setter error")).when(mMockDao).setCachedSplitsMap(any());
        
        try {
            initializationTask.run();
        } catch (Exception e) {
            fail("Exception should be handled gracefully in finally block: " + e.getMessage());
        }
        
        verify(mMockDao).setCachedSplitsMap(result);
        verify(mMockDao).setInitialized(true);
    }
}
