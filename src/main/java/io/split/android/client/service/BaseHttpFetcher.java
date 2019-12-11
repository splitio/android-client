package io.split.android.client.service;

import androidx.annotation.NonNull;

import java.net.URI;

import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpException;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.network.HttpResponse;
import io.split.android.client.utils.NetworkHelper;
import io.split.android.engine.metrics.Metrics;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class BaseHttpFetcher {

    protected final HttpClient mClient;
    protected final URI mTarget;
    protected final Metrics mMetrics;
    protected final NetworkHelper mNetworkHelper;

    public BaseHttpFetcher(@NonNull HttpClient client, @NonNull URI target, @NonNull Metrics metrics, @NonNull NetworkHelper networkHelper) {
        checkNotNull(client);
        checkNotNull(target);
        checkNotNull(metrics);
        checkNotNull(networkHelper);

        mClient = client;
        mTarget = target;
        mMetrics = metrics;
        mNetworkHelper = networkHelper;
    }

    protected String exceptionMessage(String message) {
        return "An unexpected has occurred while retrieving data from server: " + (message != null ? message : "");
    }

    protected void throwIlegalStatusException(String message) {
        throwIlegalStatusException(null, message);
    }

    protected void throwIlegalStatusException(@NonNull Exception exception) {
        throwIlegalStatusException(exception, null);
    }

    protected void throwIlegalStatusException(@NonNull Exception exception, String message) {
        if(exception != null) {
            throw new IllegalStateException(exceptionMessage(exception.getLocalizedMessage()), exception);
        } else {
            throw new IllegalStateException(exceptionMessage(message));
        }
    }

    protected void checkIfSourceIsReachable() {
        if (!mNetworkHelper.isReachable(mTarget)) {
            throwIlegalStatusException("Source not reachable");
        }
    }

    protected void metricExceptionAndThrow(String exceptionMetric, Exception exception) {
        mMetrics.count(exceptionMetric, 1);
        throwIlegalStatusException(exception);
    }

    protected void metricTime(String metric, long time) {
        mMetrics.time(metric, time);
    }

    protected void checkResponseStatusAndThrowOnFail(HttpResponse response, String statusMetric) {
        if (!response.isSuccess()) {
            mMetrics.count(String.format(statusMetric, response.getHttpStatus()), 1);
            throwIlegalStatusException("http return code " + response.getHttpStatus());
        }
    }

    protected HttpResponse executeRequest(URI uri) throws HttpException {
        return mClient.request(uri, HttpMethod.GET).execute();
    }

    public static class MySegmentsJsonParser {

    }
}
