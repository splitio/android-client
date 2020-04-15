package io.split.android.client.service.sseclient.feedbackchannel;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SyncManagerFeedbackChannel {

    private List<WeakReference<SyncManagerFeedbackListener>> mListeners;

    public SyncManagerFeedbackChannel() {
        mListeners = new CopyOnWriteArrayList<>();
    }

    public void pushMessage(@NonNull SyncManagerFeedbackMessage message) {
        for (WeakReference<SyncManagerFeedbackListener> listenerRef : mListeners) {
            SyncManagerFeedbackListener listener = listenerRef.get();
            if (listener != null) {
                listener.onFeedbackMessage(message);
            }
        }
    }

    public void register(@NonNull SyncManagerFeedbackListener listener) {
        mListeners.add(new WeakReference<>(listener));
    }

    public void close() {
        mListeners.clear();
    }
}
