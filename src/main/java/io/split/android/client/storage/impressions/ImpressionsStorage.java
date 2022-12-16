package io.split.android.client.storage.impressions;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.utils.logger.Logger;

public class ImpressionsStorage implements Storage<KeyImpression> {
    final private PersistentStorage<KeyImpression> mPersistentStorage;
    final private AbstractQueue<KeyImpression> mImpressions = new ConcurrentLinkedQueue<>();
    final private AtomicBoolean mIsPersistenceEnabled = new AtomicBoolean(true);

    public ImpressionsStorage(@NonNull PersistentStorage<KeyImpression> persistentStorage,
                              boolean isPersistenceEnabled) {
        mPersistentStorage = checkNotNull(persistentStorage);
        mIsPersistenceEnabled.set(isPersistenceEnabled);
    }

    @Override
    public void enablePersistence(boolean enabled) {
        mIsPersistenceEnabled.set(enabled);
        if (enabled) {
            Logger.v("Persisting in memory impressions");
            ArrayList<KeyImpression> toPush = new ArrayList(mImpressions);
            mImpressions.removeAll(toPush);
            mPersistentStorage.pushMany(toPush);
        }
        Logger.d("Persistence for impressions has been " + (enabled ? "enabled" : "disabled"));
    }

    @Override
    public void push(@NonNull KeyImpression element) {
        if (element == null) {
            return;
        }
        if (mIsPersistenceEnabled.get()) {
            Logger.v("Pushing impressions to persistent storage");
            mPersistentStorage.push(element);
            return;
        }
        Logger.v("Pushing impressions to in memory storage");
        mImpressions.add(element);
    }

    @Override
    public void clearInMemory() {
        mImpressions.clear();
    }
}
