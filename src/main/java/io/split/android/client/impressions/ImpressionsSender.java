package io.split.android.client.impressions;

import io.split.android.client.dtos.TestImpressions;

import java.util.List;

/**
 * Created by patricioe on 6/20/16.
 */
public interface ImpressionsSender {

    void post(List<TestImpressions> impressions);
}
