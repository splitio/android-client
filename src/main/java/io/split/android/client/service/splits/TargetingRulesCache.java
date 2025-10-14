package io.split.android.client.service.splits;

import androidx.annotation.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import io.split.android.client.dtos.TargetingRulesChange;
import io.split.android.client.utils.logger.Logger;

/**
 * Thread-safe cache for storing prefetched targeting rules during fresh installs.
 * The cache is designed for single-use: once the cached value is consumed, it's cleared
 * and the lock is released to avoid blocking subsequent syncs.
 */
public class TargetingRulesCache {

    private static final long PREFETCH_WAIT_TIMEOUT_MS = 2000;

    private final ReentrantLock mLock;
    private volatile TargetingRulesChange mCachedValue;
    private volatile boolean mConsumed;

    public TargetingRulesCache() {
        mLock = new ReentrantLock();
        mCachedValue = null;
        mConsumed = false;
    }

    /**
     * Stores a value in the cache.
     *
     * @param value The targeting rules change to cache
     */
    public void set(@Nullable TargetingRulesChange value) {
        mLock.lock();
        try {
            if (!mConsumed) {
                mCachedValue = value;
            }
        } finally {
            mLock.unlock();
        }
    }

    /**
     * Retrieves and consumes the cached value if available.
     * Attempts to acquire the lock with a timeout to wait for in-progress prefetch operations.
     * After consumption, the cache is cleared and marked as consumed.
     *
     * @return The cached value, or null if not available or already consumed
     */
    @Nullable
    public TargetingRulesChange getAndConsume() {
        if (mConsumed) {
            return null;
        }

        // Try to acquire lock with timeout - this waits for prefetch if in progress
        boolean lockAcquired = false;
        try {
            lockAcquired = mLock.tryLock(PREFETCH_WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!lockAcquired) {
                Logger.w("Timeout waiting for prefetch lock after " + PREFETCH_WAIT_TIMEOUT_MS + "ms");
                return null;
            }

            if (mConsumed || mCachedValue == null) {
                return null;
            }

            TargetingRulesChange result = mCachedValue;
            mCachedValue = null;
            mConsumed = true;
            return result;
        } catch (InterruptedException e) {
            Logger.w("Interrupted while waiting for prefetch lock");
            Thread.currentThread().interrupt();
            return null;
        } finally {
            if (lockAcquired) {
                mLock.unlock();
            }
        }
    }

    /**
     * Checks if a cached value is available (not yet consumed).
     *
     * @return true if a value is cached and not consumed
     */
    public boolean hasValue() {
        return !mConsumed && mCachedValue != null;
    }

    /**
     * Executes an operation while holding the cache lock and stores the result.
     * The lock is for ensuring concurrent getAndConsume() calls will wait for completion.
     *
     * @param operation The operation to execute (typically a network fetch)
     * @throws Exception if the operation fails
     */
    public void setWithLock(CacheOperation operation) throws Exception {
        mLock.lock();
        try {
            if (!mConsumed) {
                mCachedValue = operation.execute();
            }
        } finally {
            mLock.unlock();
        }
    }

    /**
     * Interface for operations that produce a TargetingRulesChange.
     */
    public interface CacheOperation {
        TargetingRulesChange execute() throws Exception;
    }
}
