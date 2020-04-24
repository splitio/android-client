package io.split.android.client.service.sseauthentication;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.sseclient.InvalidJwtTokenException;
import io.split.android.client.service.sseclient.SseAuthenticationResponse;
import io.split.android.client.service.sseclient.SseJwtParser;
import io.split.android.client.service.sseclient.SseJwtToken;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class SseAuthenticationTask implements SplitTask {

    private static final String API_KEY_PARAM = "apiKey";
    private static final String USER_KEY_PARAM = "users";

    private final HttpFetcher<SseAuthenticationResponse> mAuthFetcher;
    private final String mUserKey;
    private final SseJwtParser mJwtParser;

    public SseAuthenticationTask(@NonNull HttpFetcher<SseAuthenticationResponse> authFetcher,
                                 @NonNull String userKey,
                                 @NonNull SseJwtParser channelsParser) {
        mAuthFetcher = checkNotNull(authFetcher);
        mUserKey = checkNotNull(userKey);
        mJwtParser = checkNotNull(channelsParser);
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {
        SseAuthenticationResponse authResponse;
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(USER_KEY_PARAM, mUserKey);
            authResponse = mAuthFetcher.execute(params);

        } catch (Exception e) {
            logError("Unexpected " + e.getLocalizedMessage());
            return unexectedError();
        }
        Logger.d("SSE Authentication done, now parsing token...");

        if(!authResponse.isValidApiKey()) {
            return invalidApiKeyError();
        }

        if(!authResponse.isStreamingEnabled()) {
            return streamingDisabledInfo();
        }

        try {
            return success(mJwtParser.parse(authResponse.getToken()));
        } catch (InvalidJwtTokenException e) {
            Logger.e("Error while parsing Jwt");
            return unexectedError();
        }
    }

    private void logError(String message) {
        Logger.e("Error while authenticating to SSE server: " + message);
    }

    private SplitTaskExecutionInfo unexectedError() {
        return SplitTaskExecutionInfo.error(SplitTaskType.SSE_AUTHENTICATION_TASK,
                oneValueMap(SplitTaskExecutionInfo.UNEXPECTED_ERROR, true));
    }
    private SplitTaskExecutionInfo invalidApiKeyError() {
        return SplitTaskExecutionInfo.error(SplitTaskType.SSE_AUTHENTICATION_TASK,
                oneValueMap(SplitTaskExecutionInfo.IS_VALID_API_KEY, false));
    }

    private SplitTaskExecutionInfo streamingDisabledInfo() {
        return SplitTaskExecutionInfo.success(SplitTaskType.SSE_AUTHENTICATION_TASK,
                oneValueMap(SplitTaskExecutionInfo.IS_STREAMING_ENABLED, true));
    }

    private SplitTaskExecutionInfo success(SseJwtToken jwt) {
        Map<String, Object> data = new HashMap<>();
        data.put(SplitTaskExecutionInfo.PARSED_SSE_JWT, jwt);
        data.put(SplitTaskExecutionInfo.IS_VALID_API_KEY, true);
        data.put(SplitTaskExecutionInfo.IS_STREAMING_ENABLED, true);
        return SplitTaskExecutionInfo.success(SplitTaskType.SSE_AUTHENTICATION_TASK, data);
    }

    private Map<String, Object> oneValueMap(String key, Object value) {
        Map<String, Object> data = new HashMap<>();
        data.put(key, value);
        return data;
    }
}
