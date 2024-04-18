package io.split.android.client.service.workmanager;

import androidx.work.WorkerParameters;

import io.split.android.client.service.ServiceConstants;

class SplitsSyncWorkerParams {

    private final boolean mShouldRecordTelemetry;
    private final String mApiKey;
    private final boolean mEncryptionEnabled;
    private final String mConfiguredFilterType;
    private final String[] mConfiguredFilterValues;
    private final String mFlagsSpec;

    SplitsSyncWorkerParams(WorkerParameters workerParameters) {
        this(workerParameters.getInputData().getBoolean(ServiceConstants.SHOULD_RECORD_TELEMETRY, false),
                workerParameters.getInputData().getString(ServiceConstants.WORKER_PARAM_API_KEY),
                workerParameters.getInputData().getBoolean(ServiceConstants.WORKER_PARAM_ENCRYPTION_ENABLED, false),
                workerParameters.getInputData().getString(ServiceConstants.WORKER_PARAM_CONFIGURED_FILTER_TYPE),
                workerParameters.getInputData().getStringArray(ServiceConstants.WORKER_PARAM_CONFIGURED_FILTER_VALUES),
                workerParameters.getInputData().getString(ServiceConstants.WORKER_PARAM_FLAGS_SPEC)
        );
    }

    public SplitsSyncWorkerParams(boolean shouldRecordTelemetry,
                                  String apiKey,
                                  boolean encryptionEnabled,
                                  String configuredFilterType,
                                  String[] configuredFilterValues,
                                  String flagsSpec) {
        mShouldRecordTelemetry = shouldRecordTelemetry;
        mApiKey = apiKey;
        mEncryptionEnabled = encryptionEnabled;
        mConfiguredFilterType = configuredFilterType;
        mConfiguredFilterValues = configuredFilterValues;
        mFlagsSpec = flagsSpec;
    }

    boolean shouldRecordTelemetry() {
        return mShouldRecordTelemetry;
    }

    String apiKey() {
        return mApiKey;
    }

    boolean encryptionEnabled() {
        return mEncryptionEnabled;
    }

    String configuredFilterType() {
        return mConfiguredFilterType;
    }

    String[] configuredFilterValues() {
        return mConfiguredFilterValues;
    }

    String flagsSpec() {
        return mFlagsSpec;
    }
}
