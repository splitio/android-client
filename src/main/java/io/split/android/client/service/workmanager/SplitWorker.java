package io.split.android.client.service.workmanager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

import io.split.android.android_client.BuildConfig;
import io.split.android.client.network.CertificatePin;
import io.split.android.client.network.CertificatePinningConfiguration;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpClientImpl;
import io.split.android.client.network.SplitHttpHeadersBuilder;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;

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
        mHttpClient = buildHttpClient(apiKey, buildCertPinningConfig(inputData));
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

    private static HttpClient buildHttpClient(String apiKey, @Nullable CertificatePinningConfiguration certificatePinningConfiguration) {
        HttpClientImpl.Builder builder = new HttpClientImpl.Builder();

        if (certificatePinningConfiguration != null) {
            builder.setCertificatePinningConfiguration(certificatePinningConfiguration);
        }

        HttpClient httpClient = builder
                .build();

        SplitHttpHeadersBuilder headersBuilder = new SplitHttpHeadersBuilder();
        headersBuilder.setClientVersion(BuildConfig.SPLIT_VERSION_NAME);
        headersBuilder.setApiToken(apiKey);
        headersBuilder.addJsonTypeHeaders();
        httpClient.addHeaders(headersBuilder.build());

        return httpClient;
    }

    @Nullable
    private static CertificatePinningConfiguration buildCertPinningConfig(Data inputData) {
        try {
            Type type = new TypeToken<Map<String, Set<CertificatePin>>>() {
            }.getType();
            Map<String, Set<CertificatePin>> certificatePins = Json.fromJson(
                    inputData.getString(ServiceConstants.WORKER_PARAM_CERTIFICATE_PINS), type);

            if (certificatePins != null && !certificatePins.isEmpty()) {
                CertificatePinningConfiguration.Builder builder = CertificatePinningConfiguration.builder();
                for (Map.Entry<String, Set<CertificatePin>> entry : certificatePins.entrySet()) {
                    builder.addPins(entry.getKey(), entry.getValue());
                }

                return builder
                        .build();
            }
        } catch (Exception e) {
            Logger.e("Error parsing certificate pinning configuration for background sync worker", e.getLocalizedMessage());
        }

        return null;
    }
}
