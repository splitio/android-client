package io.split.android.client.service.splits;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.VisibleForTesting;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import io.split.android.client.storage.general.GeneralInfoStorage;
import io.split.android.client.utils.logger.Logger;

public class OutdatedSplitProxyHandler {

    private static final long PROXY_CHECK_INTERVAL_MILLIS = TimeUnit.HOURS.toMillis(1);

    private final String mLatestSpec;
    private final String mPreviousSpec;
    private final boolean mForBackgroundSync;
    private final long mProxyCheckIntervalMillis;

    private final AtomicLong mLastProxyCheckTimestamp = new AtomicLong(0L);
    private final GeneralInfoStorage mGeneralInfoStorage;
    private final AtomicReference<ProxyHandlingType> mCurrentProxyHandlingType = new AtomicReference<>(ProxyHandlingType.NONE);

    OutdatedSplitProxyHandler(String flagSpec, String previousSpec, boolean forBackgroundSync, GeneralInfoStorage generalInfoStorage) {
        this(flagSpec, previousSpec, forBackgroundSync, generalInfoStorage, PROXY_CHECK_INTERVAL_MILLIS);
    }

    @VisibleForTesting
    OutdatedSplitProxyHandler(String flagSpec, String previousSpec, boolean forBackgroundSync, GeneralInfoStorage generalInfoStorage, long proxyCheckIntervalMillis) {
        mLatestSpec = flagSpec;
        mPreviousSpec = previousSpec;
        mForBackgroundSync = forBackgroundSync;
        mProxyCheckIntervalMillis = proxyCheckIntervalMillis;
        mGeneralInfoStorage = checkNotNull(generalInfoStorage);
    }

    void trackProxyError() {
        if (mForBackgroundSync) {
            Logger.i("Background sync fetch; skipping proxy handling");
            updateHandlingType(ProxyHandlingType.NONE);
        } else {
            updateLastProxyCheckTimestamp(System.currentTimeMillis());
            updateHandlingType(ProxyHandlingType.FALLBACK);
        }
    }

    void performProxyCheck() {
        if (mForBackgroundSync) {
            updateHandlingType(ProxyHandlingType.NONE);
        }

        long lastProxyCheckTimestamp = getLastProxyCheckTimestamp();

        if (lastProxyCheckTimestamp == 0L) {
            Logger.v("Never checked proxy; continuing with latest spec");
            updateHandlingType(ProxyHandlingType.NONE);
        } else if (System.currentTimeMillis() - lastProxyCheckTimestamp > mProxyCheckIntervalMillis) {
            Logger.i("Time since last check elapsed. Attempting recovery with latest spec: " + mLatestSpec);
            updateHandlingType(ProxyHandlingType.RECOVERY);
        } else {
            Logger.v("Have used proxy fallback mode; time since last check has not elapsed. Using previous spec");
            updateHandlingType(ProxyHandlingType.FALLBACK);
        }
    }

    private void updateHandlingType(ProxyHandlingType proxyHandlingType) {
        mCurrentProxyHandlingType.set(proxyHandlingType);
    }

    void resetProxyCheckTimestamp() {
        updateLastProxyCheckTimestamp(0L);
    }

    String getCurrentSpec() {
        if (mCurrentProxyHandlingType.get() == ProxyHandlingType.FALLBACK) {
            return mPreviousSpec;
        }

        return mLatestSpec;
    }

    boolean isFallbackMode() {
        return mCurrentProxyHandlingType.get() == ProxyHandlingType.FALLBACK;
    }

    boolean isRecoveryMode() {
        return mCurrentProxyHandlingType.get() == ProxyHandlingType.RECOVERY;
    }

    private long getLastProxyCheckTimestamp() {
        mLastProxyCheckTimestamp.compareAndSet(0L, mGeneralInfoStorage.getLastProxyUpdateTimestamp());
        return mLastProxyCheckTimestamp.get();
    }

    private void updateLastProxyCheckTimestamp(long newTimestamp) {
        mLastProxyCheckTimestamp.set(newTimestamp);
        mGeneralInfoStorage.setLastProxyUpdateTimestamp(newTimestamp);
    }

    private enum ProxyHandlingType {
        // no action
        NONE,
        // switch to previous spec
        FALLBACK,
        // attempt recovery
        RECOVERY,
    }
}
