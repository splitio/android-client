package io.split.android.client.impressions;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.split.android.client.utils.Logger;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A wrapper around an ImpressionListener provided by the customer. The purpose
 * of the wrapper is to protect the SplitClient from any slow down happening due
 * to the client's ImpressionListener.
 *
 */
public class AsynchronousImpressionListener implements ImpressionListener {

    private final ImpressionListener _delegate;
    private final ExecutorService _executor;

    public static AsynchronousImpressionListener build(ImpressionListener delegate, int capacity) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("impression-listener-wrapper-%d")
                .build();

        ExecutorService executor = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(capacity), threadFactory);

        return new AsynchronousImpressionListener(delegate, executor);
    }

    public AsynchronousImpressionListener(ImpressionListener delegate, ExecutorService executor) {
        _delegate = delegate;
        _executor = executor;
    }


    @Override
    public void log(final Impression impression) {
        try {
            _executor.execute(new Runnable() {
                @Override
                public void run() {
                    _delegate.log(impression);
                }
            });
        } catch (Exception e) {
            Logger.w(e, "Unable to send impression to impression listener");
        }
    }

    @Override
    public void close() {
        try {
            _executor.shutdown();
            _delegate.close();
        } catch (Exception e) {
            Logger.w(e, "Unable to close AsynchronousImpressionListener");
        }
    }
}
