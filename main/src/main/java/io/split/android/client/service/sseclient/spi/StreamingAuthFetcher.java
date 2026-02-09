package io.split.android.client.service.sseclient.spi;

import androidx.annotation.NonNull;

import java.util.Map;

import io.split.android.client.service.sseclient.SseAuthenticationResponse;

/**
 * Abstraction for fetching streaming authentication tokens.
 */
public interface StreamingAuthFetcher {

    /**
     * Executes the auth request with the provided parameters.
     *
     * @param params request parameters
     * @return authentication response
     * @throws StreamingAuthException when request fails
     */
    @NonNull
    SseAuthenticationResponse execute(@NonNull Map<String, Object> params) throws StreamingAuthException;
}
