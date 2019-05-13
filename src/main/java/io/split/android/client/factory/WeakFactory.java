package io.split.android.client.factory;

import java.lang.ref.WeakReference;

import io.split.android.client.SplitFactory;

public class WeakFactory {
    private WeakReference<SplitFactory> mFactory;

    public WeakFactory(SplitFactory factory) {
        mFactory = new WeakReference<>(factory);
    }

    public WeakReference<SplitFactory> getFactory() {
        return mFactory;
    }
}
