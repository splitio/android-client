package io.split.android.client;

import android.content.Context;

import androidx.annotation.NonNull;

import io.split.android.client.api.Key;
import io.split.android.client.exceptions.SplitInstantiationException;
import io.split.android.client.localhost.LocalhostSplitFactory;
import io.split.android.client.service.ServiceConstants;

/**
 * Builds an instance of {@link SplitFactory}.
 */
public class SplitFactoryBuilder {

    /**
     * Instantiates a {@link SplitFactory} with default configurations
     *
     * @param sdkKey      the SDK key. MUST NOT be null
     * @param matchingKey the matching key. MUST NOT be null
     * @param context     the Android Context. MUST NOT be null
     * @return a {@link SplitFactory} implementation
     * @throws SplitInstantiationException
     */
    public static SplitFactory build(@NonNull String sdkKey, @NonNull String matchingKey, @NonNull Context context) throws SplitInstantiationException {
        if (matchingKey == null) {
            throw new SplitInstantiationException("Could not instantiate SplitFactory. Matching key cannot be null");
        }

        Key key = new Key(matchingKey, null);
        return build(sdkKey, key, context);
    }

    /**
     * Instantiates a {@link SplitFactory} with default configurations
     *
     * @param sdkKey  the SDK key. MUST NOT be null
     * @param key     the matching key. MUST NOT be null
     * @param context the Android Context. MUST NOT be null
     * @return a {@link SplitFactory} implementation
     * @throws SplitInstantiationException
     */
    public static SplitFactory build(@NonNull String sdkKey, @NonNull Key key, @NonNull Context context) throws SplitInstantiationException {
        return build(sdkKey, key, SplitClientConfig.builder().build(), context);
    }

    /**
     * Instantiates a {@link SplitFactory}
     *
     * @param sdkKey  the SDK key. MUST NOT be null
     * @param key     the matching key. MUST NOT be null
     * @param context the Android Context. MUST NOT be null
     * @return a {@link SplitFactory} implementation
     * @throws SplitInstantiationException
     */
    public static synchronized SplitFactory build(@NonNull String sdkKey, @NonNull Key key, @NonNull SplitClientConfig config, @NonNull Context context) throws SplitInstantiationException {
        try {
            checkPreconditions(sdkKey, key, config, context);

            if (ServiceConstants.LOCALHOST.equals(sdkKey)) {
                return new LocalhostSplitFactory(key.matchingKey(), context, config);
            } else {
                return new SplitFactoryImpl(sdkKey, key, config, context);
            }
        } catch (Exception ex) {
            throw new SplitInstantiationException("Could not instantiate SplitFactory", ex);
        }
    }

    /**
     * Instantiates a local {@link SplitFactory}
     *
     * @return a {@link SplitFactory} implementation
     */
    public static SplitFactory local(@NonNull String key, @NonNull Context context) {
        return new LocalhostSplitFactory(key, context, SplitClientConfig.builder().build());
    }

    private static void checkPreconditions(@NonNull String sdkKey, @NonNull Key key, @NonNull SplitClientConfig config, @NonNull Context context) throws SplitInstantiationException {
        if (sdkKey == null) {
            throw new SplitInstantiationException("Could not instantiate SplitFactory. SDK key cannot be null");
        }

        if (key == null) {
            throw new SplitInstantiationException("Could not instantiate SplitFactory. Matching key cannot be null");
        }

        if (config == null) {
            throw new SplitInstantiationException("Could not instantiate SplitFactory. Config cannot be null");
        }

        if (context == null) {
            throw new SplitInstantiationException("Could not instantiate SplitFactory. Context cannot be null");
        }
    }
}
