package io.split.android.client.storage.db;

import android.database.Cursor;
import android.os.Process;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.SplitClientFactoryImpl;
import io.split.android.client.SplitFactoryImpl;
import io.split.android.client.utils.logger.Logger;

public class SplitQueryDaoImpl implements SplitQueryDao {

    private final SplitRoomDatabase mDatabase;
    private volatile Map<String, SplitEntity> mCachedSplitsMap;
    private final Object mLock = new Object();
    private boolean mIsInitialized = false;
    private final Thread mInitializationThread;

    public SplitQueryDaoImpl(SplitRoomDatabase mDatabase) {
        this.mDatabase = mDatabase;
        // Start prefilling the map in a background thread
        mInitializationThread = new Thread(() -> {
            try {
                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
            } catch (Exception ignore) {
                // Ignore
            }
            long startTime = System.currentTimeMillis();
            System.out.println(SplitFactoryImpl.StartupTimeTracker.getElapsedTimeLog("SplitQueryDaoImpl: Starting background prefill of splits map"));
            
            Map<String, SplitEntity> result = loadSplitsMap();
            
            synchronized (mLock) {
                mCachedSplitsMap = result;
                mIsInitialized = true;
                mLock.notifyAll(); // Notify any waiting threads
            }
            
            long endTime = System.currentTimeMillis();
            System.out.println(SplitFactoryImpl.StartupTimeTracker.getElapsedTimeLog("SplitQueryDaoImpl: Completed background prefill in " + (endTime - startTime) +
                              "ms, loaded " + (result != null ? result.size() : 0) + " entries"));
        });
        mInitializationThread.setName("SplitMapPrefill");
        mInitializationThread.start();
    }

    int getColumnIndexOrThrow(@NonNull Cursor c, @NonNull String name) {
        final int index = c.getColumnIndex(name);
        if (index >= 0) {
            return index;
        }
        return c.getColumnIndexOrThrow("`" + name + "`");
    }

    public Map<String, SplitEntity> getAllAsMap() {
        // Fast path - if the map is already initialized, return it immediately
        if (mIsInitialized) {
            System.out.println(SplitFactoryImpl.StartupTimeTracker.getElapsedTimeLog("SplitQueryDaoImpl.getAllAsMap: Using prefilled map with " +
                              (mCachedSplitsMap != null ? mCachedSplitsMap.size() : 0) + " entries"));
            return new HashMap<>(mCachedSplitsMap);
        }
        
        // Wait for initialization to complete if it's in progress
        synchronized (mLock) {
            if (mIsInitialized) {
                System.out.println(SplitFactoryImpl.StartupTimeTracker.getElapsedTimeLog("SplitQueryDaoImpl.getAllAsMap: Using prefilled map after waiting"));
                return new HashMap<>(mCachedSplitsMap);
            }
            
            // If initialization thread is running, wait for it
            if (mInitializationThread != null && mInitializationThread.isAlive()) {
                try {
                    System.out.println(SplitFactoryImpl.StartupTimeTracker.getElapsedTimeLog("SplitQueryDaoImpl.getAllAsMap: Waiting for prefill to complete"));
                    mLock.wait(5000); // Wait up to 5 seconds
                    
                    if (mIsInitialized) {
                        System.out.println(SplitFactoryImpl.StartupTimeTracker.getElapsedTimeLog("SplitQueryDaoImpl.getAllAsMap: Prefill completed while waiting"));
                        return new HashMap<>(mCachedSplitsMap);
                    } else {
                        System.out.println(SplitFactoryImpl.StartupTimeTracker.getElapsedTimeLog("SplitQueryDaoImpl.getAllAsMap: Timeout waiting for prefill, loading directly"));
                    }
                } catch (InterruptedException e) {
                    System.out.println(SplitFactoryImpl.StartupTimeTracker.getElapsedTimeLog("SplitQueryDaoImpl.getAllAsMap: Interrupted while waiting for prefill"));
                }
            }
            
            // If we get here, either initialization failed or timed out
            // Load the map directly
            System.out.println(SplitFactoryImpl.StartupTimeTracker.getElapsedTimeLog("SplitQueryDaoImpl.getAllAsMap: Loading map directly"));
            Map<String, SplitEntity> result = loadSplitsMap();
            
            // Cache the result for future calls
            mCachedSplitsMap = result;
            mIsInitialized = true;
            
            return new HashMap<>(result);
        }
    }
    
    /**
     * Internal method to load the splits map from the database.
     * This contains the actual loading logic separated from the caching/synchronization.
     */
    private Map<String, SplitEntity> loadSplitsMap() {
        long startTime = System.currentTimeMillis();
        System.out.println(SplitFactoryImpl.StartupTimeTracker.getElapsedTimeLog("SplitQueryDaoImpl.loadSplitsMap: Starting"));
        
        String sql = "SELECT name, body FROM splits";
        long beforeQueryTime = System.currentTimeMillis();
        
        Cursor cursor = mDatabase.query(sql, null);
        long afterQueryTime = System.currentTimeMillis();
        System.out.println(SplitFactoryImpl.StartupTimeTracker.getElapsedTimeLog("SplitQueryDaoImpl.loadSplitsMap: Query execution took " + (afterQueryTime - beforeQueryTime) + "ms"));

        final int ESTIMATED_CAPACITY = 2000;
        Map<String, SplitEntity> result = new HashMap<>(ESTIMATED_CAPACITY);
        
        try {
            final int nameIndex = getColumnIndexOrThrow(cursor, "name");
            final int bodyIndex = getColumnIndexOrThrow(cursor, "body");

            int processedCount = 0;
            
            final int BATCH_SIZE = 100;
            String[] names = new String[BATCH_SIZE];
            String[] bodies = new String[BATCH_SIZE];
            int batchCount = 0;
            
            long cursorIterationStart = System.currentTimeMillis();
            while (cursor.moveToNext()) {
                names[batchCount] = cursor.getString(nameIndex);
                bodies[batchCount] = cursor.getString(bodyIndex);
                batchCount++;
                processedCount++;

                // Process in batches
                if (batchCount == BATCH_SIZE) {
                    for (int i = 0; i < BATCH_SIZE; i++) {
                        SplitEntity entity = new SplitEntity();
                        entity.setName(names[i]);
                        entity.setBody(bodies[i]);
                        result.put(names[i], entity);
                    }
                    batchCount = 0;
                }
            }

            // Process any remaining items
            for (int i = 0; i < batchCount; i++) {
                SplitEntity entity = new SplitEntity();
                entity.setName(names[i]);
                entity.setBody(bodies[i]);
                result.put(names[i], entity);
            }
            
            long cursorIterationEnd = System.currentTimeMillis();
            System.out.println(SplitFactoryImpl.StartupTimeTracker.getElapsedTimeLog("SplitQueryDaoImpl.loadSplitsMap: Cursor iteration took " +
                              (cursorIterationEnd - cursorIterationStart) + "ms for " + processedCount + " rows"));
        } catch (Exception e) {
            Logger.e("Error executing loadSplitsMap query: " + e.getLocalizedMessage());
        } finally {
            cursor.close();
        }
        
        long endTime = System.currentTimeMillis();
        System.out.println(SplitFactoryImpl.StartupTimeTracker.getElapsedTimeLog("SplitQueryDaoImpl.loadSplitsMap: Total execution time " +
                          (endTime - startTime) + "ms, returned " + result.size() + " entries"));
        
        return result;
    }
}
