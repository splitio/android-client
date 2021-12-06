package io.split.android.client.network;

import com.google.common.base.Strings;
import com.google.common.net.UrlEscapers;

import java.net.URI;
import java.net.URISyntaxException;

public class SdkTargetPath {
    public static final String SPLIT_CHANGES = "/splitChanges";
    public static final String MY_SEGMENTS = "/mySegments";
    public static final String EVENTS = "/events/bulk";
    public static final String IMPRESSIONS = "/testImpressions/bulk";
    public static final String IMPRESSIONS_COUNT = "/testImpressions/count";
    public static final String SSE_AUTHENTICATION = "/auth";
    public static final String TELEMETRY_CONFIG = "/metrics/config";

    public static final URI splitChanges(String baseUrl, String queryString) throws URISyntaxException {
        return buildUrl(baseUrl, SPLIT_CHANGES, queryString);
    }

    public static final URI mySegments(String baseUrl, String key) throws URISyntaxException {
        String encodedKey = key != null ? UrlEscapers.urlPathSegmentEscaper().escape(key) : null;
        return buildUrl(baseUrl, MY_SEGMENTS + "/" + encodedKey);
    }

    public static final URI events(String baseUrl) throws URISyntaxException {
        return buildUrl(baseUrl, EVENTS);
    }

    public static final URI impressions(String baseUrl) throws URISyntaxException {
        return buildUrl(baseUrl, IMPRESSIONS);
    }

    public static final URI impressionsCount(String baseUrl) throws URISyntaxException {
        return buildUrl(baseUrl, IMPRESSIONS_COUNT);
    }

    public static final URI sseAuthentication(String baseUrl) throws URISyntaxException {
        return buildUrl(baseUrl, SSE_AUTHENTICATION);
    }

    public static URI telemetryConfig(String baseUrl) throws URISyntaxException {
        return buildUrl(baseUrl, TELEMETRY_CONFIG);
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
