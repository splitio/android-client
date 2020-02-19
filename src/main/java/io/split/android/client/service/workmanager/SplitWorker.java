package io.split.android.client.service.workmanager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.metrics.CachedMetrics;
import io.split.android.client.metrics.HttpMetrics;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpClientImpl;
import io.split.android.client.network.SplitHttpHeadersBuilder;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.NetworkHelper;
import io.split.android.engine.metrics.Metrics;

import static androidx.core.util.Preconditions.checkNotNull;

public abstract class SplitWorker extends Worker {

    private final SplitRoomDatabase mDatabase;
    private final HttpClient mHttpClient;
    private final NetworkHelper mNetworkHelper;
    private final String mEndpoint;
    private final Metrics mMetrics;

    protected SplitTask mSplitTask;

    public SplitWorker(@NonNull Context context,
                       @NonNull WorkerParameters workerParams) {

        super(context, workerParams);
        Data inputData = workerParams.getInputData();
        String databaseName = inputData.getString(ServiceConstants.WORKER_PARAM_DATABASE_NAME);
        String apiKey = inputData.getString(ServiceConstants.WORKER_PARAM_API_KEY);
        mEndpoint = inputData.getString(ServiceConstants.WORKER_PARAM_ENDPOINT);
        String metricsEndpoint = inputData.getString(ServiceConstants.WORKER_PARAM_EVENTS_ENDPOINT);
        mDatabase = SplitRoomDatabase.getDatabase(context, databaseName);

        SplitHttpHeadersBuilder headersBuilder = new SplitHttpHeadersBuilder();
        headersBuilder.setClientVersion(SplitClientConfig.splitSdkVersion);
        headersBuilder.setApiToken(apiKey);
        mHttpClient = new HttpClientImpl();
        mHttpClient.addHeaders(headersBuilder.build());
        mNetworkHelper = new NetworkHelper();

        URI eventsRootTarget = URI.create(metricsEndpoint);
        HttpMetrics httpMetrics = null;
        try {
            httpMetrics = HttpMetrics.create(mHttpClient, eventsRootTarget);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        mMetrics = new CachedMetrics(httpMetrics, TimeUnit.SECONDS.toMillis(1));
    }

    @NonNull
    @Override
    public Result doWork() {
        checkNotNull(mSplitTask);
        SplitTaskExecutionInfo info = mSplitTask.execute();
        Data data = new Data.Builder()
                .putString(ServiceConstants.TASK_INFO_FIELD_STATUS, info.getStatus().toString())
                .putString(ServiceConstants.TASK_INFO_FIELD_TYPE, info.getTaskType().toString())
                .putInt(ServiceConstants.TASK_INFO_FIELD_RECORDS_NON_SENT, info.getNonSentRecords())
                .putLong(ServiceConstants.TASK_INFO_FIELD_BYTES_NON_SET, info.getNonSentBytes())
                .build();
        return Result.success(data);
    }

    protected SplitRoomDatabase getDatabase() {
        return mDatabase;
    }

    public HttpClient getHttpClient() {
        return mHttpClient;
    }

    public NetworkHelper getNetworkHelper() {
        return mNetworkHelper;
    }

    public String getEndPoint() {
        return mEndpoint;
    }

    public Metrics getMetrics() {
        return mMetrics;
    }
}
