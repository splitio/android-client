package io.split.android.client.service;

public class ServiceConstants {
    public static final long RECORDED_DATA_EXPIRATION_PERIOD = 3600 * 24 * 90; // Impresions and events
    public static final long ESTIMATED_IMPRESSION_SIZE_IN_BYTES = 150L;
    public static final long MAX_EVENTS_SIZE_BYTES = 5 * 1024 * 1024L;
    public static final long NO_INITIAL_DELAY = 0;
    public static final long DEFAULT_INITIAL_DELAY = 15L;
    public static final int DEFAULT_RECORDS_PER_PUSH = 100;
    public static final long DEFAULT_SPLITS_CACHE_EXPIRATION_IN_SECONDS = 864000; // 10 days

    public static final int MAX_ROWS_PER_QUERY = 100;

    public final static String TASK_INFO_FIELD_STATUS = "taskStatus";
    public final static String TASK_INFO_FIELD_TYPE = "taskType";
    public final static String TASK_INFO_FIELD_RECORDS_NON_SENT = "recordNonSent";
    public final static String TASK_INFO_FIELD_BYTES_NON_SET = "bytesNonSent";

    public final static String WORKER_PARAM_DATABASE_NAME = "databaseName";
    public final static String WORKER_PARAM_KEY = "key";
    public final static String WORKER_PARAM_API_KEY = "apiKey";
    public final static String WORKER_PARAM_ENDPOINT = "endpoint";
    public final static String WORKER_PARAM_EVENTS_ENDPOINT = "eventsEndpoint";
    public final static String WORKER_PARAM_IMPRESSIONS_PER_PUSH = "impressionsPerPush";
    public final static String WORKER_PARAM_EVENTS_PER_PUSH = "eventsPerPush";
    public final static String WORKER_PARAM_SPLIT_CACHE_EXPIRATION = "splitCacheExpiration";

    public final static String CACHE_CONTROL_HEADER = "Cache-Control";
    public final static String CACHE_CONTROL_NO_CACHE = "no-cache";
}
