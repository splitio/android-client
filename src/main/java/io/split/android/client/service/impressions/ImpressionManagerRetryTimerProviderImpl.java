package io.split.android.client.service.impressions;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Supplier;

import io.split.android.client.RetryBackoffCounterTimerFactory;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;

public class ImpressionManagerRetryTimerProviderImpl implements ImpressionManagerRetryTimerProvider {

    private final RetryBackoffCounterTimerFactory mRetryBackoffCounterTimerFactory;

    private final SplitTaskExecutor mTaskExecutor;

    private final Supplier<RetryBackoffCounterTimer> mUniqueKeysRetrySupplier = new MemoizedSupplier<>(buildBackoffTimerDelegate());

    private final Supplier<RetryBackoffCounterTimer> mImpressionsRetrySupplier = new MemoizedSupplier<>(buildBackoffTimerDelegate());

    private final Supplier<RetryBackoffCounterTimer> mImpressionsCountRetrySupplier = new MemoizedSupplier<>(buildBackoffTimerDelegate());

    public ImpressionManagerRetryTimerProviderImpl(SplitTaskExecutor taskExecutor) {
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
