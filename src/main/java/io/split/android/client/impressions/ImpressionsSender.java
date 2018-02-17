package io.split.android.client.impressions;

import io.split.android.client.dtos.TestImpressions;

import java.util.List;

public interface ImpressionsSender {

    boolean post(List<TestImpressions> impressions);
}
