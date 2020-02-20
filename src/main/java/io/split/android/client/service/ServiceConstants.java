package io.split.android.client.service;

public class ServiceConstants {
    //TODO: CHECK THIS VALUE TINCHO!!!!
    public static final long EXPIRATION_PERIOD = 3600 * 24 * 90;
    public static final long ESTIMATED_IMPRESSION_SIZE_IN_BYTES = 50L;
    public static final long MAX_EVENTS_SIZE_BYTES = 5 * 1024 * 1024L;
    public static final long NO_INITIAL_DELAY = 0;
    public static final int DEFAULT_RECORDS_PER_PUSH = 100;

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
}
