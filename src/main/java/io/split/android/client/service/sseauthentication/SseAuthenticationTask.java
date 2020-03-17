package io.split.android.client.service.sseauthentication;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.sseclient.SseAuthenticationResponse;
import io.split.android.client.service.sseclient.SseChannelsParser;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class SseAuthenticationTask implements SplitTask {

    private static final String API_KEY_PARAM = "apiKey";
    private static final String USER_KEY_PARAM = "userKey";

    private final HttpFetcher<SseAuthenticationResponse> mAuthFetcher;
    private final String mApiKey;
    private final String mUserKey;
    private final SseChannelsParser mChannelParser;

    public SseAuthenticationTask(@NonNull HttpFetcher<SseAuthenticationResponse> authFetcher,
                                 @NonNull String apiKey,
                                 @NonNull String userKey,
                                 @NonNull SseChannelsParser channelsParser) {
        mAuthFetcher = checkNotNull(authFetcher);
        mApiKey = checkNotNull(apiKey);
        mUserKey = checkNotNull(userKey);
        mChannelParser = checkNotNull(channelsParser);
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {
        SseAuthenticationResponse authResponse;
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(API_KEY_PARAM, mApiKey);
            params.put(USER_KEY_PARAM, mUserKey);
            authResponse = mAuthFetcher.execute(params);

        } catch (Exception e) {
            logError("Unexpected " + e.getLocalizedMessage());
            return SplitTaskExecutionInfo.error(SplitTaskType.SSE_AUTHENTICATION_TASK);
        }
        Logger.d("SSE Authentication done");
        Map<String, Object> data = new HashMap<>();
        data.put(SplitTaskExecutionInfo.SSE_TOKEN, authResponse.getToken());
        data.put(SplitTaskExecutionInfo.CHANNEL_LIST_PARAM, mChannelParser.parse(authResponse.getToken()));
        data.put(SplitTaskExecutionInfo.IS_VALID_API_KEY, authResponse.isValidApiKey());
        data.put(SplitTaskExecutionInfo.IS_STREAMING_ENABLED, authResponse.isStreamingEnabled());
        return SplitTaskExecutionInfo.success(SplitTaskType.SSE_AUTHENTICATION_TASK, data);
    }

    private void logError(String message) {
        Logger.e("Error while authenticating to SSE server: " + message);
    }
}
