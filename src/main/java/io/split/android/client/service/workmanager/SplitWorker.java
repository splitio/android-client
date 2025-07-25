package io.split.android.client.service.workmanager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import io.split.android.android_client.BuildConfig;
import io.split.android.client.dtos.HttpProxyDto;
import io.split.android.client.network.BasicCredentialsProvider;
import io.split.android.client.network.BearerCredentialsProvider;
import io.split.android.client.network.CertificatePinningConfiguration;
import io.split.android.client.network.CertificatePinningConfigurationProvider;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpClientImpl;
import io.split.android.client.network.HttpProxy;
import io.split.android.client.network.SplitHttpHeadersBuilder;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.storage.cipher.SplitCipherFactory;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageFactory;
import io.split.android.client.storage.general.GeneralInfoStorage;
import io.split.android.client.utils.HttpProxySerializer;

public abstract class SplitWorker extends Worker {

    private final SplitRoomDatabase mDatabase;
    private final HttpClient mHttpClient;
    private final String mEndpoint;

    protected SplitTask mSplitTask;

    public SplitWorker(@NonNull Context context,
                       @NonNull WorkerParameters workerParams) {

        super(context, workerParams);
        Data inputData = workerParams.getInputData();
        String databaseName = inputData.getString(ServiceConstants.WORKER_PARAM_DATABASE_NAME);
        String apiKey = inputData.getString(ServiceConstants.WORKER_PARAM_API_KEY);
        mEndpoint = inputData.getString(ServiceConstants.WORKER_PARAM_ENDPOINT);
        mDatabase = SplitRoomDatabase.getDatabase(context, databaseName);
        mHttpClient = buildHttpClient(apiKey,
                buildCertPinningConfig(inputData.getString(ServiceConstants.WORKER_PARAM_CERTIFICATE_PINS)),
                buildProxyConfig(inputData.getString(ServiceConstants.WORKER_PARAM_USES_PROXY), mDatabase, apiKey));
    }

    private static HttpProxy buildProxyConfig(String usesProxy, SplitRoomDatabase database, String apiKey) {
        if (usesProxy == null) {
            return null;
        }

        GeneralInfoStorage storage = StorageFactory.getGeneralInfoStorage(database, SplitCipherFactory.create(apiKey, true));
        HttpProxyDto proxyConfigDto = HttpProxySerializer.deserialize(storage);
        if (proxyConfigDto == null) {
            return null;
        }

        if (proxyConfigDto.host == null) {
            return null;
        }

        HttpProxy.Builder builder = HttpProxy.newBuilder(proxyConfigDto.host, proxyConfigDto.port);

        addCredentialsProvider(proxyConfigDto, builder);
        addMtls(proxyConfigDto, builder);
        addCaCert(proxyConfigDto, builder);

        return builder.build();
    }

    private static void addCaCert(HttpProxyDto proxyConfigDto, HttpProxy.Builder builder) {
        if (proxyConfigDto.caCert != null) {
            InputStream caCertStream = stringToInputStream(proxyConfigDto.caCert);
            builder.proxyCacert(caCertStream);
        }
    }

    private static void addMtls(HttpProxyDto proxyConfigDto, HttpProxy.Builder builder) {
        if (proxyConfigDto.clientCert != null && proxyConfigDto.clientKey != null) {
            InputStream clientCertStream = stringToInputStream(proxyConfigDto.clientCert);
            InputStream clientKeyStream = stringToInputStream(proxyConfigDto.clientKey);
            builder.mtlsAuth(clientCertStream, clientKeyStream);
        }
    }

    private static void addCredentialsProvider(HttpProxyDto proxyConfigDto, HttpProxy.Builder builder) {
        if (proxyConfigDto.username != null && proxyConfigDto.password != null) {
            builder.credentialsProvider(new BasicCredentialsProvider() {
                @Override
                public String getUsername() {
                    return proxyConfigDto.username;
                }

                @Override
                public String getPassword() {
                    return proxyConfigDto.password;
                }
            });
        } else if (proxyConfigDto.bearerToken != null) {
            builder.credentialsProvider(new BearerCredentialsProvider() {

                @Override
                public String getToken() {
                    return proxyConfigDto.bearerToken;
                }
            });
        }
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

    private static HttpClient buildHttpClient(String apiKey, @Nullable CertificatePinningConfiguration certificatePinningConfiguration, HttpProxy proxyConfiguration) {
        HttpClientImpl.Builder builder = new HttpClientImpl.Builder();

        if (certificatePinningConfiguration != null) {
            builder.setCertificatePinningConfiguration(certificatePinningConfiguration);
        }

        if (proxyConfiguration != null) {
            builder.setProxy(proxyConfiguration);
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
    private static CertificatePinningConfiguration buildCertPinningConfig(@Nullable String pinsJson) {
        if (pinsJson == null || pinsJson.trim().isEmpty()) {
            return null;
        }

        return CertificatePinningConfigurationProvider.getCertificatePinningConfiguration(pinsJson);
    }

    private static InputStream stringToInputStream(String input) {
        if (input == null) {
            return null;
        }
        return new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    }
}
