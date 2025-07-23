package io.split.android.client.network;

/**
 * Interface for providing basic credentials.
 * <p>
 * The username and password will be used to create a Proxy-Authorization header using Basic authentication
 */
public interface BasicCredentialsProvider extends ProxyCredentialsProvider {

    String getUserName();

    String getPassword();
}
