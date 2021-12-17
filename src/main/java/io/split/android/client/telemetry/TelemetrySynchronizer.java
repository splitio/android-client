package io.split.android.client.telemetry;

public interface TelemetrySynchronizer {

    void synchronizeConfig();

    void synchronizeStats();

    void destroy();

    void flush();
}
