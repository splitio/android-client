package io.split.android.client.service.sseclient.reactor;

public interface PushNotificationQueue<T> {

    /***
     * Basic generic interface to be implemented for hold incoming notifications in a queue.
     */

    /***
     * Adds a new notification to the queue
     * @param notification The notification to be added to the queue
     */
    void put(T notification);

    /***
     * Takes a new notifaction from the queue
     * @return The notification took from the queue
     */
    T get();
}
