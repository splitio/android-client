package io.split.android.client.storage.db;

import android.database.Cursor;
import android.os.Process;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

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

            Map<String, SplitEntity> result = loadSplitsMap();
            
            synchronized (mLock) {
                mCachedSplitsMap = result;
                mIsInitialized = true;
                mLock.notifyAll(); // Notify any waiting threads
            }
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
        if (mIsInitialized && !mCachedSplitsMap.isEmpty()) {
            return new HashMap<>(mCachedSplitsMap);
        }
        
        // Wait for initialization to complete if it's in progress
        synchronized (mLock) {
            if (mIsInitialized && !mCachedSplitsMap.isEmpty()) {
                return new HashMap<>(mCachedSplitsMap);
            }
            
            // If initialization thread is running, wait for it
            if (mInitializationThread != null && mInitializationThread.isAlive()) {
                try {
                    mLock.wait(5000); // Wait up to 5 seconds
                    
                    if (mIsInitialized) {
                        return new HashMap<>(mCachedSplitsMap);
                    }
                } catch (InterruptedException e) {

                }
            }
            
            // If we get here, either initialization failed or timed out
            // Load the map directly
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
        final String sql = "SELECT name, body FROM splits";

        Cursor cursor = mDatabase.query(sql, null);

        final int ESTIMATED_CAPACITY = 2000;
        Map<String, SplitEntity> result = new HashMap<>(ESTIMATED_CAPACITY);
        
        try {
            final int nameIndex = getColumnIndexOrThrow(cursor, "name");
            final int bodyIndex = getColumnIndexOrThrow(cursor, "body");

            final int BATCH_SIZE = 100;
            String[] names = new String[BATCH_SIZE];
            String[] bodies = new String[BATCH_SIZE];
            int batchCount = 0;

            while (cursor.moveToNext()) {
                names[batchCount] = cursor.getString(nameIndex);
                bodies[batchCount] = cursor.getString(bodyIndex);
                batchCount++;

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
        } catch (Exception e) {
            Logger.e("Error executing loadSplitsMap query: " + e.getLocalizedMessage());
        } finally {
            cursor.close();
        }

        return result;
    }
}
