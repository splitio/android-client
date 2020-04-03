package io.split.android.client.service.sseclient.feedbackchannel;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PushManagerEventBroadcaster {

    private List<WeakReference<BroadcastedEventListener>> mListeners;

    public PushManagerEventBroadcaster() {
        mListeners = new CopyOnWriteArrayList<>();
    }

    public void pushMessage(@NonNull BroadcastedEvent message) {
        for (WeakReference<BroadcastedEventListener> listenerRef : mListeners) {
            BroadcastedEventListener listener = listenerRef.get();
            if (listener != null) {
                listener.onEvent(message);
            }
        }
    }

    public void register(@NonNull BroadcastedEventListener listener) {
        mListeners.add(new WeakReference<>(listener));
    }

    public void close() {
        mListeners.clear();
    }
}
