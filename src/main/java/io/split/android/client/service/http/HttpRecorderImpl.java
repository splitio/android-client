package io.split.android.client.service.http;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import java.net.URI;

import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.network.HttpResponse;

public class HttpRecorderImpl<T> implements HttpRecorder<T> {

    private final HttpClient mClient;
    private final URI mTarget;
    private final HttpRequestBodySerializer<T> mRequestSerializer;

    public HttpRecorderImpl(@NonNull HttpClient client,
                            @NonNull URI target,
                            @NonNull HttpRequestBodySerializer<T> requestSerializer) {

        mClient = checkNotNull(client);
        mTarget = checkNotNull(target);
        mRequestSerializer = checkNotNull(requestSerializer);
    }

    @Override
    public void execute(@NonNull T data) throws HttpRecorderException {
        checkNotNull(data);

        String serializedData = mRequestSerializer.serialize(data);
        try {

            HttpResponse response = mClient.request(mTarget, HttpMethod.POST, serializedData).execute();
            if (!response.isSuccess()) {
                int httpStatus = response.getHttpStatus();
                throw new HttpRecorderException(mTarget.toString(), "http return code " + httpStatus, httpStatus);
            }
        } catch (HttpRecorderException httpRecorderException) {
            throw httpRecorderException;
        } catch (Exception e) {
            throw new HttpRecorderException(mTarget.toString(), e.getLocalizedMessage());
        }
    }
}
