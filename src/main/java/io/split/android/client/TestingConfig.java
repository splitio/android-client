package io.split.android.client;

import io.split.android.client.service.ServiceConstants;

public class TestingConfig {
    private int cdnBackoffTime = ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_WAIT;

    public TestingConfig() {
    }

    public int getCdnBackoffTime() {
        return cdnBackoffTime;
    }

    public void setCdnBackoffTime(int cdnBackoffTime) {
        this.cdnBackoffTime = cdnBackoffTime;
    }
}
