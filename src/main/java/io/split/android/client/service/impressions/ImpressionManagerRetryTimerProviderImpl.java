package io.split.android.client.service.impressions;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import io.split.android.client.RetryBackoffCounterTimerFactory;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;

class ImpressionManagerRetryTimerProviderImpl implements ImpressionManagerRetryTimerProvider {

    private final RetryBackoffCounterTimerFactory mRetryBackoffCounterTimerFactory;

    private final SplitTaskExecutor mTaskExecutor;

    private final Supplier<RetryBackoffCounterTimer> mUniqueKeysRetrySupplier = Suppliers.memoize(buildBackoffTimerDelegate());

    private final Supplier<RetryBackoffCounterTimer> mImpressionsRetrySupplier = Suppliers.memoize(buildBackoffTimerDelegate());

    private final Supplier<RetryBackoffCounterTimer> mImpressionsCountRetrySupplier = Suppliers.memoize(buildBackoffTimerDelegate());

    ImpressionManagerRetryTimerProviderImpl(SplitTaskExecutor taskExecutor) {
        this(taskExecutor, new RetryBackoffCounterTimerFactory());
    }

    @VisibleForTesting
    ImpressionManagerRetryTimerProviderImpl(SplitTaskExecutor taskExecutor, RetryBackoffCounterTimerFactory retryBackoffCounterTimerFactory) {
        mRetryBackoffCounterTimerFactory = checkNotNull(retryBackoffCounterTimerFactory);
        mTaskExecutor = checkNotNull(taskExecutor);
    }

    @Override
    public RetryBackoffCounterTimer getUniqueKeysTimer() {
        return mUniqueKeysRetrySupplier.get();
    }

    @Override
    public RetryBackoffCounterTimer getImpressionsTimer() {
        return mImpressionsRetrySupplier.get();
    }

    @Override
    public RetryBackoffCounterTimer getImpressionsCountTimer() {
        return mImpressionsCountRetrySupplier.get();
    }

    @NonNull
    private Supplier<RetryBackoffCounterTimer> buildBackoffTimerDelegate() {
        return new Supplier<RetryBackoffCounterTimer>() {
            @Override
            public RetryBackoffCounterTimer get() {
                return mRetryBackoffCounterTimerFactory
                        .createWithFixedInterval(mTaskExecutor,
                                ServiceConstants.TELEMETRY_CONFIG_RETRY_INTERVAL_SECONDS,
                                ServiceConstants.UNIQUE_KEYS_MAX_RETRY_ATTEMPTS);
            }
        };
    }
}
