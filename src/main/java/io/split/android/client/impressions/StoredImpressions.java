package io.split.android.client.impressions;

import com.google.common.base.Preconditions;

import java.util.List;

import io.split.android.client.dtos.TestImpressions;

public class StoredImpressions {
    private final String id;
    private final List<TestImpressions> impressions;
    private int attempts = 0;
    private long timestamp = 0;

    public static StoredImpressions from(String id, List<TestImpressions> impressions, long timestamp) {
        return new StoredImpressions(id, impressions, timestamp);
    }

    private StoredImpressions(String id, List<TestImpressions> impressions, long timestamp) {
        this.id = Preconditions.checkNotNull(id);
        this.impressions = Preconditions.checkNotNull(impressions);
        this.timestamp = timestamp;
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

    public boolean isDeprecated() {
        long diff = System.currentTimeMillis() - timestamp;

        long oneDayMillis = 3600 * 1000;
        if (diff > oneDayMillis) {
            return true;
        }
        return false;
    }
}
