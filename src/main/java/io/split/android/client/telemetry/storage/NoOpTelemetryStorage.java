package io.split.android.client.telemetry.storage;

import java.util.List;

import io.split.android.client.telemetry.model.EventsDataRecordsEnum;
import io.split.android.client.telemetry.model.HttpErrors;
import io.split.android.client.telemetry.model.HttpLatencies;
import io.split.android.client.telemetry.model.ImpressionsDataType;
import io.split.android.client.telemetry.model.LastSync;
import io.split.android.client.telemetry.model.Method;
import io.split.android.client.telemetry.model.MethodExceptions;
import io.split.android.client.telemetry.model.MethodLatencies;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.model.streaming.StreamingEvent;

public class NoOpTelemetryStorage implements TelemetryStorage {

    @Override
    public MethodExceptions popExceptions() {
        return null;
    }

    @Override
    public MethodLatencies popLatencies() {
        return null;
    }

    @Override
    public void recordLatency(Method method, long latency) {

    }

    @Override
    public void recordException(Method method) {

    }


    @Override
    public long getNonReadyUsage() {
        return 0;
    }

    @Override
    public long getActiveFactories() {
        return 0;
    }

    @Override
    public long getRedundantFactories() {
        return 0;
    }

    @Override
    public long getTimeUntilReady() {
        return 0;
    }

    @Override
    public long getTimeUntilReadyFromCache() {
        return 0;
    }

    @Override
    public void recordNonReadyUsage() {

    }

    @Override
    public void recordActiveFactories(int count) {

    }

    @Override
    public void recordRedundantFactories(int count) {

    }

    @Override
    public void recordTimeUntilReady(long time) {

    }

    @Override
    public void recordTimeUntilReadyFromCache(long timeUntilReadyFromCache) {

    }

    @Override
    public long getImpressionsStats(ImpressionsDataType type) {
        return 0;
    }

    @Override
    public long getEventsStats(EventsDataRecordsEnum type) {
        return 0;
    }

    @Override
    public LastSync getLastSynchronization() {
        return null;
    }

    @Override
    public HttpErrors popHttpErrors() {
        return null;
    }

    @Override
    public HttpLatencies popHttpLatencies() {
        return null;
    }

    @Override
    public long popAuthRejections() {
        return 0;
    }

    @Override
    public long popTokenRefreshes() {
        return 0;
    }

    @Override
    public List<StreamingEvent> popStreamingEvents() {
        return null;
    }

    @Override
    public List<String> popTags() {
        return null;
    }

    @Override
    public long getSessionLength() {
        return 0;
    }

    @Override
    public void addTag(String tag) {

    }

    @Override
    public void recordImpressionStats(ImpressionsDataType dataType, long count) {

    }

    @Override
    public void recordEventStats(EventsDataRecordsEnum dataType, long count) {

    }

    @Override
    public void recordSuccessfulSync(OperationType resource, long time) {

    }

    @Override
    public void recordSyncError(OperationType syncedResource, int status) {

    }

    @Override
    public void recordSyncLatency(OperationType resource, long latency) {

    }

    @Override
    public void recordAuthRejections() {

    }

    @Override
    public void recordTokenRefreshes() {

    }

    @Override
    public void recordStreamingEvents(StreamingEvent streamingEvent) {

    }

    @Override
    public void recordSessionLength(long sessionLength) {

    }
}
