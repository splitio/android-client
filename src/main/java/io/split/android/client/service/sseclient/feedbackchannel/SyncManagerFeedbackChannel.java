package io.split.android.client.service.sseclient.feedbackchannel;

import androidx.annotation.NonNull;

public interface SyncManagerFeedbackChannel {
    void pushMessage(@NonNull SyncManagerFeedbackMessage message);
    void register(@NonNull SyncManagerFeedbackListener listener);
    void close();
}
