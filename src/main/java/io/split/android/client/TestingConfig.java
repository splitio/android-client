package io.split.android.client;

import io.split.android.android_client.BuildConfig;
import io.split.android.client.service.ServiceConstants;

public class TestingConfig {
    private int cdnBackoffTime = ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_WAIT;
    private String mFlagsSpec = BuildConfig.FLAGS_SPEC;

    public TestingConfig() {
    }

    public int getCdnBackoffTime() {
        return cdnBackoffTime;
    }

    public void setCdnBackoffTime(int cdnBackoffTime) {
        this.cdnBackoffTime = cdnBackoffTime;
    }

    String getFlagsSpec() {
        return mFlagsSpec;
    }

    public void setFlagsSpec(String flagsSpec) {
        mFlagsSpec = flagsSpec;
    }
}
