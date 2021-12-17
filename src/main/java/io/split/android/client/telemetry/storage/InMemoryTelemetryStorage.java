package io.split.android.client.telemetry.storage;

import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import io.split.android.client.telemetry.model.EventsDataRecordsEnum;
import io.split.android.client.telemetry.model.FactoryCounter;
import io.split.android.client.telemetry.model.HttpErrors;
import io.split.android.client.telemetry.model.HttpLatencies;
import io.split.android.client.telemetry.model.ImpressionsDataType;
import io.split.android.client.telemetry.model.LastSync;
import io.split.android.client.telemetry.model.Method;
import io.split.android.client.telemetry.model.MethodExceptions;
import io.split.android.client.telemetry.model.MethodLatencies;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.model.PushCounterEvent;
import io.split.android.client.telemetry.model.streaming.StreamingEvent;
import io.split.android.client.telemetry.util.AtomicLongArray;

public class InMemoryTelemetryStorage implements TelemetryStorage {

    private static final int MAX_LATENCY_BUCKET_COUNT = 23;
    private static final int MAX_STREAMING_EVENTS = 20;
    private static final int MAX_TAGS = 10;

    private final Map<Method, AtomicLong> methodExceptionsCounter = Maps.newConcurrentMap();
    private final ConcurrentMap<Method, AtomicLongArray> methodLatencies = Maps.newConcurrentMap();

    private final Map<FactoryCounter, AtomicLong> factoryCounters = Maps.newConcurrentMap();

    private final Map<ImpressionsDataType, AtomicLong> impressionsData = Maps.newConcurrentMap();
    private final Map<EventsDataRecordsEnum, AtomicLong> eventsData = Maps.newConcurrentMap();

    private final Map<OperationType, AtomicLong> lastSynchronizationData = Maps.newConcurrentMap();

    private final AtomicLong sessionLength = new AtomicLong();

    private final Object httpErrorsLock = new Object();
    private final Map<OperationType, Map<Long, Long>> httpErrors = Maps.newConcurrentMap();

    private final Map<OperationType, AtomicLongArray> httpLatencies = Maps.newConcurrentMap();

    private final Map<PushCounterEvent, AtomicLong> pushCounters = Maps.newConcurrentMap();

    private final Object streamingEventsLock = new Object();
    private List<StreamingEvent> streamingEvents = new ArrayList<>();

    private final Object tagsLock = new Object();
    private final Set<String> tags = new HashSet<>();

    private final ILatencyTracker latencyTracker;

    public InMemoryTelemetryStorage(ILatencyTracker latencyTracker) {
        this.latencyTracker = latencyTracker;

        initializeProperties();
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

        lastSync.setLastEventSync(lastSynchronizationData.get(OperationType.EVENTS).get());
        lastSync.setLastSplitSync(lastSynchronizationData.get(OperationType.SPLITS).get());
        lastSync.setLastMySegmentSync(lastSynchronizationData.get(OperationType.MY_SEGMENT).get());
        lastSync.setLastTelemetrySync(lastSynchronizationData.get(OperationType.TELEMETRY).get());
        lastSync.setLastImpressionSync(lastSynchronizationData.get(OperationType.IMPRESSIONS).get());
        lastSync.setLastImpressionCountSync(lastSynchronizationData.get(OperationType.IMPRESSIONS_COUNT).get());
        lastSync.setLastTokenRefresh(lastSynchronizationData.get(OperationType.TOKEN).get());

        return lastSync;
    }

    @Override
    public HttpErrors popHttpErrors() {
        HttpErrors errors = new HttpErrors();

        errors.setEventsSyncErrs(httpErrors.get(OperationType.EVENTS));
        errors.setImpressionCountSyncErrs(httpErrors.get(OperationType.IMPRESSIONS_COUNT));
        errors.setTelemetrySyncErrs(httpErrors.get(OperationType.TELEMETRY));
        errors.setImpressionSyncErrs(httpErrors.get(OperationType.IMPRESSIONS));
        errors.setSplitSyncErrs(httpErrors.get(OperationType.SPLITS));
        errors.setMySegmentSyncErrs(httpErrors.get(OperationType.MY_SEGMENT));
        errors.setTokenGetErrs(httpErrors.get(OperationType.TOKEN));

        initializeHttpErrors();

        return errors;
    }

    @Override
    public HttpLatencies popHttpLatencies() {
        HttpLatencies latencies = new HttpLatencies();

        latencies.setTelemetry(httpLatencies.get(OperationType.TELEMETRY).fetchAndClearAll());
        latencies.setEvents(httpLatencies.get(OperationType.EVENTS).fetchAndClearAll());
        latencies.setSplits(httpLatencies.get(OperationType.SPLITS).fetchAndClearAll());
        latencies.setMySegments(httpLatencies.get(OperationType.MY_SEGMENT).fetchAndClearAll());
        latencies.setToken(httpLatencies.get(OperationType.TOKEN).fetchAndClearAll());
        latencies.setImpressions(httpLatencies.get(OperationType.IMPRESSIONS).fetchAndClearAll());
        latencies.setImpressionsCount(httpLatencies.get(OperationType.IMPRESSIONS_COUNT).fetchAndClearAll());

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
        synchronized (streamingEventsLock) {
            List<StreamingEvent> streamingEventsList = streamingEvents;
            streamingEvents = new ArrayList<>();

            return streamingEventsList;
        }
    }

    @Override
    public List<String> popTags() {
        synchronized (tagsLock) {
            List<String> tagList = new ArrayList<>(tags);
            tags.clear();

            return tagList;
        }
    }

    @Override
    public long getSessionLength() {
        return sessionLength.get();
    }

    @Override
    public void addTag(String tag) {
        synchronized (tagsLock) {
            if (tags.size() < MAX_TAGS) {
                tags.add(tag);
            }
        }
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
    public void recordSuccessfulSync(OperationType resource, long time) {
        lastSynchronizationData.put(resource, new AtomicLong(time));
    }

    @Override
    public void recordSyncError(OperationType OperationType, int status) {
        synchronized (httpErrorsLock) {
            Map<Long, Long> statusMap = httpErrors.get(OperationType);
            if (statusMap == null) {
                return;
            }

            if (!statusMap.containsKey((long) status)) {
                statusMap.put((long) status, 0L);
            }

            statusMap.put((long) status, statusMap.get((long) status) + 1L);
        }
    }

    @Override
    public void recordSyncLatency(OperationType resource, long latency) {
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
        synchronized (streamingEventsLock) {
            if (streamingEvents.size() < MAX_STREAMING_EVENTS) {
                streamingEvents.add(streamingEvent);
            }
        }
    }

    @Override
    public void recordSessionLength(long sessionLength) {
        this.sessionLength.set(sessionLength);
    }

    private void initializeProperties() {
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
        lastSynchronizationData.put(OperationType.IMPRESSIONS, new AtomicLong());
        lastSynchronizationData.put(OperationType.IMPRESSIONS_COUNT, new AtomicLong());
        lastSynchronizationData.put(OperationType.TELEMETRY, new AtomicLong());
        lastSynchronizationData.put(OperationType.EVENTS, new AtomicLong());
        lastSynchronizationData.put(OperationType.MY_SEGMENT, new AtomicLong());
        lastSynchronizationData.put(OperationType.SPLITS, new AtomicLong());
        lastSynchronizationData.put(OperationType.TOKEN, new AtomicLong());
    }

    private void initializeHttpErrors() {
        httpErrors.put(OperationType.EVENTS, Maps.newConcurrentMap());
        httpErrors.put(OperationType.SPLITS, Maps.newConcurrentMap());
        httpErrors.put(OperationType.TELEMETRY, Maps.newConcurrentMap());
        httpErrors.put(OperationType.MY_SEGMENT, Maps.newConcurrentMap());
        httpErrors.put(OperationType.IMPRESSIONS_COUNT, Maps.newConcurrentMap());
        httpErrors.put(OperationType.IMPRESSIONS, Maps.newConcurrentMap());
        httpErrors.put(OperationType.TOKEN, Maps.newConcurrentMap());
    }

    private void initializeHttpLatencies() {
        httpLatencies.put(OperationType.EVENTS, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        httpLatencies.put(OperationType.IMPRESSIONS, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        httpLatencies.put(OperationType.TELEMETRY, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        httpLatencies.put(OperationType.IMPRESSIONS_COUNT, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        httpLatencies.put(OperationType.MY_SEGMENT, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        httpLatencies.put(OperationType.SPLITS, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
        httpLatencies.put(OperationType.TOKEN, new AtomicLongArray(MAX_LATENCY_BUCKET_COUNT));
    }

    private void initializePushCounters() {
        pushCounters.put(PushCounterEvent.AUTH_REJECTIONS, new AtomicLong());
        pushCounters.put(PushCounterEvent.TOKEN_REFRESHES, new AtomicLong());
    }
}
