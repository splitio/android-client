package io.split.android.client.network;

import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

public interface SplitAuthenticator {

    URLConnection authenticate(URLConnection connection);
}
