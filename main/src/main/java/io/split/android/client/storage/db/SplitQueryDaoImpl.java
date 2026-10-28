package io.split.android.client.storage.db;

import android.database.Cursor;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.split.android.client.utils.logger.Logger;

public class SplitQueryDaoImpl implements SplitQueryDao {

    // Rows whose body is larger than this many bytes (UTF-8) are skipped: a larger body does not fit in a CursorWindow
    private static final int MAX_BODY_BYTES = 1_000_000;

    private final SplitRoomDatabase mDatabase;
    // Guarded by mLock
    private Map<String, SplitEntity> mCachedSplitsMap;
    private final Object mLock = new Object();
    // Names of oversized rows already warned about by this DAO; each is logged at most once
    private final Set<String> mWarnedOversizedNames = ConcurrentHashMap.newKeySet();
    private final Thread mInitializationThread;
    private boolean mIsInitialized = false;
    private boolean mIsInvalidated = false;

    /**
     * Outcome of a load. A failed load keeps the rows read before the failure, but must not be cached.
     */
    private static final class LoadResult {
        final Map<String, SplitEntity> map;
        final boolean failed;

        LoadResult(Map<String, SplitEntity> map, boolean failed) {
            this.map = map;
            this.failed = failed;
        }
    }

    public SplitQueryDaoImpl(SplitRoomDatabase mDatabase) {
        this.mDatabase = mDatabase;
        // Start prefilling the map in a background thread
        mInitializationThread = new Thread(() -> {
            LoadResult result = loadSplitsResult();

            synchronized (mLock) {
                if (!mIsInvalidated && !result.failed) {
                    mCachedSplitsMap = result.map;
                    mIsInitialized = true;
                }
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
        synchronized (mLock) {
            if (mIsInvalidated) {
                // Writes happened: read the table, never use or fill the prefill.
                return new HashMap<>(loadSplitsMap());
            }

            if (isValid() && !mCachedSplitsMap.isEmpty()) {
                return new HashMap<>(mCachedSplitsMap);
            }

            // If initialization thread is running, wait for it
            if (mInitializationThread != null && mInitializationThread.isAlive()) {
                try {
                    mLock.wait(5000); // Wait up to 5 seconds

                    if (mIsInvalidated) {
                        return new HashMap<>(loadSplitsMap());
                    }
                    if (isValid()) {
                        return new HashMap<>(mCachedSplitsMap);
                    }
                } catch (InterruptedException e) {

                }
            }

            // If we get here, either initialization failed or timed out
            // Load the map directly
            LoadResult loaded = loadSplitsResult();

            // A failed load (possibly partial) is returned to this caller only, never cached
            if (mIsInvalidated || loaded.failed) {
                return new HashMap<>(loaded.map);
            }

            // Cache the result for future calls
            mCachedSplitsMap = loaded.map;
            mIsInitialized = true;

            return new HashMap<>(loaded.map);
        }
    }

    private boolean isValid() {
        return mIsInitialized && !mIsInvalidated && mCachedSplitsMap != null;
    }

    @Override
    public void invalidate() {
        synchronized (mLock) {
            mCachedSplitsMap = null;
            mIsInvalidated = true;
            mLock.notifyAll();
            Logger.i("Invalidated preloaded flags");
        }
    }
    
    /**
     * Internal method to load the splits map from the database.
     * This contains the actual loading logic separated from the caching/synchronization.
     */
    private Map<String, SplitEntity> loadSplitsMap() {
        return loadSplitsResult().map;
    }

    private LoadResult loadSplitsResult() {
        final String sql = "SELECT name, body FROM splits WHERE length(CAST(body AS BLOB)) <= ?";

        final int ESTIMATED_CAPACITY = 2000;
        final Map<String, SplitEntity> result = new HashMap<>(ESTIMATED_CAPACITY);

        final int BATCH_SIZE = 100;
        final String[] names = new String[BATCH_SIZE];
        final String[] bodies = new String[BATCH_SIZE];
        int batchCount = 0;
        boolean failed = false;

        Cursor cursor = null;
        try {
            cursor = mDatabase.query(sql, new Object[]{MAX_BODY_BYTES});

            final int nameIndex = getColumnIndexOrThrow(cursor, "name");
            final int bodyIndex = getColumnIndexOrThrow(cursor, "body");

            while (cursor.moveToNext()) {
                names[batchCount] = cursor.getString(nameIndex);
                bodies[batchCount] = cursor.getString(bodyIndex);
                batchCount++;

                // Process in batches
                if (batchCount == BATCH_SIZE) {
                    putBatch(result, names, bodies, batchCount);
                    batchCount = 0;
                }
            }
        } catch (Exception e) {
            failed = true;
            Logger.e("Error executing loadSplitsMap query: " + e.getLocalizedMessage());
        } finally {
            // Keep the rows read so far, including the pending batch, even when the load failed
            putBatch(result, names, bodies, batchCount);
            closeQuietly(cursor);
        }

        logOversizedSplits();

        return new LoadResult(result, failed);
    }

    /**
     * Logs one warning per split whose body is over MAX_BODY_BYTES, once per DAO: a row already
     * warned about is not logged again on later loads. Only name and size are read,
     * never the body. Failures are logged and do not affect the main load.
     */
    private void logOversizedSplits() {
        final String sql = "SELECT name, length(CAST(body AS BLOB)) FROM splits WHERE length(CAST(body AS BLOB)) > ?";

        Cursor cursor = null;
        try {
            cursor = mDatabase.query(sql, new Object[]{MAX_BODY_BYTES});

            final int nameIndex = getColumnIndexOrThrow(cursor, "name");
            while (cursor.moveToNext()) {
                final String name = cursor.getString(nameIndex);
                if (!mWarnedOversizedNames.add(String.valueOf(name))) {
                    continue;
                }
                final long size = cursor.getLong(1);
                Logger.w("Skipped oversized preloaded flag '" + name + "' (" + size
                        + " bytes): exceeds the row limit of " + MAX_BODY_BYTES + " bytes");
            }
        } catch (Exception e) {
            Logger.e("Error executing oversized splits query: " + e.getLocalizedMessage());
        } finally {
            closeQuietly(cursor);
        }
    }

    private static void putBatch(Map<String, SplitEntity> result, String[] names, String[] bodies, int count) {
        for (int i = 0; i < count; i++) {
            SplitEntity entity = new SplitEntity();
            entity.setName(names[i]);
            entity.setBody(bodies[i]);
            result.put(names[i], entity);
        }
    }

    private static void closeQuietly(Cursor cursor) {
        try {
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception ignored) {
            // ignore
        }
    }
}
