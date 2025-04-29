package io.split.android.client.service.splits;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import io.split.android.client.storage.general.GeneralInfoStorage;
import io.split.android.client.utils.logger.Logger;

public class OutdatedSplitProxyHandler {

    private static final long PROXY_CHECK_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(1);

    private final String mLatestSpec;
    private final String mPreviousSpec;
    private final AtomicReference<String> mCurrentSpec;
    private final boolean mForBackgroundSync;
    private final long mProxyCheckIntervalMillis;

    private final AtomicLong mLastProxyCheckTimestamp = new AtomicLong(0L);
    private final GeneralInfoStorage mGeneralInfoStorage;

    OutdatedSplitProxyHandler(String flagSpec, String previousSpec, boolean forBackgroundSync, GeneralInfoStorage generalInfoStorage) {
        this(flagSpec, previousSpec, forBackgroundSync, generalInfoStorage, PROXY_CHECK_INTERVAL_MILLIS);
    }

    @VisibleForTesting
    OutdatedSplitProxyHandler(String flagSpec, String previousSpec, boolean forBackgroundSync, GeneralInfoStorage generalInfoStorage, long proxyCheckIntervalMillis) {
        mLatestSpec = flagSpec;
        mPreviousSpec = previousSpec;
        mCurrentSpec = new AtomicReference<>(flagSpec);
        mForBackgroundSync = forBackgroundSync;
        mProxyCheckIntervalMillis = proxyCheckIntervalMillis;
        mGeneralInfoStorage = checkNotNull(generalInfoStorage);
    }

    ProxyHandlingType handle() {
        if (mForBackgroundSync) {
            Logger.i("Background sync fetch; skipping proxy handling");
            return ProxyHandlingType.NONE;
        }

        if (mCurrentSpec.get().equals(mLatestSpec)) {
            return fallback();
        }

        return ProxyHandlingType.NONE;
    }

    @NonNull
    private ProxyHandlingType fallback() {
        Logger.i("Switching to previous spec: " + mPreviousSpec);

        updateLastProxyCheckTimestamp(System.currentTimeMillis());

        mCurrentSpec.set(mPreviousSpec);

        return ProxyHandlingType.FALLBACK;
    }

    ProxyHandlingType proxyCheck() {
        if (mCurrentSpec.get().equals(mLatestSpec)) {
            long lastProxyCheckTimestamp = getLastProxyCheckTimestamp();
            if (lastProxyCheckTimestamp != 0L) {
                // we may need to recover
                if (System.currentTimeMillis() - lastProxyCheckTimestamp > mProxyCheckIntervalMillis) {
                    Logger.i("Attempting recovery with latest spec: " + mLatestSpec);
                    mCurrentSpec.set(mLatestSpec);
                    updateLastProxyCheckTimestamp(System.currentTimeMillis());
                    return ProxyHandlingType.RECOVERY;
                }
            } else {
                return fallback();
            }
        }
        Logger.v("No need to handle outdated proxy");
        return ProxyHandlingType.NONE;
    }

    void resetProxyCheckTimestamp() {
        updateLastProxyCheckTimestamp(0L);
    }

    private long getLastProxyCheckTimestamp() {
        mLastProxyCheckTimestamp.compareAndSet(0L, mGeneralInfoStorage.getLastProxyUpdateTimestamp());
        return mLastProxyCheckTimestamp.get();
    }

    private void updateLastProxyCheckTimestamp(long newTimestamp) {
        mLastProxyCheckTimestamp.set(newTimestamp);
        mGeneralInfoStorage.setLastProxyUpdateTimestamp(newTimestamp);
    }

    String getCurrentSpec() {
        return mCurrentSpec.get();
    }

    boolean isFallbackMode() {
        return mCurrentSpec.get().equals(mPreviousSpec);
    }

    boolean isNormalMode() {
        return mCurrentSpec.get().equals(mLatestSpec);
    }

    enum ProxyHandlingType {
        // no action
        NONE,
        // switch to previous spec
        FALLBACK,
        // attempt recovery
        RECOVERY,
    }
}
