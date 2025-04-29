package io.split.android.client.service.splits;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import io.split.android.client.utils.logger.Logger;

public class OutdatedSplitProxyHandler {

    private final String mLatestSpec;
    private final String mPreviousSpec;
    private static final long PROXY_CHECK_INTERNAL_MILLIS = TimeUnit.HOURS.toMillis(1);
    private final AtomicReference<String> mCurrentSpec;
    private final AtomicLong mLastProxyCheckTimestamp = new AtomicLong(0); // TODO, persist
    private final boolean mForBackgroundSync;

    OutdatedSplitProxyHandler(String flagSpec, String previousSpec, boolean forBackgroundSync) {
        mLatestSpec = flagSpec;
        mPreviousSpec = previousSpec;
        mCurrentSpec = new AtomicReference<>(flagSpec);
        mForBackgroundSync = forBackgroundSync;
    }

    ProxyHandlingType handle() {
        if (mForBackgroundSync) {
            Logger.i("Background sync fetch; skipping proxy handling");
            return ProxyHandlingType.NONE;
        }

        if (mCurrentSpec.get().equals(mLatestSpec)) {
            Logger.i("Switching to previous spec: " + mPreviousSpec);

            mLastProxyCheckTimestamp.set(System.currentTimeMillis());

            mCurrentSpec.set(mPreviousSpec);

            return ProxyHandlingType.FALLBACK;
        } else if (mCurrentSpec.get().equals(mPreviousSpec)) {
            if (System.currentTimeMillis() - mLastProxyCheckTimestamp.get() > PROXY_CHECK_INTERNAL_MILLIS) {
                Logger.i("Attempting recovery with latest spec: " + mLatestSpec);
                return ProxyHandlingType.RECOVERY;
            }
        }

        return ProxyHandlingType.NONE;
    }

    String getCurrentSpec() {
        return mCurrentSpec.get();
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
