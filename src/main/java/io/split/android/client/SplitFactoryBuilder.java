package io.split.android.client;

import android.content.Context;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeoutException;

import io.split.android.client.api.Key;
import io.split.android.client.localhost.LocalhostSplitFactory;
import io.split.android.client.service.ServiceConstants;

/**
 * Builds an instance of SplitClient.
 */
public class SplitFactoryBuilder {

    /**
     *
     * @param sdkKey
     * @param matchingKey
     * @param context
     * @return
     * @throws IOException
     * @throws InterruptedException
     * @throws TimeoutException
     * @throws URISyntaxException
     */
    public static SplitFactory build(String sdkKey, String matchingKey, Context context) throws IOException, InterruptedException, TimeoutException, URISyntaxException {
        Key key = new Key(matchingKey, null);
        return build(sdkKey, key, context);
    }

    /**
     * Instantiates a SplitFactory with default configurations
     *
     * @param sdkKey the SDK key. MUST NOT be null
     * @return a SplitFactory
     * @throws IOException                           if the SDK was being started in 'localhost' mode, but
     *                                               there were problems reading the override file from disk.
     * @throws java.lang.InterruptedException        if you asked to block until the sdk was
     *                                               ready and the block was interrupted.
     * @throws java.util.concurrent.TimeoutException if you asked to block until the sdk was
     *                                               ready and the timeout specified via config#ready() passed.
     */
    public static SplitFactory build(String sdkKey, Key key, Context context) throws IOException, InterruptedException, TimeoutException, URISyntaxException {
        return build(sdkKey, key, SplitClientConfig.builder().build(), context);
    }

    /**
     * @param sdkKey the SDK key. MUST NOT be null
     * @param config   parameters to control sdk construction. MUST NOT be null.
     * @return a SplitFactory
     * @throws java.io.IOException                   if the SDK was being started in 'localhost' mode, but
     *                                               there were problems reading the override file from disk.
     * @throws InterruptedException                  if you asked to block until the sdk was
     *                                               ready and the block was interrupted.
     * @throws java.util.concurrent.TimeoutException if you asked to block until the sdk was
     *                                               ready and the timeout specified via config#ready() passed.
     */
    public static synchronized SplitFactory build(String sdkKey, Key key, SplitClientConfig config, Context context) throws IOException, InterruptedException, TimeoutException, URISyntaxException {
        if (ServiceConstants.LOCALHOST.equals(sdkKey)) {
            return new LocalhostSplitFactory(key.matchingKey(), context, config);
        } else {
            return new SplitFactoryImpl(sdkKey, key, config, context);
        }
    }

    /**
     * Instantiates a local Off-The-Grid SplitFactory
     *
     * @return a SplitFactory
     * @throws IOException if there were problems reading the override file from disk.
     */
    public static SplitFactory local(String key, Context context) throws IOException {
        return new LocalhostSplitFactory(key, context, SplitClientConfig.builder().build() );
    }
}
