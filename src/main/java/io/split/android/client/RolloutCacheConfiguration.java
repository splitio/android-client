package io.split.android.client;

import io.split.android.client.service.ServiceConstants;
import io.split.android.client.utils.logger.Logger;

public class RolloutCacheConfiguration {

    private final int mExpiration;
    private final boolean mClearOnInit;

    private RolloutCacheConfiguration(int expiration, boolean clearOnInit) {
        mExpiration = expiration;
        mClearOnInit = clearOnInit;
    }

    public int getExpiration() {
        return mExpiration;
    }

    public boolean clearOnInit() {
        return mClearOnInit;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private static final int MIN_EXPIRATION_DAYS = 1;

        private int mExpiration = ServiceConstants.DEFAULT_ROLLOUT_CACHE_EXPIRATION;
        private boolean mClearOnInit = false;

        private Builder() {

        }

        /**
         * Set the expiration time for the rollout definitions cache, in days. Default is 10 days.
         * @param expiration in days
         * @return This builder
         */
        public Builder expiration(int expiration) {
            if (expiration < MIN_EXPIRATION_DAYS) {
                Logger.w("Cache expiration must be at least 1 day. Using default value.");
                mExpiration = ServiceConstants.DEFAULT_ROLLOUT_CACHE_EXPIRATION;
            } else {
                mExpiration = expiration;
            }

            return this;
        }

        /**
         * Set if the rollout definitions cache should be cleared on initialization. Default is false.
         * @param clearOnInit whether to clear cache on initialization.
         * @return This builder
         */
        public Builder clearOnInit(boolean clearOnInit) {
            mClearOnInit = clearOnInit;
            return this;
        }

        public RolloutCacheConfiguration build() {
            return new RolloutCacheConfiguration(mExpiration, mClearOnInit);
        }
    }
}
