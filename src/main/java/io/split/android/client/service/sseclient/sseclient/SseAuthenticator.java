package io.split.android.client.service.sseclient.sseclient;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.sseclient.InvalidJwtTokenException;
import io.split.android.client.service.sseclient.SseAuthenticationResponse;
import io.split.android.client.service.sseclient.SseJwtParser;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class SseAuthenticator {
    private static final String USER_KEY_PARAM = "users";

    private final HttpFetcher<SseAuthenticationResponse> mAuthFetcher;
    private final String mUserKey;
    private final SseJwtParser mJwtParser;

    public SseAuthenticator(@NonNull HttpFetcher<SseAuthenticationResponse> authFetcher,
                            @NonNull String userKey,
                            @NonNull SseJwtParser jwtParser) {
        mAuthFetcher = checkNotNull(authFetcher);
        mUserKey = checkNotNull(userKey);
        mJwtParser = checkNotNull(jwtParser);
    }

    public SseAuthenticationResult authenticate() {
        SseAuthenticationResponse authResponse;
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(USER_KEY_PARAM, mUserKey);
            authResponse = mAuthFetcher.execute(params, null);

        } catch (Exception e) {
            logError("Unexpected " + e.getLocalizedMessage());
            return unexectedError();
        }
        Logger.d("SSE Authentication done, now parsing token");

        if(authResponse.isClientError()) {
            Logger.d("Error while authenticating to streaming. Check your api key is correct.");
            return new SseAuthenticationResult(false, false, false, null);
        }

        if(!authResponse.isStreamingEnabled()) {
            Logger.d("Streaming disabled for api key");
            return new SseAuthenticationResult(true, true, false, null);
        }

        try {
            return new SseAuthenticationResult(true, true, true, mJwtParser.parse(authResponse.getToken()));
        } catch (InvalidJwtTokenException e) {
            Logger.e("Error while parsing Jwt");
        }
        return unexectedError();
    }

    private void logError(String message) {
        Logger.e("Error while authenticating to SSE server: " + message);
    }

    private SseAuthenticationResult unexectedError() {
        return new SseAuthenticationResult(false, true);
    }
}
