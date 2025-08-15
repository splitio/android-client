package io.split.android.client.storage.db;

import android.database.Cursor;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.utils.logger.Logger;

public class SplitQueryDaoImpl implements SplitQueryDao {

    private final SplitRoomDatabase mDatabase;
    private volatile Map<String, SplitEntity> mCachedSplitsMap;
    private final Object mLock = new Object();
    private final Thread mInitializationThread;
    private final SplitMapInitializationTask mInitializationTask;
    private boolean mIsInitialized = false;
    private boolean mIsInvalidated = false;

    public SplitQueryDaoImpl(SplitRoomDatabase database) {
        mDatabase = database;
        mInitializationTask = new SplitMapInitializationTask(this, mLock);

        // Start prefilling the map in a background thread
        mInitializationThread = new Thread(mInitializationTask);
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
        try {
            // Fast path - if the map is already initialized, return it immediately
            if (isValid() && !mCachedSplitsMap.isEmpty()) {
                return new HashMap<>(mCachedSplitsMap);
            }

            // Wait for initialization to complete if it's in progress
            synchronized (mLock) {
                if (isValid() && !mCachedSplitsMap.isEmpty()) {
                    return new HashMap<>(mCachedSplitsMap);
                }

                // If initialization thread is running, wait for it
                if (mInitializationThread != null && mInitializationThread.isAlive()) {
                    try {
                        mLock.wait(5000); // Wait up to 5 seconds

                        if (isValid()) {
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
        } catch (Exception e) {
            Logger.e("Failed to get splits map: " + e.getLocalizedMessage());
            return new HashMap<>();
        }
    }

    private boolean isValid() {
        return mIsInitialized && !mIsInvalidated;
    }

    @Override
    public void invalidate() {
        synchronized (mLock) {
            if (mCachedSplitsMap != null) {
                mCachedSplitsMap.clear();
            }
            mIsInvalidated = true;
            mLock.notifyAll();
            Logger.i("Invalidated preloaded flags");
        }
    }
    
    void setCachedSplitsMap(Map<String, SplitEntity> cachedSplitsMap) {
        mCachedSplitsMap = cachedSplitsMap;
    }

    void setInitialized(boolean initialized) {
        mIsInitialized = initialized;
    }

    /**
     * Get the initialization task for testing purposes.
     * Package-private for testing access.
     */
    SplitMapInitializationTask getInitializationTask() {
        return mInitializationTask;
    }

    /**
     * Internal method to load the splits map from the database.
     * This contains the actual loading logic separated from the caching/synchronization.
     */
    Map<String, SplitEntity> loadSplitsMap() {
        try {
            mDatabase.getOpenHelper().getWritableDatabase();

            mDatabase.splitDao().getAll().size();

            return getStringSplitEntityMap();
        } catch (Exception e) {
            Logger.e("Failed to ensure database initialization: " + e.getLocalizedMessage());
            return new HashMap<>();
        }
    }

    @NonNull
    private Map<String, SplitEntity> getStringSplitEntityMap() throws IllegalStateException {
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
