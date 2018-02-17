package io.split.android.client.impressions;

import com.google.common.base.Preconditions;

import java.util.List;

import io.split.android.client.dtos.TestImpressions;

public class StoredImpressions {
    private final String id;
    private final List<TestImpressions> impressions;

    public static StoredImpressions from(String id, List<TestImpressions> impressions) {
        return new StoredImpressions(id, impressions);
    }

    private StoredImpressions(String id, List<TestImpressions> impressions) {
        this.id = Preconditions.checkNotNull(id);
        this.impressions = Preconditions.checkNotNull(impressions);
    }

    public String id() {
        return id;
    }

    public List<TestImpressions> impressions() {
        return impressions;
    }
}
