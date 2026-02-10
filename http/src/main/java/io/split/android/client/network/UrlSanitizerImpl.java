package io.split.android.client.network;

import android.net.Uri;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import io.split.android.client.utils.logger.Logger;

public class UrlSanitizerImpl implements UrlSanitizer {

    @Override
    public URL getUrl(URI uri) {

        Uri.Builder mUrlBuilder = new Uri.Builder();

        mUrlBuilder
                .encodedAuthority(uri.getAuthority())
                .encodedFragment(uri.getFragment())
                .scheme(uri.getScheme())
                .encodedQuery(uri.getQuery());

        try {
            mUrlBuilder.encodedPath(uri.getPath());
        } catch (IllegalArgumentException exception) {
            Logger.e(exception);
        }

        try {
            return new URL(mUrlBuilder.build().toString());
        } catch (MalformedURLException e) {
            Logger.e(e.getMessage());
            return null;
        }
    }
}
