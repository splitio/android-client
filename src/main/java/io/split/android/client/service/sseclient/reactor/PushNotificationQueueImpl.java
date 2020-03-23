package io.split.android.client.service.sseclient.reactor;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class PushNotificationQueueImpl<T> implements PushNotificationQueue<T> {

    /***
     * Generic implementation fo PushNotificationQueue using a blocking queue to make
     * it thread safe
     */
    private final BlockingQueue<T> mQueue;
    private final static int QUEUE_CAPACITY = 50;
    public PushNotificationQueueImpl() {
        mQueue = new ArrayBlockingQueue<T>(QUEUE_CAPACITY);
    }

    @Override
    public void put(T notification) {
        try {
            mQueue.put(notification);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public T get() {
        T notification = null;
        try {
            notification = mQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return notification;
    }
}
