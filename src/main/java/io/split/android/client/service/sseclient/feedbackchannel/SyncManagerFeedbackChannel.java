package io.split.android.client.service.sseclient.feedbackchannel;

import androidx.annotation.NonNull;

public interface SyncManagerFeedbackChannel {
    /***
     * This interface  defines the methods to be implemented by a feedback
     * channel to handle the communication between synchronization components
     */

    void pushMessage(@NonNull SyncManagerFeedbackMessage message);

    void register(@NonNull SyncManagerFeedbackListener listener);

    void close();
}
