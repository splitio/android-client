package io.split.android.client.impressions;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.TestImpressions;

@Deprecated
public class StoredImpressions {
    private final String id;
    private final List<TestImpressions> impressions;
    private int attempts = 0;
    private long timestamp;

    public static StoredImpressions from(String id, List<TestImpressions> impressions, long timestamp) {
        return new StoredImpressions(id, impressions, timestamp);
    }

    public static StoredImpressions from(String id, int attempts, long timestamp) {
        return new StoredImpressions(id, attempts, timestamp);
    }

    private StoredImpressions(String id, List<TestImpressions> impressions, long timestamp) {
        this.id = Preconditions.checkNotNull(id);
        this.impressions = Preconditions.checkNotNull(impressions);
        this.timestamp = timestamp;
    }

    private StoredImpressions(String id, int attempts, long timestamp) {
        this.id = Preconditions.checkNotNull(id);
        this.attempts = attempts;
        this.timestamp = timestamp;
        this.impressions = new ArrayList<>();
    }

    public String id() {
        return id;
    }

    public List<TestImpressions> impressions() {
        return impressions;
    }
    public int  getAttempts(){
        return attempts;
    }

    public void addAttempt(){
        attempts++;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void addImpressions(List<TestImpressions> impressions) {
        this.impressions.addAll(impressions);
    }

}
