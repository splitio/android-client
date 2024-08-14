package io.split.android.client.service.synchronizer;

public interface LastUpdateTimestampProvider {

    long getLastUpdateTimestamp();

    void setLastUpdateTimestamp(long timestamp);
}
