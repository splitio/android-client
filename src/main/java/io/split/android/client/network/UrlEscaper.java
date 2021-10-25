package io.split.android.client.network;

import java.net.URI;
import java.net.URL;

public interface UrlEscaper {

    URL getUrl(URI uri);
}
