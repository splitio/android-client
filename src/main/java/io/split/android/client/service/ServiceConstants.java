package io.split.android.client.service;

import java.util.concurrent.TimeUnit;

public class ServiceConstants {
    public static final long RECORDED_DATA_EXPIRATION_PERIOD = TimeUnit.DAYS.toSeconds(90); // Impressions and events
    public static final long ESTIMATED_IMPRESSION_SIZE_IN_BYTES = 150L;
    public static final long MAX_EVENTS_SIZE_BYTES = 5 * 1024 * 1024L;
    public static final long NO_INITIAL_DELAY = 0;
    public static final long DEFAULT_INITIAL_DELAY = 15L;
    public static final long MIN_INITIAL_DELAY = 5L;
    public static final int DEFAULT_RECORDS_PER_PUSH = 100;
    public static final int DEFAULT_ROLLOUT_CACHE_EXPIRATION = 10; // 10 days
    public static final long DEFAULT_SPLITS_CACHE_EXPIRATION_IN_SECONDS = TimeUnit.DAYS.toSeconds(DEFAULT_ROLLOUT_CACHE_EXPIRATION); // 10 days

    public static final int MAX_ROWS_PER_QUERY = 100;

    public static final int DEFAULT_IMPRESSION_COUNT_ROWS_POP = 200;

    public final static String TASK_INFO_FIELD_STATUS = "taskStatus";
    public final static String TASK_INFO_FIELD_TYPE = "taskType";
    public final static String TASK_INFO_FIELD_RECORDS_NON_SENT = "recordNonSent";
    public final static String TASK_INFO_FIELD_BYTES_NON_SET = "bytesNonSent";

    public final static String WORKER_PARAM_DATABASE_NAME = "databaseName";
    public final static String WORKER_PARAM_KEY = "key";
    public final static String WORKER_PARAM_API_KEY = "apiKey";
    public final static String WORKER_PARAM_ENDPOINT = "endpoint";
    public final static String WORKER_PARAM_IMPRESSIONS_PER_PUSH = "impressionsPerPush";
    public final static String WORKER_PARAM_EVENTS_PER_PUSH = "eventsPerPush";
    public static final String WORKER_PARAM_UNIQUE_KEYS_PER_PUSH = "unique_keys_per_push";
    public static final String WORKER_PARAM_UNIQUE_KEYS_ESTIMATED_SIZE_IN_BYTES = "unique_keys_estimated_size_in_bytes";
    public static final String WORKER_PARAM_ENCRYPTION_ENABLED = "encryptionEnabled";
    public static final String WORKER_PARAM_CONFIGURED_FILTER_VALUES = "configuredFilterValues";
    public static final String WORKER_PARAM_CONFIGURED_FILTER_TYPE = "configuredFilterType";
    public static final String WORKER_PARAM_FLAGS_SPEC = "flagsSpec";
    public static final String WORKER_PARAM_CERTIFICATE_PINS = "certificatePins";

    public static final int LAST_SEEN_IMPRESSION_CACHE_SIZE = 2000;
    public static final int MY_SEGMENT_V2_DATA_SIZE = 1024 * 10;// bytes
    public static final long DEFAULT_SSE_CONNECTION_DELAY_SECS = 60;

    public static final String DEFAULT_SPLITS_FILENAME = "splits";
    public static final String LOCALHOST = "localhost";
    public static final String LOCALHOST_FOLDER = "localhost";
    public static final String PROPERTIES_EXTENSION = "properties";
    public static final String YML_EXTENSION = "yml";
    public static final String YAML_EXTENSION = "yaml";

    public static final int TELEMETRY_CONFIG_RETRY_INTERVAL_SECONDS = 1;
    public static final int TELEMETRY_CONFIG_MAX_RETRY_ATTEMPTS = 3;
    public static final int TELEMETRY_STATS_INITIAL_DELAY = 5;
    public static final String SHOULD_RECORD_TELEMETRY = "shouldRecordTelemetry";

    public static final int ON_DEMAND_FETCH_BACKOFF_MAX_WAIT = 60;
    public static final long TEN_DAYS_EXPIRATION_PERIOD = TimeUnit.DAYS.toSeconds(10);
    public static final int MAX_UNIQUE_KEYS_IN_MEMORY = 30000;
    public static final int UNIQUE_KEYS_MAX_RETRY_ATTEMPTS = 3;
    public static final long DEFAULT_OBSERVER_CACHE_EXPIRATION_PERIOD_MS = TimeUnit.HOURS.toMillis(4);
    public static final String FLAGS_SPEC_PARAM = "s";
    public static final long DEFAULT_IMPRESSIONS_DEDUPE_TIME_INTERVAL = 3600L * 1000L; // 1 hour
    public static final int ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES = 10;
}
