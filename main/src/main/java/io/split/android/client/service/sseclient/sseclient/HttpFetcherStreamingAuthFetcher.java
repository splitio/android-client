package io.split.android.client.service.sseclient.sseclient;

import androidx.annotation.NonNull;

import java.util.Map;

import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.sseclient.SseAuthenticationResponse;
import io.split.android.client.service.sseclient.spi.StreamingAuthException;
import io.split.android.client.service.sseclient.spi.StreamingAuthFetcher;

/**
 * Adapter that implements StreamingAuthFetcher using HttpFetcher.
 */
public class HttpFetcherStreamingAuthFetcher implements StreamingAuthFetcher {

    private final HttpFetcher<SseAuthenticationResponse> mAuthFetcher;

    public HttpFetcherStreamingAuthFetcher(@NonNull HttpFetcher<SseAuthenticationResponse> authFetcher) {
        mAuthFetcher = authFetcher;
    }

    @NonNull
    @Override
    public SseAuthenticationResponse execute(@NonNull Map<String, Object> params) throws StreamingAuthException {
        try {
            return mAuthFetcher.execute(params, null);
        } catch (HttpFetcherException e) {
            throw new StreamingAuthException(e.getLocalizedMessage(), e, e.getHttpStatus());
        } catch (Exception e) {
            throw new StreamingAuthException(e.getLocalizedMessage(), e);
        }
    }
}
