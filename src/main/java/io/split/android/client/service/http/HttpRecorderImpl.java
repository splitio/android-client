package io.split.android.client.service.http;

import androidx.annotation.NonNull;

import java.net.URI;

import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.network.HttpResponse;
import io.split.android.client.utils.NetworkHelper;

import static com.google.common.base.Preconditions.checkNotNull;

public class HttpRecorderImpl<T> implements HttpRecorder<T> {

    private final HttpClient mClient;
    private final URI mTarget;
    private final NetworkHelper mNetworkHelper;
    private final HttpRequestBodySerializer<T> mRequestSerializer;

    public HttpRecorderImpl(@NonNull HttpClient client,
                            @NonNull URI target,
                            @NonNull NetworkHelper networkHelper,
                            @NonNull HttpRequestBodySerializer<T> requestSerializer) {

        mClient = checkNotNull(client);
        mTarget = checkNotNull(target);
        mNetworkHelper = checkNotNull(networkHelper);
        mRequestSerializer = checkNotNull(requestSerializer);
    }

    @Override
    public void execute(@NonNull T data) throws HttpRecorderException {
        checkNotNull(data);

        String serializedData = mRequestSerializer.serialize(data);
        try {
            if (!mNetworkHelper.isReachable(mTarget)) {
                throw new IllegalStateException("Source not reachable");
            }

            HttpResponse response = mClient.request(mTarget, HttpMethod.POST, serializedData).execute();
            if (!response.isSuccess()) {
                throw new IllegalStateException("http return code " + response.getHttpStatus());
            }
        } catch (Exception e) {
            throw new HttpRecorderException(mTarget.toString(), e.getLocalizedMessage());
        }
    }
}
