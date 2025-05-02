package io.split.android.client.service.splits;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.VisibleForTesting;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import io.split.android.client.storage.general.GeneralInfoStorage;
import io.split.android.client.utils.logger.Logger;

/**
 * Handles proxy spec fallback and recovery.
 *
 * <p>This class manages the state machine that determines which spec version (latest or legacy) should be used
 * to communicate with the Split Proxy, based on observed proxy compatibility errors.
 * It ensures that the SDK can automatically fall back to a legacy spec when the proxy is outdated, periodically
 * attempt recovery, and return to normal operation if the proxy is upgraded.</p>
 *
 * <p>State Machine:</p>
 * <ul>
 *   <li><b>NONE</b>: Normal operation, using latest spec. Default state.</li>
 *   <li><b>FALLBACK</b>: Entered when a proxy error is detected with the latest spec. SDK uses legacy spec and omits RB_SINCE param.</li>
 *   <li><b>RECOVERY</b>: Entered after fallback interval elapses. SDK attempts to use latest spec again. If successful, returns to NONE.</li>
 * </ul>
 * <p>Transitions:</p>
 * <ul>
 *   <li>NONE --(proxy error w/ latest spec)--> FALLBACK</li>
 *   <li>FALLBACK --(interval elapsed)--> RECOVERY</li>
 *   <li>RECOVERY --(success w/ latest spec)--> NONE</li>
 *   <li>RECOVERY --(proxy error)--> FALLBACK</li>
 *   <li>FALLBACK --(generic 400)--> FALLBACK (error surfaced, no state change)</li>
 * </ul>
 * <p>Only an explicit proxy outdated error triggers fallback. Generic 400s do not.</p>
 *
 */
public class OutdatedSplitProxyHandler {

    private static final String PREVIOUS_SPEC = "1.2";

    private final String mLatestSpec;
    private final String mPreviousSpec;
    private final boolean mForBackgroundSync;
    private final long mProxyCheckIntervalMillis;

    private final AtomicLong mLastProxyCheckTimestamp = new AtomicLong(0L);
    private final GeneralInfoStorage mGeneralInfoStorage;
    private final AtomicReference<ProxyHandlingType> mCurrentProxyHandlingType = new AtomicReference<>(ProxyHandlingType.NONE);

    OutdatedSplitProxyHandler(String flagSpec, boolean forBackgroundSync, GeneralInfoStorage generalInfoStorage, long proxyCheckIntervalMillis) {
        this(flagSpec, PREVIOUS_SPEC, forBackgroundSync, generalInfoStorage, proxyCheckIntervalMillis);
    }

    /**
     * Constructs an OutdatedSplitProxyHandler instance with a custom proxy check interval.
     *
     * @param flagSpec the latest spec version
     * @param previousSpec the previous spec version
     * @param forBackgroundSync whether this instance is for background sync
     * @param generalInfoStorage the general info storage
     * @param proxyCheckIntervalMillis the custom proxy check interval
     */
    @VisibleForTesting
    OutdatedSplitProxyHandler(String flagSpec, String previousSpec, boolean forBackgroundSync, GeneralInfoStorage generalInfoStorage, long proxyCheckIntervalMillis) {
        mLatestSpec = flagSpec;
        mPreviousSpec = previousSpec;
        mForBackgroundSync = forBackgroundSync;
        mProxyCheckIntervalMillis = proxyCheckIntervalMillis;
        mGeneralInfoStorage = checkNotNull(generalInfoStorage);
    }

    /**
     * Tracks a proxy error and updates the state machine accordingly.
     */
    void trackProxyError() {
        if (mForBackgroundSync) {
            Logger.i("Background sync fetch; skipping proxy handling");
            updateHandlingType(ProxyHandlingType.NONE);
        } else {
            updateLastProxyCheckTimestamp(System.currentTimeMillis());
            updateHandlingType(ProxyHandlingType.FALLBACK);
        }
    }

    /**
     * Performs a periodic proxy check to attempt recovery.
     */
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

    void resetProxyCheckTimestamp() {
        updateLastProxyCheckTimestamp(0L);
    }

    /**
     * Returns the current spec version based on the state machine.
     *
     * @return the current spec version
     */
    String getCurrentSpec() {
        if (mCurrentProxyHandlingType.get() == ProxyHandlingType.FALLBACK) {
            return mPreviousSpec;
        }

        return mLatestSpec;
    }

    /**
     * Indicates whether the SDK is in fallback mode.
     *
     * @return true if in fallback mode, false otherwise
     */
    boolean isFallbackMode() {
        return mCurrentProxyHandlingType.get() == ProxyHandlingType.FALLBACK;
    }

    /**
     * Indicates whether the SDK is in recovery mode.
     *
     * @return true if in recovery mode, false otherwise
     */
    boolean isRecoveryMode() {
        return mCurrentProxyHandlingType.get() == ProxyHandlingType.RECOVERY;
    }

    private void updateHandlingType(ProxyHandlingType proxyHandlingType) {
        mCurrentProxyHandlingType.set(proxyHandlingType);
    }

    private long getLastProxyCheckTimestamp() {
        mLastProxyCheckTimestamp.compareAndSet(0L, mGeneralInfoStorage.getLastProxyUpdateTimestamp());
        return mLastProxyCheckTimestamp.get();
    }

    private void updateLastProxyCheckTimestamp(long newTimestamp) {
        mLastProxyCheckTimestamp.set(newTimestamp);
        mGeneralInfoStorage.setLastProxyUpdateTimestamp(newTimestamp);
    }

    /**
     * Enum representing the proxy handling types.
     */
    private enum ProxyHandlingType {
        // No action
        NONE,
        // Switch to previous spec
        FALLBACK,
        // Attempt recovery
        RECOVERY,
    }
}
