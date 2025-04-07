package io.split.android.client.storage.db;

import android.database.Cursor;
import android.os.Process;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.utils.logger.Logger;

public class SplitQueryDaoImpl implements SplitQueryDao {

    private final SplitRoomDatabase mDatabase;
    private volatile Map<String, String> mCachedSplitsMap;
    private final Object mLock = new Object();
    private boolean mIsInitialized = false;
    private Thread mInitializationThread;

    public SplitQueryDaoImpl(SplitRoomDatabase mDatabase) {
        this.mDatabase = mDatabase;
        
        // Start prefilling the map in a background thread
        mInitializationThread = new Thread(() -> {
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
            long startTime = System.currentTimeMillis();
            System.out.println("[SPLIT-PERF] SplitQueryDaoImpl: Starting background prefill of splits map");
            
            Map<String, String> result = loadSplitsMap();
            
            synchronized (mLock) {
                mCachedSplitsMap = result;
                mIsInitialized = true;
                mLock.notifyAll(); // Notify any waiting threads
            }
            
            long endTime = System.currentTimeMillis();
            System.out.println("[SPLIT-PERF] SplitQueryDaoImpl: Completed background prefill in " + (endTime - startTime) + 
                              "ms, loaded " + (result != null ? result.size() : 0) + " entries");
        });
        mInitializationThread.setName("SplitMapPrefill");
        mInitializationThread.start();
    }

    public List<SplitEntity> get(long rowIdFrom, int maxRows) {

        String sql =    "SELECT rowid, name, body, updated_at FROM splits WHERE rowId > ? ORDER BY rowId LIMIT ?";
        Object[] arguments = {rowIdFrom, maxRows};
        Cursor cursor = mDatabase.query(sql, arguments);

        try {
            final int rowIdIndex = getColumnIndexOrThrow(cursor, "rowid");
            final int nameIndex = getColumnIndexOrThrow(cursor, "name");
            final int bodyIndex = getColumnIndexOrThrow(cursor, "body");
            final int updatedAtIndex = getColumnIndexOrThrow(cursor, "updated_at");
            final List<SplitEntity> entities = new ArrayList<SplitEntity>(cursor.getCount());
            while (cursor.moveToNext()) {
                final SplitEntity item;
                item = new SplitEntity();
                item.setRowId(cursor.getLong(rowIdIndex));
                item.setName(cursor.getString(nameIndex));
                item.setBody(cursor.getString(bodyIndex));
                item.setUpdatedAt(cursor.getLong(updatedAtIndex));
                entities.add(item);
            }
            return entities;
        } catch (Exception e) {
            Logger.e("Error executing splits query: " + e.getLocalizedMessage());
        } finally {
            cursor.close();
        }
        return new ArrayList<>();
    }

    int getColumnIndexOrThrow(@NonNull Cursor c, @NonNull String name) {
        final int index = c.getColumnIndex(name);
        if (index >= 0) {
            return index;
        }
        return c.getColumnIndexOrThrow("`" + name + "`");
    }
    
    /**
     * Get all splits as a Map with name as key and body as value.
     * This is a more efficient way to get splits compared to Room's automatic entity mapping.
     * If the map has been prefilled, it returns immediately.
     */
    public Map<String, String> getAllAsMap() {
        // Fast path - if the map is already initialized, return it immediately
        if (mIsInitialized) {
            System.out.println("[SPLIT-PERF] SplitQueryDaoImpl.getAllAsMap: Using prefilled map with " + 
                              (mCachedSplitsMap != null ? mCachedSplitsMap.size() : 0) + " entries");
            return new HashMap<>(mCachedSplitsMap); // Return a copy to avoid concurrent modification issues
        }
        
        // Wait for initialization to complete if it's in progress
        synchronized (mLock) {
            if (mIsInitialized) {
                System.out.println("[SPLIT-PERF] SplitQueryDaoImpl.getAllAsMap: Using prefilled map after waiting");
                return new HashMap<>(mCachedSplitsMap);
            }
            
            // If initialization thread is running, wait for it
            if (mInitializationThread != null && mInitializationThread.isAlive()) {
                try {
                    System.out.println("[SPLIT-PERF] SplitQueryDaoImpl.getAllAsMap: Waiting for prefill to complete");
                    mLock.wait(5000); // Wait up to 5 seconds
                    
                    if (mIsInitialized) {
                        System.out.println("[SPLIT-PERF] SplitQueryDaoImpl.getAllAsMap: Prefill completed while waiting");
                        return new HashMap<>(mCachedSplitsMap);
                    } else {
                        System.out.println("[SPLIT-PERF] SplitQueryDaoImpl.getAllAsMap: Timeout waiting for prefill, loading directly");
                    }
                } catch (InterruptedException e) {
                    System.out.println("[SPLIT-PERF] SplitQueryDaoImpl.getAllAsMap: Interrupted while waiting for prefill");
                }
            }
            
            // If we get here, either initialization failed or timed out
            // Load the map directly
            System.out.println("[SPLIT-PERF] SplitQueryDaoImpl.getAllAsMap: Loading map directly");
            Map<String, String> result = loadSplitsMap();
            
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
    private Map<String, String> loadSplitsMap() {
        long startTime = System.currentTimeMillis();
        System.out.println("[SPLIT-PERF] SplitQueryDaoImpl.loadSplitsMap: Starting");
        
        String sql = "SELECT name, body FROM splits";
        long beforeQueryTime = System.currentTimeMillis();
        
        Cursor cursor = mDatabase.query(sql, null);
        long afterQueryTime = System.currentTimeMillis();
        System.out.println("[SPLIT-PERF] SplitQueryDaoImpl.loadSplitsMap: Query execution took " + (afterQueryTime - beforeQueryTime) + "ms");
        
        // Use an estimated initial capacity based on previous runs
        final int ESTIMATED_CAPACITY = 2000; // Slightly higher than your observed 1736 entries
        Map<String, String> result = new HashMap<>(ESTIMATED_CAPACITY);
        
        try {
            final int nameIndex = getColumnIndexOrThrow(cursor, "name");
            final int bodyIndex = getColumnIndexOrThrow(cursor, "body");

            int processedCount = 0;
            
            // Optimize cursor iteration by using a buffer
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
                
                // Process in batches for better cache locality
                if (batchCount == BATCH_SIZE) {
                    for (int i = 0; i < BATCH_SIZE; i++) {
                        result.put(names[i], bodies[i]);
                    }
                    batchCount = 0;
                }
            }
            
            // Process any remaining items
            for (int i = 0; i < batchCount; i++) {
                result.put(names[i], bodies[i]);
            }
            
            long cursorIterationEnd = System.currentTimeMillis();
            System.out.println("[SPLIT-PERF] SplitQueryDaoImpl.loadSplitsMap: Cursor iteration took " + 
                              (cursorIterationEnd - cursorIterationStart) + "ms for " + processedCount + " rows");
        } catch (Exception e) {
            Logger.e("Error executing loadSplitsMap query: " + e.getLocalizedMessage());
        } finally {
            cursor.close();
        }
        
        long endTime = System.currentTimeMillis();
        System.out.println("[SPLIT-PERF] SplitQueryDaoImpl.loadSplitsMap: Total execution time " + 
                          (endTime - startTime) + "ms, returned " + result.size() + " entries");
        
        return result;
    }
}
