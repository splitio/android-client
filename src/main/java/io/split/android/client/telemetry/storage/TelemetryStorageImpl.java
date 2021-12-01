package io.split.android.client.telemetry.storage;

import com.google.common.collect.Maps;

import java.util.ArrayList;
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
import io.split.android.client.telemetry.model.MethodExceptions;
import io.split.android.client.telemetry.model.MethodLatencies;
import io.split.android.client.telemetry.model.PushCounterEvent;
import io.split.android.client.telemetry.model.StreamingEvent;
import io.split.android.client.telemetry.model.SyncedResource;
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

    private final Map<SyncedResource, Map<Long, Long>> httpErrors = Maps.newConcurrentMap();

    private final Map<HTTPLatenciesType, AtomicLongArray> httpLatencies = Maps.newConcurrentMap();

    private final Map<PushCounterEvent, AtomicLong> pushCounters = Maps.newConcurrentMap();

    private final Object streamingEventsLock = new Object();
    private final List<StreamingEvent> streamingEvents = new ArrayList<>();

    private final ILatencyTracker latencyTracker;

    public TelemetryStorageImpl(ILatencyTracker latencyTracker) {
        this.latencyTracker = latencyTracker;

        initializeMethodExceptionsCounter();
        initializeHttpLatenciesCounter();
        initializeFactoryCounters();
        initializeImpressionsData();
        initializeEventsData();
        initializeLastSynchronizationData();
        initializeHttpErrors();
        initializeHttpLatencies();
        initializePushCounters();
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

    private void initializeHttpErrors() {
        httpErrors.put(SyncedResource.EVENT_SYNC, Maps.newConcurrentMap());
        httpErrors.put(SyncedResource.SPLIT_SYNC, Maps.newConcurrentMap());
        httpErrors.put(SyncedResource.SEGMENT_SYNC, Maps.newConcurrentMap());
        httpErrors.put(SyncedResource.TELEMETRY_SYNC, Maps.newConcurrentMap());
        httpErrors.put(SyncedResource.MY_SEGMENT_SYNC, Maps.newConcurrentMap());
        httpErrors.put(SyncedResource.IMPRESSION_COUNT_SYNC, Maps.newConcurrentMap());
        httpErrors.put(SyncedResource.IMPRESSION_SYNC, Maps.newConcurrentMap());
        httpErrors.put(SyncedResource.TOKEN_SYNC, Maps.newConcurrentMap());
    }

    private void initializeHttpLatencies() {
        httpLatencies.put(HTTPLatenciesType.EVENTS, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        httpLatencies.put(HTTPLatenciesType.IMPRESSIONS, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        httpLatencies.put(HTTPLatenciesType.TELEMETRY, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        httpLatencies.put(HTTPLatenciesType.IMPRESSIONS_COUNT, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        httpLatencies.put(HTTPLatenciesType.MY_SEGMENT, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        httpLatencies.put(HTTPLatenciesType.SPLITS, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        httpLatencies.put(HTTPLatenciesType.TOKEN, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
    }

    private void initializePushCounters() {
        pushCounters.put(PushCounterEvent.AUTH_REJECTIONS, new AtomicLong());
        pushCounters.put(PushCounterEvent.TOKEN_REFRESHES, new AtomicLong());
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
        long bucketForLatencyMillis = latencyTracker.getBucketForLatencyMillis(latency);

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
        HTTPErrors errors = new HTTPErrors();

        errors.setEventsSyncErrs(httpErrors.get(SyncedResource.EVENT_SYNC));
        errors.setImpressionCountSyncErrs(httpErrors.get(SyncedResource.IMPRESSION_COUNT_SYNC));
        errors.setTelemetrySyncErrs(httpErrors.get(SyncedResource.TELEMETRY_SYNC));
        errors.setImpressionSyncErrs(httpErrors.get(SyncedResource.IMPRESSION_SYNC));
        errors.setSplitSyncErrs(httpErrors.get(SyncedResource.SPLIT_SYNC));
        errors.setSegmentSyncErrs(httpErrors.get(SyncedResource.MY_SEGMENT_SYNC));
        errors.setTokenGetErrs(httpErrors.get(SyncedResource.TOKEN_SYNC));

        initializeHttpErrors();

        return errors;
    }

    @Override
    public HTTPLatencies popHttpLatencies() {
        HTTPLatencies latencies = new HTTPLatencies();

        latencies.setTelemetry(httpLatencies.get(HTTPLatenciesType.TELEMETRY).fetchAndClearAll());
        latencies.setEvents(httpLatencies.get(HTTPLatenciesType.EVENTS).fetchAndClearAll());
        latencies.setSplits(httpLatencies.get(HTTPLatenciesType.SPLITS).fetchAndClearAll());
        latencies.setSegments(httpLatencies.get(HTTPLatenciesType.MY_SEGMENT).fetchAndClearAll());
        latencies.setToken(httpLatencies.get(HTTPLatenciesType.TOKEN).fetchAndClearAll());
        latencies.setImpressions(httpLatencies.get(HTTPLatenciesType.IMPRESSIONS).fetchAndClearAll());
        latencies.setImpressionsCount(httpLatencies.get(HTTPLatenciesType.IMPRESSIONS_COUNT).fetchAndClearAll());

        return latencies;
    }

    @Override
    public long popAuthRejections() {
        return pushCounters.get(PushCounterEvent.AUTH_REJECTIONS).getAndSet(0);
    }

    @Override
    public long popTokenRefreshes() {
        return pushCounters.get(PushCounterEvent.TOKEN_REFRESHES).getAndSet(0);
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
        return sessionLength.get();
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
        Map<Long, Long> statusMap = httpErrors.get(syncedResource);
        if (statusMap == null) {
            return;
        }

        if (!statusMap.containsKey((long) status)) {
            statusMap.put((long) status, 0L);
        }

        statusMap.put((long) status, statusMap.get((long) status) + 1L);
    }

    @Override
    public void recordSyncLatency(HTTPLatenciesType resource, long latency) {
        httpLatencies.get(resource).increment((int) latencyTracker.getBucketForLatencyMillis(latency));
    }

    @Override
    public void recordAuthRejections() {
        pushCounters.get(PushCounterEvent.AUTH_REJECTIONS).incrementAndGet();
    }

    @Override
    public void recordTokenRefreshes() {
        pushCounters.get(PushCounterEvent.TOKEN_REFRESHES).incrementAndGet();
    }

    @Override
    public void recordStreamingEvents(StreamingEvent streamingEvent) {
        synchronized (streamingEvents) {
            if (streamingEvents.size() < MAX_STREAMING_EVENTS) {
                streamingEvents.add(streamingEvent);
            }
        }
    }

    @Override
    public void recordSessionLength(long sessionLength) {
        this.sessionLength.set(sessionLength);
    }
}
