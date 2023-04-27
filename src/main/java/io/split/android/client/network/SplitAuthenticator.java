package io.split.android.client.network;

import java.net.URLConnection;

public interface SplitAuthenticator {

    URLConnection authenticate(URLConnection connection);
}
