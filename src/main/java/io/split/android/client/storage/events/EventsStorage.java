package io.split.android.client.storage.events;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.dtos.Event;
import io.split.android.client.storage.common.PersistentStorage;
import io.split.android.client.storage.common.Storage;
import io.split.android.client.utils.logger.Logger;

public class EventsStorage implements Storage<Event> {
    final private PersistentEventsStorage mPersistentStorage;
    final private AbstractQueue<Event> mEvents = new ConcurrentLinkedQueue<>();
    final private AtomicBoolean mIsPersistenceEnabled = new AtomicBoolean(true);

    public EventsStorage(@NonNull PersistentEventsStorage persistentStorage,
                         boolean isPersistenceEnabled) {
        mPersistentStorage = checkNotNull(persistentStorage);
        mIsPersistenceEnabled.set(isPersistenceEnabled);
    }

    @Override
    public void enablePersistence(boolean enabled) {
        mIsPersistenceEnabled.set(enabled);
        if (enabled) {
            Logger.v("Persisting in memory events");
            ArrayList<Event> toPush = new ArrayList(mEvents);
            mEvents.removeAll(toPush);
            mPersistentStorage.pushMany(toPush);
        }
        Logger.d("Persistence for events has been " + (enabled ? "enabled" : "disabled"));
    }

    @Override
    public void push(@NonNull Event element) {
        if (element == null) {
            return;
        }
        if (mIsPersistenceEnabled.get()) {
            Logger.v("Pushing events to persistent storage");
            mPersistentStorage.push(element);
            return;
        }
        Logger.v("Pushing events to in memory storage");
        mEvents.add(element);
    }

    @Override
    public void clearInMemory() {
        mEvents.clear();
    }
}
