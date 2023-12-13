package io.split.android.client.service.impressions;

import androidx.core.util.Supplier;

class MemoizedSupplier<T> implements Supplier<T> {

    private final Supplier<T> mDelegate;
    private boolean mIsComputed = false;
    private T mValue;

    public MemoizedSupplier(Supplier<T> delegate) {
        mDelegate = delegate;
    }

    @Override
    public synchronized T get() {
        if (!mIsComputed) {
            mValue = mDelegate.get();
            mIsComputed = true;
        }
        return mValue;
    }
}
