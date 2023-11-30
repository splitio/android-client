package io.split.android.client.network;

import androidx.annotation.Nullable;

import java.net.URI;
import java.net.URL;

public interface UrlSanitizer {

    @Nullable
    URL getUrl(URI uri);
}
