package io.split.android.client.network;

import java.net.URI;
import java.net.URL;

public interface UrlSanitizer {

    URL getUrl(URI uri);
}
