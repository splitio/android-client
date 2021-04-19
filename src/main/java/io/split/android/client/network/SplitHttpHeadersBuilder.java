package io.split.android.client.network;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.service.ServiceConstants;

public class SplitHttpHeadersBuilder {

    Map<String, String> mHeaders;

    private static final String CLIENT_MACHINE_NAME_HEADER = "SplitSDKMachineName";
    private static final String CLIENT_MACHINE_IP_HEADER = "SplitSDKMachineIP";
    private static final String CLIENT_VERSION = "SplitSDKVersion";
    private static final String AUTHORIZATION = "Authorization";

    public SplitHttpHeadersBuilder() {
        mHeaders = new HashMap<>();
        addDefaultHeaders();
    }

    private void addDefaultHeaders() {
        mHeaders.put("Content-Type", "application/json");
        mHeaders.put("Accept", "application/json");
    }

    public SplitHttpHeadersBuilder setApiToken(String apiToken) {
        mHeaders.put(AUTHORIZATION, "Bearer " + apiToken);
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

        if (mHeaders.get(AUTHORIZATION) == null) {
            throw new IllegalArgumentException("Missing authorization header!");
        }

        if (mHeaders.get(CLIENT_VERSION) == null) {
            throw new IllegalArgumentException("Missing client version header!");
        }

        return mHeaders;
    }

    static public Map<String, String> noCacheHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(ServiceConstants.CACHE_CONTROL_HEADER, ServiceConstants.CACHE_CONTROL_NO_CACHE);
        return headers;
    }
}