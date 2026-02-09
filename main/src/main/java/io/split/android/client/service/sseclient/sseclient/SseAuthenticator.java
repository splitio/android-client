package io.split.android.client.service.sseclient.sseclient;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.split.android.client.service.sseclient.InvalidJwtTokenException;
import io.split.android.client.service.sseclient.SseAuthenticationResponse;
import io.split.android.client.service.sseclient.SseJwtParser;
import io.split.android.client.service.sseclient.StreamingConstants;
import io.split.android.client.service.sseclient.spi.StreamingAuthException;
import io.split.android.client.service.sseclient.spi.StreamingAuthFetcher;
import io.split.android.client.utils.logger.Logger;

public class SseAuthenticator {
    private static final String USER_KEY_PARAM = "users";

    private final StreamingAuthFetcher mAuthFetcher;
    private final Set<String> mUserKeys;
    private final SseJwtParser mJwtParser;
    private final String mFlagsSpec;

    public SseAuthenticator(@NonNull StreamingAuthFetcher authFetcher,
                            @NonNull SseJwtParser jwtParser,
                            @Nullable String flagsSpec) {
        mAuthFetcher = checkNotNull(authFetcher);
        mUserKeys = Collections.newSetFromMap(new ConcurrentHashMap<>());
        mJwtParser = checkNotNull(jwtParser);
        mFlagsSpec = flagsSpec;
    }

    public SseAuthenticationResult authenticate(long defaultSseConnectionDelaySecs) {
        SseAuthenticationResponse authResponse;
        try {
            Map<String, Object> params = new LinkedHashMap<>();
            if (mFlagsSpec != null && !mFlagsSpec.trim().isEmpty()) {
                params.put(StreamingConstants.FLAGS_SPEC_PARAM, mFlagsSpec);
            }
            params.put(USER_KEY_PARAM, mUserKeys);
            authResponse = mAuthFetcher.execute(params);

        } catch (StreamingAuthException authException) {
            logError("Unexpected " + authException.getLocalizedMessage());
            if (authException.getStatusCode() != null) {
                if (isNotRetryable(authException.getStatusCode())) {
                    return unsuccessfulAuthenticationUnrecoverableError();
                }

                return unexpectedHttpError(authException.getStatusCode());
            } else {
                return unexpectedError();
            }
        } catch (Exception e) {
            logError("Unexpected " + e.getLocalizedMessage());
            return unexpectedError();
        }
        Logger.d("SSE Authentication done, now parsing token");

        if (authResponse.isClientError()) {
            Logger.d("Error while authenticating to streaming. Check your SDK key is correct.");
            return unsuccessfulAuthenticationUnrecoverableError();
        }

        if (!authResponse.isStreamingEnabled()) {
            Logger.d("Streaming disabled for SDK key");
            return new SseAuthenticationResult(true, true, false, 0, null);
        }

        try {
            long sseConnectionDelay = authResponse.getSseConnectionDelay() != null ? authResponse.getSseConnectionDelay() : defaultSseConnectionDelaySecs;
            Logger.d("SSE token parsed successfully");
            return new SseAuthenticationResult(true, true, true,
                    sseConnectionDelay, mJwtParser.parse(authResponse.getToken()));
        } catch (InvalidJwtTokenException e) {
            Logger.e("Error while parsing Jwt");
        }
        return unexpectedError();
    }

    @NonNull
    private static SseAuthenticationResult unsuccessfulAuthenticationUnrecoverableError() {
        return new SseAuthenticationResult(false, false, false, 0, null);
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

    private boolean isNotRetryable(int httpStatus) {
        return httpStatus == 400
                || httpStatus == 403
                || httpStatus == 414
                || httpStatus == 9009;
    }
}
