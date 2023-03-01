package io.split.android.client;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.concurrent.TimeoutException;

import io.split.android.client.api.Key;
import io.split.android.client.localhost.LocalhostSplitFactory;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.utils.logger.Logger;
import io.split.android.grammar.Treatments;

/**
 * Builds an instance of SplitClient.
 */
public class SplitFactoryBuilder {

    /**
     *
     * @param apiToken
     * @param matchingkey
     * @param context
     * @return
     * @throws IOException
     * @throws InterruptedException
     * @throws TimeoutException
     * @throws URISyntaxException
     */
    public static SplitFactory build(String apiToken, String matchingkey, Context context) throws IOException, InterruptedException, TimeoutException, URISyntaxException {
        Key key = new Key(matchingkey, null);
        return build(apiToken, key, context);
    }

    /**
     * Instantiates a SplitFactory with default configurations
     *
     * @param apiToken the API token. MUST NOT be null
     * @return a SplitFactory
     * @throws IOException                           if the SDK was being started in 'localhost' mode, but
     *                                               there were problems reading the override file from disk.
     * @throws java.lang.InterruptedException        if you asked to block until the sdk was
     *                                               ready and the block was interrupted.
     * @throws java.util.concurrent.TimeoutException if you asked to block until the sdk was
     *                                               ready and the timeout specified via config#ready() passed.
     */
    public static SplitFactory build(String apiToken, Key key, Context context) throws IOException, InterruptedException, TimeoutException, URISyntaxException {
        return build(apiToken, key, SplitClientConfig.builder().build(), context);
    }

    /**
     * @param apiToken the API token. MUST NOT be null
     * @param config   parameters to control sdk construction. MUST NOT be null.
     * @return a SplitFactory
     * @throws java.io.IOException                   if the SDK was being started in 'localhost' mode, but
     *                                               there were problems reading the override file from disk.
     * @throws InterruptedException                  if you asked to block until the sdk was
     *                                               ready and the block was interrupted.
     * @throws java.util.concurrent.TimeoutException if you asked to block until the sdk was
     *                                               ready and the timeout specified via config#ready() passed.
     */
    public static synchronized SplitFactory build(String apiToken, Key key, SplitClientConfig config, Context context) throws IOException, InterruptedException, TimeoutException, URISyntaxException {
        if (ServiceConstants.LOCALHOST.equals(apiToken)) {
            return new LocalhostSplitFactory(key.matchingKey(), context, config);
        } else {
            return new SplitFactoryImpl(apiToken, key, config, context);
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
