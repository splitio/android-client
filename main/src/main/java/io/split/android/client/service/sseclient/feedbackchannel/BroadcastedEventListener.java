package io.split.android.client.service.sseclient.feedbackchannel;

public interface BroadcastedEventListener {
    /***
     *  Interface to be implemented by a component to be registered
     *  in the feedback channel to listen to incomming messages
     */
    void onEvent(PushStatusEvent message);
}
