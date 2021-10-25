package io.split.android.client.network;

import java.net.URI;
import java.net.URL;

import io.split.android.client.utils.Logger;
import okhttp3.HttpUrl;

public class UrlSanitizerImpl implements UrlSanitizer {

    private final HttpUrl.Builder mUrlBuilder;

    public UrlSanitizerImpl() {
        mUrlBuilder = new HttpUrl.Builder();
    }

    @Override
    public URL getUrl(URI uri) {

        mUrlBuilder
                .fragment(uri.getFragment())
                .host(uri.getHost())
                .scheme(uri.getScheme())
                .encodedQuery(uri.getQuery());

        try {
            mUrlBuilder.encodedPath(uri.getPath());
        } catch (IllegalArgumentException exception) {
            Logger.e(exception);
        }

        int port = uri.getPort();
        if (port > 0 && port <= 65535) {
            try {
                mUrlBuilder.port(port);
            } catch (IllegalArgumentException exception) {
                Logger.e(exception);
            }
        }

        return mUrlBuilder.build().url();
    }
}
