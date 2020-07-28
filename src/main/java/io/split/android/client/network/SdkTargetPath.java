package io.split.android.client.network;

import com.google.common.base.Strings;

import java.net.URI;
import java.net.URISyntaxException;

import io.split.android.client.FilterBuilder;
import io.split.android.client.SyncConfig;

public class SdkTargetPath {
    public static final String SPLIT_CHANGES = "/splitChanges";
    public static final String MY_SEGMENTS = "/mySegments";
    public static final String EVENTS = "/events/bulk";
    public static final String IMPRESSIONS = "/testImpressions/bulk";
    public static final String SSE_AUTHENTICATION = "/auth";

    public static final URI splitChanges(String baseUrl, String queryString) throws URISyntaxException {
        return buildUrl(baseUrl, SPLIT_CHANGES, queryString);
    }

    public static final URI mySegments(String baseUrl, String key) throws URISyntaxException {
        return buildUrl(baseUrl, MY_SEGMENTS + "/" + key);
    }

    public static final URI events(String baseUrl) throws URISyntaxException {
        return buildUrl(baseUrl, EVENTS);
    }

    public static final URI impressions(String baseUrl) throws URISyntaxException {
        return buildUrl(baseUrl, IMPRESSIONS);
    }

    public static final URI sseAuthentication(String baseUrl) throws URISyntaxException {
        return buildUrl(baseUrl, SSE_AUTHENTICATION);
    }

    private static URI buildUrl(String baseUrl, String path) throws URISyntaxException {
        return buildUrl(baseUrl, path, null);
    }

    private static URI buildUrl(String baseUrl, String path, String queryString) throws URISyntaxException {
        String urlString = baseUrl + path;
        if (!Strings.isNullOrEmpty(queryString)) {
            urlString = urlString + "?" + queryString;
        }
        return new URI(urlString);
    }
}
