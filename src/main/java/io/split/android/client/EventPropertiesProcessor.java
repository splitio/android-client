package io.split.android.client;

import java.util.Map;

public interface EventPropertiesProcessor {
    ProcessedEventProperties process(Map<String, Object> properties);
}
