package tests.workmanager;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.impl.model.WorkSpec;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import io.split.android.client.ServiceEndpoints;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFilter;
import io.split.android.client.network.CertificatePinningConfiguration;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.synchronizer.WorkManagerWrapper;
import io.split.android.client.service.workmanager.EventsRecorderWorker;
import io.split.android.client.service.workmanager.ImpressionsRecorderWorker;
import io.split.android.client.service.workmanager.MyLargeSegmentsSyncWorker;
import io.split.android.client.service.workmanager.MySegmentsSyncWorker;
import io.split.android.client.service.workmanager.splits.SplitsSyncWorker;

public class WorkManagerWrapperTest {

    @Mock
    private WorkManager mWorkManager;
    private WorkManagerWrapper mWrapper;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        when(mWorkManager.getWorkInfosByTagLiveData(any())).thenReturn(mock(LiveData.class));

        SplitClientConfig splitClientConfig = buildConfig(false);

        mWrapper = getWrapper(splitClientConfig);
    }

    private static @NonNull SplitClientConfig buildConfig(boolean largeSegmentsEnabled) {
        SplitClientConfig config = new SplitClientConfig.Builder()
                .serviceEndpoints(
                        ServiceEndpoints.builder()
                                .sseAuthServiceEndpoint("https://test.split.io/serviceEndpoint")
                                .apiEndpoint("https://test.split.io/api")
                                .eventsEndpoint("https://test.split.io/events")
                                .telemetryServiceEndpoint("https://test.split.io/telemetry")
                                .build()
                )
                .largeSegmentsEnabled(largeSegmentsEnabled)
                .synchronizeInBackgroundPeriod(5263)
                .eventsPerPush(526)
                .impressionsPerPush(256)
                .backgroundSyncWhenWifiOnly(true)
                .backgroundSyncWhenBatteryNotLow(false)
                .certificatePinningConfiguration(CertificatePinningConfiguration.builder()
                        .addPin("events.split.io", "sha256/sDKdggs")
                        .addPin("sdk.split.io", "sha256/jIUe51")
                        .addPin("events.split.io", "sha1/jLeisDf")
                        .build())
                .build();

        try {
            Method method = config.getClass().getDeclaredMethod("enableTelemetry");
            method.setAccessible(true);
            method.invoke(config);
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return config;
    }

    private @NonNull WorkManagerWrapper getWrapper(SplitClientConfig splitClientConfig) {
        return new WorkManagerWrapper(
                mWorkManager,
                splitClientConfig,
                "api_key",
                "test_database_name",
                SplitFilter.bySet(Arrays.asList("set_1", "set_2"))
        );
    }

    @Test
    public void removeWorkCancelsJobs() {
        mWrapper.removeWork();

        verify(mWorkManager).cancelUniqueWork(SplitTaskType.SPLITS_SYNC.toString());
        verify(mWorkManager).cancelUniqueWork(SplitTaskType.MY_SEGMENTS_SYNC.toString());
        verify(mWorkManager).cancelUniqueWork(SplitTaskType.EVENTS_RECORDER.toString());
        verify(mWorkManager).cancelUniqueWork(SplitTaskType.IMPRESSIONS_RECORDER.toString());
        verify(mWorkManager).cancelUniqueWork(SplitTaskType.UNIQUE_KEYS_RECORDER_TASK.toString());
        verify(mWorkManager).cancelUniqueWork(SplitTaskType.MY_LARGE_SEGMENT_SYNC.toString());
    }

    @Test
    public void scheduleWorkSchedulesSplitsJob() {
        mWrapper.scheduleWork();

        Data inputData = new Data.Builder()
                .putLong("splitCacheExpiration", 864000)
                .putString("endpoint", "https://test.split.io/api")
                .putBoolean("shouldRecordTelemetry", true)
                .putStringArray("configuredFilterValues", new String[]{"set_1", "set_2"})
                .putString("configuredFilterType", SplitFilter.Type.BY_SET.queryStringField())
                .putString("flagsSpec", "1.1")
                .putString("certificatePins", certificatePinsJson())
                .build();

        PeriodicWorkRequest expectedRequest = new PeriodicWorkRequest
                .Builder(SplitsSyncWorker.class, 5263, TimeUnit.MINUTES)
                .setInputData(buildInputData(inputData))
                .setConstraints(buildConstraints())
                .setInitialDelay(15L, TimeUnit.MINUTES)
                .build();

        ArgumentCaptor<PeriodicWorkRequest> argumentCaptor = ArgumentCaptor.forClass(PeriodicWorkRequest.class);

        verify(mWorkManager).enqueueUniquePeriodicWork(
                eq(SplitTaskType.SPLITS_SYNC.toString()),
                eq(ExistingPeriodicWorkPolicy.REPLACE),
                argumentCaptor.capture()
        );

        assertWorkSpecMatches(argumentCaptor.getValue().getWorkSpec(), expectedRequest.getWorkSpec());
    }

    @Test
    public void scheduleWorkSchedulesEventsJob() {
        mWrapper.scheduleWork();

        Data inputData = new Data.Builder()
                .putString("endpoint", "https://test.split.io/events")
                .putInt("eventsPerPush", 526)
                .putBoolean("shouldRecordTelemetry", true)
                .putString("certificatePins", certificatePinsJson())
                .build();

        PeriodicWorkRequest expectedRequest = new PeriodicWorkRequest
                .Builder(EventsRecorderWorker.class, 5263, TimeUnit.MINUTES)
                .setInputData(buildInputData(inputData))
                .setConstraints(buildConstraints())
                .setInitialDelay(15L, TimeUnit.MINUTES)
                .build();

        ArgumentCaptor<PeriodicWorkRequest> argumentCaptor = ArgumentCaptor.forClass(PeriodicWorkRequest.class);

        verify(mWorkManager).enqueueUniquePeriodicWork(
                eq(SplitTaskType.EVENTS_RECORDER.toString()),
                eq(ExistingPeriodicWorkPolicy.REPLACE),
                argumentCaptor.capture()
        );

        assertWorkSpecMatches(argumentCaptor.getValue().getWorkSpec(), expectedRequest.getWorkSpec());
    }

    @Test
    public void scheduleWorkSchedulesImpressionsJob() {
        mWrapper.scheduleWork();

        Data inputData = new Data.Builder()
                .putString("endpoint", "https://test.split.io/events")
                .putInt("impressionsPerPush", 256)
                .putString("certificatePins", certificatePinsJson())
                .putBoolean("shouldRecordTelemetry", true).build();

        PeriodicWorkRequest expectedRequest = new PeriodicWorkRequest
                .Builder(ImpressionsRecorderWorker.class, 5263, TimeUnit.MINUTES)
                .setInputData(buildInputData(inputData))
                .setConstraints(buildConstraints())
                .setInitialDelay(15L, TimeUnit.MINUTES)
                .build();

        ArgumentCaptor<PeriodicWorkRequest> argumentCaptor = ArgumentCaptor.forClass(PeriodicWorkRequest.class);

        verify(mWorkManager).enqueueUniquePeriodicWork(
                eq(SplitTaskType.IMPRESSIONS_RECORDER.toString()),
                eq(ExistingPeriodicWorkPolicy.REPLACE),
                argumentCaptor.capture()
        );

        assertWorkSpecMatches(argumentCaptor.getValue().getWorkSpec(), expectedRequest.getWorkSpec());
    }

    @Test
    public void scheduleMySegmentsWorkSchedulesJob() {
        HashSet<String> keys = new HashSet<>();
        keys.add("key1");
        keys.add("key2");
        mWrapper.scheduleMySegmentsWork(keys);

        Data.Builder dataBuilder = new Data.Builder();
        String[] keysArray = new String[keys.size()];
        keys.toArray(keysArray);
        dataBuilder.putString("endpoint", "https://test.split.io/api");
        dataBuilder.putStringArray("key", keysArray);
        dataBuilder.putBoolean("shouldRecordTelemetry", true);
        dataBuilder.putString("certificatePins", certificatePinsJson());

        PeriodicWorkRequest expectedRequest = new PeriodicWorkRequest
                .Builder(MySegmentsSyncWorker.class, 5263, TimeUnit.MINUTES)
                .setInputData(buildInputData(dataBuilder.build()))
                .setConstraints(buildConstraints())
                .setInitialDelay(15L, TimeUnit.MINUTES)
                .build();

        ArgumentCaptor<PeriodicWorkRequest> argumentCaptor = ArgumentCaptor.forClass(PeriodicWorkRequest.class);

        verify(mWorkManager).enqueueUniquePeriodicWork(
                eq(SplitTaskType.MY_SEGMENTS_SYNC.toString()),
                eq(ExistingPeriodicWorkPolicy.REPLACE),
                argumentCaptor.capture()
        );
        verify(mWorkManager, times(0)).enqueueUniquePeriodicWork(
                eq(SplitTaskType.MY_LARGE_SEGMENT_SYNC.toString()),
                eq(ExistingPeriodicWorkPolicy.REPLACE),
                argumentCaptor.capture()
        );

        assertWorkSpecMatches(argumentCaptor.getValue().getWorkSpec(), expectedRequest.getWorkSpec());
    }

    @Test
    public void scheduleMySegmentsWorkSchedulesWorkWhenLargeSegmentsIsEnabled() {
        mWrapper = getWrapper(buildConfig(true));

        HashSet<String> keys = new HashSet<>();
        keys.add("key1");
        keys.add("key2");
        mWrapper.scheduleMySegmentsWork(keys);

        Data.Builder dataBuilder = new Data.Builder();
        String[] keysArray = new String[keys.size()];
        keys.toArray(keysArray);
        dataBuilder.putString("endpoint", "https://test.split.io/api");
        dataBuilder.putStringArray("key", keysArray);
        dataBuilder.putBoolean("shouldRecordTelemetry", true);
        dataBuilder.putString("certificatePins", certificatePinsJson());

        PeriodicWorkRequest expectedRequest = new PeriodicWorkRequest
                .Builder(MyLargeSegmentsSyncWorker.class, 5263, TimeUnit.MINUTES)
                .setInputData(buildInputData(dataBuilder.build()))
                .setConstraints(buildConstraints())
                .setInitialDelay(15L, TimeUnit.MINUTES)
                .build();

        ArgumentCaptor<PeriodicWorkRequest> argumentCaptor = ArgumentCaptor.forClass(PeriodicWorkRequest.class);

        verify(mWorkManager).enqueueUniquePeriodicWork(
                eq(SplitTaskType.MY_LARGE_SEGMENT_SYNC.toString()),
                eq(ExistingPeriodicWorkPolicy.REPLACE),
                argumentCaptor.capture()
        );

        assertWorkSpecMatches(argumentCaptor.getValue().getWorkSpec(), expectedRequest.getWorkSpec());
    }

    private void assertWorkSpecMatches(WorkSpec workSpec, WorkSpec expectedWorkSpec) {
        assertEquals(expectedWorkSpec.backoffPolicy, workSpec.backoffPolicy);
        assertEquals(expectedWorkSpec.backoffDelayDuration, workSpec.backoffDelayDuration);
        assertEquals(expectedWorkSpec.constraints, workSpec.constraints);
        assertEquals(expectedWorkSpec.initialDelay, workSpec.initialDelay);
        assertEquals(expectedWorkSpec.input, workSpec.input);
        assertEquals(expectedWorkSpec.minimumRetentionDuration, workSpec.minimumRetentionDuration);
        assertEquals(expectedWorkSpec.periodStartTime, workSpec.periodStartTime);
        assertEquals(expectedWorkSpec.workerClassName, workSpec.workerClassName);
        assertEquals(expectedWorkSpec.isBackedOff(), workSpec.isBackedOff());
        assertEquals(expectedWorkSpec.isPeriodic(), workSpec.isPeriodic());
    }

    private Data buildInputData(Data customData) {
        Data.Builder dataBuilder = new Data.Builder();
        dataBuilder.putString("databaseName", "test_database_name");
        dataBuilder.putString("apiKey", "api_key");
        dataBuilder.putBoolean("encryptionEnabled", false);
        if (customData != null) {
            dataBuilder.putAll(customData);
        }
        return dataBuilder.build();
    }

    private Constraints buildConstraints() {
        Constraints.Builder constraintsBuilder = new Constraints.Builder();
        constraintsBuilder.setRequiredNetworkType(NetworkType.UNMETERED);
        constraintsBuilder.setRequiresBatteryNotLow(false);
        return constraintsBuilder.build();
    }

    @NonNull
    private static String certificatePinsJson() {
        return "{\"events.split.io\":[{\"algo\":\"sha256\",\"pin\":[-80,50,-99,-126,11]},{\"algo\":\"sha1\",\"pin\":[-116,-73,-94,-80,55]}],\"sdk.split.io\":[{\"algo\":\"sha256\",\"pin\":[-116,-123,30,-25]}]}";
    }
}
