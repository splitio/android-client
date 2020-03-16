package io.split.android.client.service.sseclient.feedbackchannel;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SyncManagerFeedbackChannelImpl implements SyncManagerFeedbackChannel {

    private List<WeakReference<SyncManagerFeedbackListener>> mListeners;

    public SyncManagerFeedbackChannelImpl() {
        mListeners = new CopyOnWriteArrayList<>();
    }

    @Override
    public void pushMessage(@NonNull SyncManagerFeedbackMessage message) {
        for(WeakReference<SyncManagerFeedbackListener> listenerRef : mListeners) {
            SyncManagerFeedbackListener listener = listenerRef.get();
            if(listener != null) {
                listener.onFedbackMessage(message);
            }
        }
    }

    @Override
    public void register(@NonNull SyncManagerFeedbackListener listener) {
        mListeners.add(new WeakReference<>(listener));
    }

    @Override
    public void close() {
        mListeners.clear();
    }
}
