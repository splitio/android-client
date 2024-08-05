package io.split.android.client.telemetry.storage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
import io.split.android.client.telemetry.model.UpdatesFromSSE;
import io.split.android.client.telemetry.model.streaming.StreamingEvent;
import io.split.android.client.telemetry.model.streaming.UpdatesFromSSEEnum;

public class InMemoryTelemetryStorage implements TelemetryStorage {

    private static final int MAX_STREAMING_EVENTS = 20;
    private static final int MAX_TAGS = 10;

    private final Map<Method, AtomicLong> methodExceptionsCounter = new ConcurrentHashMap<>();
    private final ConcurrentMap<Method, ILatencyTracker> methodLatencies = new ConcurrentHashMap<>();

    private final Map<FactoryCounter, AtomicLong> factoryCounters = new ConcurrentHashMap<>();

    private final Map<ImpressionsDataType, AtomicLong> impressionsData = new ConcurrentHashMap<>();
    private final Map<EventsDataRecordsEnum, AtomicLong> eventsData = new ConcurrentHashMap<>();

    private final Map<OperationType, AtomicLong> lastSynchronizationData = new ConcurrentHashMap<>();

    private final AtomicLong sessionLength = new AtomicLong();

    private final Object httpErrorsLock = new Object();
    private final Map<OperationType, Map<Long, Long>> httpErrors = new ConcurrentHashMap<>();

    private final Map<OperationType, ILatencyTracker> httpLatencies = new ConcurrentHashMap<>();

    private final Map<PushCounterEvent, AtomicLong> pushCounters = new ConcurrentHashMap<>();

    private final Object streamingEventsLock = new Object();
    private List<StreamingEvent> streamingEvents = new ArrayList<>();
    private Map<UpdatesFromSSEEnum, AtomicLong> updatesFromSSE = new ConcurrentHashMap<>();

    private final Object tagsLock = new Object();
    private final Object httpLatenciesLock = new Object();
    private final Object methodLatenciesLock = new Object();
    private final Object updatesFromSSELock = new Object();

    private final Set<String> tags = new HashSet<>();

    public InMemoryTelemetryStorage() {
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
        methodExceptions.setTreatmentsByFlagSet(methodExceptionsCounter.get(Method.TREATMENTS_BY_FLAG_SET).getAndSet(0L));
        methodExceptions.setTreatmentsByFlagSets(methodExceptionsCounter.get(Method.TREATMENTS_BY_FLAG_SETS).getAndSet(0L));
        methodExceptions.setTreatmentsWithConfigByFlagSet(methodExceptionsCounter.get(Method.TREATMENTS_WITH_CONFIG_BY_FLAG_SET).getAndSet(0L));
        methodExceptions.setTreatmentsWithConfigByFlagSets(methodExceptionsCounter.get(Method.TREATMENTS_WITH_CONFIG_BY_FLAG_SETS).getAndSet(0L));

        return methodExceptions;
    }

    @Override
    public MethodLatencies popLatencies() {
        synchronized (methodLatenciesLock) {
            MethodLatencies latencies = new MethodLatencies();

            latencies.setTreatment(popLatencies(Method.TREATMENT));
            latencies.setTreatments(popLatencies(Method.TREATMENTS));
            latencies.setTreatmentWithConfig(popLatencies(Method.TREATMENT_WITH_CONFIG));
            latencies.setTreatmentsWithConfig(popLatencies(Method.TREATMENTS_WITH_CONFIG));
            latencies.setTreatmentsByFlagSet(popLatencies(Method.TREATMENTS_BY_FLAG_SET));
            latencies.setTreatmentsByFlagSets(popLatencies(Method.TREATMENTS_BY_FLAG_SETS));
            latencies.setTreatmentsWithConfigByFlagSet(popLatencies(Method.TREATMENTS_WITH_CONFIG_BY_FLAG_SET));
            latencies.setTreatmentsWithConfigByFlagSets(popLatencies(Method.TREATMENTS_WITH_CONFIG_BY_FLAG_SETS));
            latencies.setTrack(popLatencies(Method.TRACK));

            return latencies;
        }
    }

    @Override
    public void recordLatency(Method method, long latency) {
        ILatencyTracker latencyTracker = methodLatencies.get(method);
        if (latencyTracker != null) {
            synchronized (methodLatencies) {
                latencyTracker.addLatencyMillis(latency);
            }
        }
    }

    @Override
    public void recordException(Method method) {
        methodExceptionsCounter.get(method).incrementAndGet();
    }

    @Override
    public long getNonReadyUsage() {
        return factoryCounters.get(FactoryCounter.NON_READY_USAGES).get();
    }

    @Override
    public long getActiveFactories() {
        return factoryCounters.get(FactoryCounter.ACTIVE_FACTORIES).get();
    }

    @Override
    public long getRedundantFactories() {
        return factoryCounters.get(FactoryCounter.REDUNDANT_FACTORIES).get();
    }

    @Override
    public long getTimeUntilReady() {
        return factoryCounters.get(FactoryCounter.SDK_READY_TIME).get();
    }

    @Override
    public long getTimeUntilReadyFromCache() {
        return factoryCounters.get(FactoryCounter.SDK_READY_FROM_CACHE).get();
    }

    @Override
    public void recordNonReadyUsage() {
        factoryCounters.get(FactoryCounter.NON_READY_USAGES).incrementAndGet();
    }

    @Override
    public void recordActiveFactories(int count) {
        factoryCounters.get(FactoryCounter.ACTIVE_FACTORIES).set(count);
    }

    @Override
    public void recordRedundantFactories(int count) {
        factoryCounters.get(FactoryCounter.REDUNDANT_FACTORIES).set(count);
    }

    @Override
    public void recordTimeUntilReady(long time) {
        factoryCounters.get(FactoryCounter.SDK_READY_TIME).set(time);
    }

    @Override
    public void recordTimeUntilReadyFromCache(long time) {
        factoryCounters.get(FactoryCounter.SDK_READY_FROM_CACHE).set(time);
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
        lastSync.setLastMyLargeSegmentSync(lastSynchronizationData.get(OperationType.MY_LARGE_SEGMENT).get());
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
        errors.setMyLargeSegmentsSyncErrs(httpErrors.get(OperationType.MY_LARGE_SEGMENT));
        errors.setTokenGetErrs(httpErrors.get(OperationType.TOKEN));

        initializeHttpErrors();

        return errors;
    }

    @Override
    public HttpLatencies popHttpLatencies() {
        synchronized (httpLatenciesLock) {
            HttpLatencies latencies = new HttpLatencies();

            latencies.setTelemetry(popLatencies(OperationType.TELEMETRY));
            latencies.setEvents(popLatencies(OperationType.EVENTS));
            latencies.setSplits(popLatencies(OperationType.SPLITS));
            latencies.setMySegments(popLatencies(OperationType.MY_SEGMENT));
            latencies.setMyLargeSegments(popLatencies(OperationType.MY_LARGE_SEGMENT));
            latencies.setToken(popLatencies(OperationType.TOKEN));
            latencies.setImpressions(popLatencies(OperationType.IMPRESSIONS));
            latencies.setImpressionsCount(popLatencies(OperationType.IMPRESSIONS_COUNT));

            return latencies;
        }
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
    public UpdatesFromSSE popUpdatesFromSSE() {
        synchronized (updatesFromSSELock) {
            long sCount = 0L;
            long mCount = 0L;
            long lCount = 0L;

            AtomicLong splits = updatesFromSSE.get(UpdatesFromSSEEnum.SPLITS);
            if (splits != null) {
                sCount = splits.getAndSet(0L);
            }

            AtomicLong mySegments = updatesFromSSE.get(UpdatesFromSSEEnum.MY_SEGMENTS);
            if (mySegments != null) {
                mCount = mySegments.getAndSet(0L);
            }

            AtomicLong myLargeSegments = updatesFromSSE.get(UpdatesFromSSEEnum.MY_LARGE_SEGMENTS);
            if (myLargeSegments != null) {
                lCount = myLargeSegments.getAndSet(0L);
            }

            return new UpdatesFromSSE(sCount, mCount, lCount);
        }
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
    public void recordSyncError(OperationType OperationType, Integer status) {
        synchronized (httpErrorsLock) {
            if (status == null) {
                return;
            }

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
        ILatencyTracker latencyTracker = httpLatencies.get(resource);
        if (latencyTracker != null) {
            latencyTracker.addLatencyMillis(latency);
        }
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

    @Override
    public void recordUpdatesFromSSE(UpdatesFromSSEEnum sseUpdate) {
        updatesFromSSE.get(sseUpdate).incrementAndGet();
    }

    private void initializeProperties() {
        initializeMethodExceptionsCounter();
        initializeMethodLatenciesCounter();
        initializeFactoryCounters();
        initializeImpressionsData();
        initializeEventsData();
        initializeLastSynchronizationData();
        initializeHttpErrors();
        initializeHttpLatencies();
        initializePushCounters();
        initializeUpdatesFromSSE();
    }

    private void initializeMethodLatenciesCounter() {
        methodLatencies.put(Method.TREATMENT, new BinarySearchLatencyTracker());
        methodLatencies.put(Method.TREATMENTS, new BinarySearchLatencyTracker());
        methodLatencies.put(Method.TREATMENT_WITH_CONFIG, new BinarySearchLatencyTracker());
        methodLatencies.put(Method.TREATMENTS_WITH_CONFIG, new BinarySearchLatencyTracker());
        methodLatencies.put(Method.TRACK, new BinarySearchLatencyTracker());
        methodLatencies.put(Method.TREATMENTS_BY_FLAG_SET, new BinarySearchLatencyTracker());
        methodLatencies.put(Method.TREATMENTS_BY_FLAG_SETS, new BinarySearchLatencyTracker());
        methodLatencies.put(Method.TREATMENTS_WITH_CONFIG_BY_FLAG_SET, new BinarySearchLatencyTracker());
        methodLatencies.put(Method.TREATMENTS_WITH_CONFIG_BY_FLAG_SETS, new BinarySearchLatencyTracker());
    }

    private void initializeMethodExceptionsCounter() {
        methodExceptionsCounter.put(Method.TREATMENT, new AtomicLong());
        methodExceptionsCounter.put(Method.TREATMENTS, new AtomicLong());
        methodExceptionsCounter.put(Method.TREATMENT_WITH_CONFIG, new AtomicLong());
        methodExceptionsCounter.put(Method.TREATMENTS_WITH_CONFIG, new AtomicLong());
        methodExceptionsCounter.put(Method.TRACK, new AtomicLong());
        methodExceptionsCounter.put(Method.TREATMENTS_BY_FLAG_SET, new AtomicLong());
        methodExceptionsCounter.put(Method.TREATMENTS_BY_FLAG_SETS, new AtomicLong());
        methodExceptionsCounter.put(Method.TREATMENTS_WITH_CONFIG_BY_FLAG_SET, new AtomicLong());
        methodExceptionsCounter.put(Method.TREATMENTS_WITH_CONFIG_BY_FLAG_SETS, new AtomicLong());
    }

    private void initializeFactoryCounters() {
        factoryCounters.put(FactoryCounter.NON_READY_USAGES, new AtomicLong());
        factoryCounters.put(FactoryCounter.SDK_READY_TIME, new AtomicLong());
        factoryCounters.put(FactoryCounter.SDK_READY_FROM_CACHE, new AtomicLong());
        factoryCounters.put(FactoryCounter.REDUNDANT_FACTORIES, new AtomicLong());
        factoryCounters.put(FactoryCounter.ACTIVE_FACTORIES, new AtomicLong());
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
        lastSynchronizationData.put(OperationType.MY_LARGE_SEGMENT, new AtomicLong());
        lastSynchronizationData.put(OperationType.SPLITS, new AtomicLong());
        lastSynchronizationData.put(OperationType.TOKEN, new AtomicLong());
    }

    private void initializeHttpErrors() {
        httpErrors.put(OperationType.EVENTS, new ConcurrentHashMap<>());
        httpErrors.put(OperationType.SPLITS, new ConcurrentHashMap<>());
        httpErrors.put(OperationType.TELEMETRY, new ConcurrentHashMap<>());
        httpErrors.put(OperationType.MY_SEGMENT, new ConcurrentHashMap<>());
        httpErrors.put(OperationType.MY_LARGE_SEGMENT, new ConcurrentHashMap<>());
        httpErrors.put(OperationType.IMPRESSIONS_COUNT, new ConcurrentHashMap<>());
        httpErrors.put(OperationType.IMPRESSIONS, new ConcurrentHashMap<>());
        httpErrors.put(OperationType.TOKEN, new ConcurrentHashMap<>());
    }

    private void initializeHttpLatencies() {
        httpLatencies.put(OperationType.EVENTS, new BinarySearchLatencyTracker());
        httpLatencies.put(OperationType.IMPRESSIONS, new BinarySearchLatencyTracker());
        httpLatencies.put(OperationType.TELEMETRY, new BinarySearchLatencyTracker());
        httpLatencies.put(OperationType.IMPRESSIONS_COUNT, new BinarySearchLatencyTracker());
        httpLatencies.put(OperationType.MY_SEGMENT, new BinarySearchLatencyTracker());
        httpLatencies.put(OperationType.MY_LARGE_SEGMENT, new BinarySearchLatencyTracker());
        httpLatencies.put(OperationType.SPLITS, new BinarySearchLatencyTracker());
        httpLatencies.put(OperationType.TOKEN, new BinarySearchLatencyTracker());
    }

    private void initializePushCounters() {
        pushCounters.put(PushCounterEvent.AUTH_REJECTIONS, new AtomicLong());
        pushCounters.put(PushCounterEvent.TOKEN_REFRESHES, new AtomicLong());
    }

    private void initializeUpdatesFromSSE() {
        updatesFromSSE.put(UpdatesFromSSEEnum.SPLITS, new AtomicLong());
        updatesFromSSE.put(UpdatesFromSSEEnum.MY_SEGMENTS, new AtomicLong());
        updatesFromSSE.put(UpdatesFromSSEEnum.MY_LARGE_SEGMENTS, new AtomicLong());
    }

    private List<Long> popLatencies(OperationType operationType) {
        long[] latencies = httpLatencies.get(operationType).getLatencies();
        httpLatencies.get(operationType).clear();
        return getLatenciesList(latencies);
    }

    private List<Long> popLatencies(Method method) {
        long[] latencies = methodLatencies.get(method).getLatencies();
        methodLatencies.get(method).clear();

        return getLatenciesList(latencies);
    }

    private static List<Long> getLatenciesList(long[] latencies) {
        ArrayList<Long> longs = new ArrayList<>();

        for (long lat : latencies) {
            longs.add(lat);
        }

        return longs;
    }
}
