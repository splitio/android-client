package io.split.android.fake;

import io.split.android.client.impressions.Impression;
import io.split.android.client.impressions.ImpressionListener;

public class ImpressionListenerMock implements ImpressionListener {
    @Override
    public void log(Impression impression) {
    }

    @Override
    public void close() {
    }
}
