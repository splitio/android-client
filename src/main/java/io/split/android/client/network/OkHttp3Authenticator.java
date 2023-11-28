package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;

import io.split.android.client.utils.logger.Logger;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

public class OkHttp3Authenticator implements okhttp3.Authenticator {

    private final SplitAuthenticator mSplitAuthenticator;

    public OkHttp3Authenticator(SplitAuthenticator splitAuthenticator) {
        mSplitAuthenticator = splitAuthenticator;
    }

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, @NonNull Response response) {

        try {
            SplitAuthenticatedRequest authenticatedRequestResult = mSplitAuthenticator.authenticate(new SplitAuthenticatedRequest(response));
            if (authenticatedRequestResult == null) {
                return null;
            }

            Request.Builder builder = response.request()
                    .newBuilder();

            if (authenticatedRequestResult.getHeaders() != null) {
                for (Map.Entry<String, List<String>> header : authenticatedRequestResult.getHeaders().entrySet()) {
                    for (String value : header.getValue()) {
                        builder.addHeader(header.getKey(), value);
                    }
                }
            }

            return builder.build();
        } catch (Exception exception) {
            Logger.e("Error authenticating request: ", exception.getMessage());
            return null;
        }
    }
}
