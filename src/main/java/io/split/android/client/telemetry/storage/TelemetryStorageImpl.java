package io.split.android.client.telemetry.storage;

import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import io.split.android.client.telemetry.model.Config;
import io.split.android.client.telemetry.model.EventsDataRecordsEnum;
import io.split.android.client.telemetry.model.FactoryCounter;
import io.split.android.client.telemetry.model.HTTPErrors;
import io.split.android.client.telemetry.model.HTTPLatencies;
import io.split.android.client.telemetry.model.HTTPLatenciesType;
import io.split.android.client.telemetry.model.ImpressionsDataType;
import io.split.android.client.telemetry.model.LastSync;
import io.split.android.client.telemetry.model.LastSynchronizationRecords;
import io.split.android.client.telemetry.model.Method;
import io.split.android.client.telemetry.model.MethodLatencies;
import io.split.android.client.telemetry.model.MethodExceptions;
import io.split.android.client.telemetry.model.SyncedResource;
import io.split.android.client.telemetry.model.StreamingEvent;
import io.split.android.client.telemetry.util.AtomicLongArray;

public class TelemetryStorageImpl implements TelemetryStorage {

    private static final int MAX_LATENCY_BUCKET_COUNT = 23;
    private static final int MAX_STREAMING_EVENTS = 20;
    private static final int MAX_TAGS = 10;

    private final Map<Method, AtomicLong> methodExceptionsCounter = Maps.newConcurrentMap();
    private final ConcurrentMap<Method, AtomicLongArray> methodLatencies = Maps.newConcurrentMap();

    private final Map<FactoryCounter, AtomicLong> factoryCounters = Maps.newConcurrentMap();

    private final Map<ImpressionsDataType, AtomicLong> impressionsData = Maps.newConcurrentMap();
    private final Map<EventsDataRecordsEnum, AtomicLong> eventsData = Maps.newConcurrentMap();

    private final Map<LastSynchronizationRecords, AtomicLong> lastSynchronizationData = Maps.newConcurrentMap();

    private final AtomicLong sessionLength = new AtomicLong();

    public TelemetryStorageImpl() {
        initializeMethodExceptionsCounter();
        initializeHttpLatenciesCounter();
        initializeFactoryCounters();
        initializeImpressionsData();
        initializeEventsData();
        initializeLastSynchronizationData();
    }

    private void initializeHttpLatenciesCounter() {
        methodLatencies.put(Method.TREATMENT, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        methodLatencies.put(Method.TREATMENTS, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        methodLatencies.put(Method.TREATMENT_WITH_CONFIG, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        methodLatencies.put(Method.TREATMENTS_WITH_CONFIG, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        methodLatencies.put(Method.TRACK, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
    }

    private void initializeMethodExceptionsCounter() {
        methodExceptionsCounter.put(Method.TREATMENT, new AtomicLong());
        methodExceptionsCounter.put(Method.TREATMENTS, new AtomicLong());
        methodExceptionsCounter.put(Method.TREATMENT_WITH_CONFIG, new AtomicLong());
        methodExceptionsCounter.put(Method.TREATMENTS_WITH_CONFIG, new AtomicLong());
        methodExceptionsCounter.put(Method.TRACK, new AtomicLong());
    }

    private void initializeFactoryCounters() {
        factoryCounters.put(FactoryCounter.BUR_TIMEOUTS, new AtomicLong());
        factoryCounters.put(FactoryCounter.NON_READY_USAGES, new AtomicLong());
    }

    private void initializeImpressionsData() {
        impressionsData.put(ImpressionsDataType.IMPRESSIONS_QUEUED, new AtomicLong());
        impressionsData.put(ImpressionsDataType.IMPRESSIONS_DEDUPED, new AtomicLong());
        impressionsData.put(ImpressionsDataType.IMPRESSIONS_DROPPED, new AtomicLong());
    }

    private void initializeEventsData() {
        eventsData.put(EventsDataRecordsEnum.EVENTS_DROPPED, new AtomicLong());
        eventsData.put(EventsDataRecordsEnum.EVENTS_QUEUED, new AtomicLong());
    }

    private void initializeLastSynchronizationData() {
        lastSynchronizationData.put(LastSynchronizationRecords.IMPRESSIONS, new AtomicLong());
        lastSynchronizationData.put(LastSynchronizationRecords.IMPRESSIONS_COUNT, new AtomicLong());
        lastSynchronizationData.put(LastSynchronizationRecords.TELEMETRY, new AtomicLong());
        lastSynchronizationData.put(LastSynchronizationRecords.EVENTS, new AtomicLong());
        lastSynchronizationData.put(LastSynchronizationRecords.MY_SEGMENT, new AtomicLong());
        lastSynchronizationData.put(LastSynchronizationRecords.SPLITS, new AtomicLong());
        lastSynchronizationData.put(LastSynchronizationRecords.TOKEN, new AtomicLong());
    }

    @Override
    public MethodExceptions popExceptions() {
        MethodExceptions methodExceptions = new MethodExceptions();

        methodExceptions.setTreatment(methodExceptionsCounter.get(Method.TREATMENT).getAndSet(0L));
        methodExceptions.setTreatments(methodExceptionsCounter.get(Method.TREATMENTS).getAndSet(0L));
        methodExceptions.setTreatmentWithConfig(methodExceptionsCounter.get(Method.TREATMENT_WITH_CONFIG).getAndSet(0L));
        methodExceptions.setTreatmentsWithConfig(methodExceptionsCounter.get(Method.TREATMENTS_WITH_CONFIG).getAndSet(0L));
        methodExceptions.setTrack(methodExceptionsCounter.get(Method.TRACK).getAndSet(0L));

        return methodExceptions;
    }

    @Override
    public MethodLatencies popLatencies() {
        MethodLatencies latencies = new MethodLatencies();

        latencies.setTreatment(methodLatencies.get(Method.TREATMENT).fetchAndClearAll());
        latencies.setTreatments(methodLatencies.get(Method.TREATMENTS).fetchAndClearAll());
        latencies.setTreatmentWithConfig(methodLatencies.get(Method.TREATMENT_WITH_CONFIG).fetchAndClearAll());
        latencies.setTreatmentsWithConfig(methodLatencies.get(Method.TREATMENTS_WITH_CONFIG).fetchAndClearAll());
        latencies.setTrack(methodLatencies.get(Method.TRACK).fetchAndClearAll());

        return latencies;
    }

    @Override
    public void recordLatency(Method method, long latency) {
        BinarySearchLatencyTracker binarySearchLatencyTracker = new BinarySearchLatencyTracker();
        long bucketForLatencyMillis = binarySearchLatencyTracker.getBucketForLatencyMillis(latency);

        methodLatencies.get(method).increment((int) bucketForLatencyMillis);
    }

    @Override
    public void recordException(Method method) {
        methodExceptionsCounter.get(method).incrementAndGet();
    }

    @Override
    public long getBURTimeouts() {
        return factoryCounters.get(FactoryCounter.BUR_TIMEOUTS).get();
    }

    @Override
    public long getNonReadyUsage() {
        return factoryCounters.get(FactoryCounter.NON_READY_USAGES).get();
    }

    @Override
    public void recordConfig(Config config) {

    }

    @Override
    public void recordBURTimeout() {
        factoryCounters.get(FactoryCounter.BUR_TIMEOUTS).incrementAndGet();
    }

    @Override
    public void recordNonReadyUsage() {
        factoryCounters.get(FactoryCounter.NON_READY_USAGES).incrementAndGet();
    }

    @Override
    public long getImpressionsStats(ImpressionsDataType type) {
        return impressionsData.get(type).get();
    }

    @Override
    public long getEventsStats(EventsDataRecordsEnum type) {
        return eventsData.get(type).get();
    }

    @Override
    public LastSync getLastSynchronization() {
        LastSync lastSync = new LastSync();

        lastSync.setLastEventSync(lastSynchronizationData.get(LastSynchronizationRecords.EVENTS).get());
        lastSync.setLastSplitSync(lastSynchronizationData.get(LastSynchronizationRecords.SPLITS).get());
        lastSync.setLastSegmentSync(lastSynchronizationData.get(LastSynchronizationRecords.MY_SEGMENT).get());
        lastSync.setLastTelemetrySync(lastSynchronizationData.get(LastSynchronizationRecords.TELEMETRY).get());
        lastSync.setLastImpressionSync(lastSynchronizationData.get(LastSynchronizationRecords.IMPRESSIONS).get());
        lastSync.setLastImpressionCountSync(lastSynchronizationData.get(LastSynchronizationRecords.IMPRESSIONS_COUNT).get());
        lastSync.setLastTokenRefresh(lastSynchronizationData.get(LastSynchronizationRecords.TOKEN).get());

        return lastSync;
    }

    @Override
    public HTTPErrors popHTTPErrors() {
        return null;
    }

    @Override
    public HTTPLatencies popHttpLatencies() {
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
        impressionsData.get(dataType).addAndGet(count);
    }

    @Override
    public void recordEventStats(EventsDataRecordsEnum dataType, long count) {
        eventsData.get(dataType).addAndGet(count);
    }

    @Override
    public void recordSuccessfulSync(LastSynchronizationRecords resource, long time) {
        lastSynchronizationData.put(resource, new AtomicLong(time));
    }

    @Override
    public void recordSyncError(SyncedResource syncedResource, int status) {

    }

    @Override
    public void recordSyncLatency(HTTPLatenciesType resource, long latency) {

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
