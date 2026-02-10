package io.split.android.client.network;

/**
 * Interface for providing proxy credentials.
 * <p>
 * The token will be sent in the header "Proxy-Authorization: Bearer <token>"
 */
public interface BearerCredentialsProvider extends ProxyCredentialsProvider {

    String getToken();
}
