package io.split.android.client.network;

import java.net.URI;
import java.net.URISyntaxException;

public class SdkTargetPath {
    public static final String SPLIT_CHANGES = "/splitChanges";
    public static final String MY_SEGMENTS = "/mySegments";
    public static final String EVENTS = "/events/bulk";
    public static final String IMPRESSIONS = "/testImpressions/bulk";

    public static final URI splitChanges(String baseUrl) throws URISyntaxException {
        return buildUrl(baseUrl, SPLIT_CHANGES);
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

    private static URI buildUrl(String baseUrl, String path) throws URISyntaxException {
        return new URI(baseUrl + path);
    }
}
