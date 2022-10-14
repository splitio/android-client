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
    public static final String TELEMETRY_STATS = "/metrics/usage";
    public static final String UNIQUE_KEYS = "/keys/cs";

    public static URI splitChanges(String baseUrl, String queryString) throws URISyntaxException {
        return buildUrl(baseUrl, SPLIT_CHANGES, queryString);
    }

    public static URI mySegments(String baseUrl, String key) throws URISyntaxException {
        String encodedKey = key != null ? UrlEscapers.urlPathSegmentEscaper().escape(key) : null;
        return buildUrl(baseUrl, MY_SEGMENTS + "/" + encodedKey);
    }

    public static URI events(String baseUrl) throws URISyntaxException {
        return buildUrl(baseUrl, EVENTS);
    }

    public static URI impressions(String baseUrl) throws URISyntaxException {
        return buildUrl(baseUrl, IMPRESSIONS);
    }

    public static URI impressionsCount(String baseUrl) throws URISyntaxException {
        return buildUrl(baseUrl, IMPRESSIONS_COUNT);
    }

    public static URI sseAuthentication(String baseUrl) throws URISyntaxException {
        return buildUrl(baseUrl, SSE_AUTHENTICATION);
    }

    public static URI telemetryConfig(String baseUrl) throws URISyntaxException {
        return buildUrl(baseUrl, TELEMETRY_CONFIG);
    }

    public static URI telemetryStats(String baseUrl) throws URISyntaxException {
        return buildUrl(baseUrl, TELEMETRY_STATS);
    }

    public static URI uniqueKeys(String baseUrl) throws URISyntaxException {
        return buildUrl(baseUrl, UNIQUE_KEYS);
    }

    private static URI buildUrl(String baseUrl, String path) throws URISyntaxException {
        return buildUrl(baseUrl, path, null);
    }

    private static URI buildUrl(String baseUrl, String path, String queryString) throws URISyntaxException {
        if (baseUrl != null && baseUrl.endsWith("/")) {
            baseUrl = removeLastChar(baseUrl);
        }
        String urlString = baseUrl + path;
        if (!Strings.isNullOrEmpty(queryString)) {
            urlString = urlString + "?" + queryString;
        }
        return new URI(urlString);
    }

    private static String removeLastChar(String sourceString) {
        return sourceString == null || sourceString.length() == 0
                ? sourceString
                : (sourceString.substring(0, sourceString.length() - 1));
    }
}
