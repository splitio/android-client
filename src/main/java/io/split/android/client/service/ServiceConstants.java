package io.split.android.client.service;

public class ServiceConstants {
    //TODO: CHECK THIS VALUE TINCHO!!!!
    public static final long ESTIMATED_IMPRESSION_SIZE_IN_BYTES = 50L;
    public static final long MAX_EVENTS_SIZE_BYTES = 5 * 1024 * 1024L;
    public static final long DEFAULT_WORK_EXECUTION_PERIOD = 15;
    public static final long NO_INITIAL_DELAY = 0;

    public final static String TASK_INFO_FIELD_STATUS = "taskStatus";
    public final static String TASK_INFO_FIELD_TYPE = "taskType";
    public final static String TASK_INFO_FIELD_RECORDS_NON_SENT = "recordNonSent";
    public final static String TASK_INFO_FIELD_BYTES_NON_SET = "bytesNonSent";
}
