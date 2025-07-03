package io.split.android.client.network;

import androidx.annotation.Nullable;

/**
 * Interface for providing proxy credentials.
 */
public interface ProxyCredentialsProvider {

    /**
     * Returns Bearer token for proxy authentication.
     * <p>
     * If set, this token will be sent to the proxy as 'Proxy-Authorization: Bearer <token>'.
     *
     * @return Bearer token
     */
    @Nullable
    String getBearerToken();
}
