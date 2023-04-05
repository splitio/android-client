package io.split.android.client.service.sseclient.sseclient;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.http.NetworkNotAvailableException;
import io.split.android.client.service.sseclient.InvalidJwtTokenException;
import io.split.android.client.service.sseclient.SseAuthenticationResponse;
import io.split.android.client.service.sseclient.SseJwtParser;
import io.split.android.client.utils.ConcurrentSet;
import io.split.android.client.utils.logger.Logger;

public class SseAuthenticator {
    private static final String USER_KEY_PARAM = "users";

    private final HttpFetcher<SseAuthenticationResponse> mAuthFetcher;
    private final Set<String> mUserKeys;
    private final SseJwtParser mJwtParser;

    public SseAuthenticator(@NonNull HttpFetcher<SseAuthenticationResponse> authFetcher,
                            @NonNull SseJwtParser jwtParser) {
        mAuthFetcher = checkNotNull(authFetcher);
        mUserKeys = new ConcurrentSet<>();
        mJwtParser = checkNotNull(jwtParser);
    }

    public SseAuthenticationResult authenticate() {
        SseAuthenticationResponse authResponse;
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(USER_KEY_PARAM, mUserKeys);
            authResponse = mAuthFetcher.execute(params, null);

        } catch (HttpFetcherException httpFetcherException) {
            if (!(httpFetcherException instanceof NetworkNotAvailableException)) {
                logError("Unexpected " + httpFetcherException.getLocalizedMessage());
            }
            if (httpFetcherException.getHttpStatus() != null) {
                return unexpectedHttpError(httpFetcherException.getHttpStatus());
            } else {
                return unexpectedError();
            }
        } catch (Exception e) {
            logError("Unexpected " + e.getLocalizedMessage());
            return unexpectedError();
        }
        Logger.d("SSE Authentication done, now parsing token");

        if (authResponse.isClientError()) {
            Logger.d("Error while authenticating to streaming. Check your api key is correct.");
            return new SseAuthenticationResult(false, false, false, 0, null);
        }

        if (!authResponse.isStreamingEnabled()) {
            Logger.d("Streaming disabled for api key");
            return new SseAuthenticationResult(true, true, false, 0, null);
        }

        try {
            long sseConnectionDelay = authResponse.getSseConnectionDelay() != null ? authResponse.getSseConnectionDelay().longValue() : ServiceConstants.DEFAULT_SSE_CONNECTION_DELAY_SECS;
            return new SseAuthenticationResult(true, true, true,
                    sseConnectionDelay, mJwtParser.parse(authResponse.getToken()));
        } catch (InvalidJwtTokenException e) {
            Logger.e("Error while parsing Jwt");
        }
        return unexpectedError();
    }

    public void registerKey(String userKey) {
        mUserKeys.add(userKey);
    }

    public void unregisterKey(String userKey) {
        mUserKeys.remove(userKey);
    }

    private void logError(String message) {
        Logger.e("Error while authenticating to SSE server: " + message);
    }

    private SseAuthenticationResult unexpectedError() {
        return new SseAuthenticationResult(false, true);
    }

    private SseAuthenticationResult unexpectedHttpError(int httpStatus) {
        return new SseAuthenticationResult(httpStatus);
    }
}
