package io.split.android.client.network;

import java.util.HashMap;
import java.util.Map;

public class SplitHttpHeadersBuilder {

    static private final Map<String, String> noCacheHeaders;

    static {
        noCacheHeaders = new HashMap<>();
        noCacheHeaders.put(SplitHttpHeadersBuilder.CACHE_CONTROL_HEADER, SplitHttpHeadersBuilder.CACHE_CONTROL_NO_CACHE);
    }

    Map<String, String> mHeaders;

    private static final String CLIENT_MACHINE_NAME_HEADER = "SplitSDKMachineName";
    private static final String CLIENT_MACHINE_IP_HEADER = "SplitSDKMachineIP";
    private static final String CLIENT_VERSION = "SplitSDKVersion";
    private static final String AUTHORIZATION = "Authorization";
    private static final String ABLY_CLIENT_KEY = "SplitSDKClientKey";
    public final static String CACHE_CONTROL_HEADER = "Cache-Control";
    public final static String CACHE_CONTROL_NO_CACHE = "no-cache";

    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String ACCEPT_TYPE_HEADER = "Accept";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CONTENT_TYPE_STREAM = "text/event-stream";

    private static final int ABLY_CLIENT_KEY_LENGTH = 4;

    public SplitHttpHeadersBuilder() {
        mHeaders = new HashMap<>();
    }

    public SplitHttpHeadersBuilder addJsonTypeHeaders() {
        mHeaders.put(CONTENT_TYPE_HEADER, CONTENT_TYPE_JSON);
        mHeaders.put(ACCEPT_TYPE_HEADER, CONTENT_TYPE_JSON);
        return this;
    }

    public SplitHttpHeadersBuilder addStreamingTypeHeaders() {
        mHeaders.put(SplitHttpHeadersBuilder.CONTENT_TYPE_HEADER, SplitHttpHeadersBuilder.CONTENT_TYPE_STREAM);
        return this;
    }

    public SplitHttpHeadersBuilder setApiToken(String apiToken) {
        mHeaders.put(AUTHORIZATION, "Bearer " + apiToken);
        return this;
    }

    public SplitHttpHeadersBuilder setAblyApiToken(String apiToken) {
        mHeaders.put(ABLY_CLIENT_KEY, apiToken.substring(apiToken.length() - ABLY_CLIENT_KEY_LENGTH));
        return this;
    }

    public SplitHttpHeadersBuilder setHostname(String hostname) {
        if (hostname != null) {
            mHeaders.put(CLIENT_MACHINE_NAME_HEADER, hostname);
        }
        return this;
    }

    public SplitHttpHeadersBuilder setHostIp(String hostIp) {
        if (hostIp != null) {
            mHeaders.put(CLIENT_MACHINE_IP_HEADER, hostIp);
        }
        return this;
    }

    public SplitHttpHeadersBuilder setClientVersion(String clientVersion) {
        if (clientVersion == null) {
            throw new IllegalArgumentException("Client Version Http Header cannot be null!");
        }
        mHeaders.put(CLIENT_VERSION, clientVersion);
        return this;
    }

    public Map<String, String> build() {
        if (mHeaders.get(CONTENT_TYPE_HEADER) == null) {
            throw new IllegalArgumentException("Missing CONTENT TYPE header!");
        } else if (mHeaders.get(CONTENT_TYPE_HEADER).equals(CONTENT_TYPE_JSON)) {
            if (mHeaders.get(AUTHORIZATION) == null) {
                throw new IllegalArgumentException("Missing authorization header!");
            }
            if (mHeaders.get(CLIENT_VERSION) == null) {
                throw new IllegalArgumentException("Missing client version header!");
            }
        } else if (mHeaders.get(CONTENT_TYPE_HEADER).equals(CONTENT_TYPE_STREAM)) {
            if (mHeaders.get(ABLY_CLIENT_KEY) == null) {
                throw new IllegalArgumentException("Missing ably key header!");
            }
        } else {
            throw new IllegalArgumentException("Invalid CONTENT TYPE header!");
        }
        return mHeaders;
    }

    static public Map<String, String> noCacheHeaders() {
        return noCacheHeaders;
    }
}