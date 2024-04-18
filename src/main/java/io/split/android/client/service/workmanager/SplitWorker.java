package io.split.android.client.service.workmanager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import io.split.android.android_client.BuildConfig;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpClientImpl;
import io.split.android.client.network.SplitHttpHeadersBuilder;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.storage.db.SplitRoomDatabase;

public abstract class SplitWorker extends Worker {

    private final SplitRoomDatabase mDatabase;
    private final HttpClient mHttpClient;
    private final String mEndpoint;
    private final long mCacheExpirationInSeconds;

    protected SplitTask mSplitTask;

    public SplitWorker(@NonNull Context context,
                       @NonNull WorkerParameters workerParams) {

        super(context, workerParams);
        Data inputData = workerParams.getInputData();
        String databaseName = inputData.getString(ServiceConstants.WORKER_PARAM_DATABASE_NAME);
        String apiKey = inputData.getString(ServiceConstants.WORKER_PARAM_API_KEY);
        mEndpoint = inputData.getString(ServiceConstants.WORKER_PARAM_ENDPOINT);
        mDatabase = SplitRoomDatabase.getDatabase(context, databaseName);
        mCacheExpirationInSeconds = inputData.getLong(ServiceConstants.WORKER_PARAM_SPLIT_CACHE_EXPIRATION,
                ServiceConstants.DEFAULT_SPLITS_CACHE_EXPIRATION_IN_SECONDS);
        SplitHttpHeadersBuilder headersBuilder = new SplitHttpHeadersBuilder();
        headersBuilder.setClientVersion(BuildConfig.SPLIT_VERSION_NAME);
        headersBuilder.setApiToken(apiKey);
        headersBuilder.addJsonTypeHeaders();
        mHttpClient = new HttpClientImpl.Builder().build();
        mHttpClient.addHeaders(headersBuilder.build());
    }

    @NonNull
    @Override
    public Result doWork() {
        if (mSplitTask != null) {
            mSplitTask.execute();
            return Result.success();
        } else {
            return Result.failure();
        }
    }

    protected SplitRoomDatabase getDatabase() {
        return mDatabase;
    }

    public HttpClient getHttpClient() {
        return mHttpClient;
    }

    public String getEndPoint() {
        return mEndpoint;
    }

    public long getCacheExpirationInSeconds() {
        return mCacheExpirationInSeconds;
    }
}
