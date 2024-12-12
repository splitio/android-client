package io.split.android.client.localhost;

import io.split.android.client.impressions.DecoratedImpression;
import io.split.android.client.impressions.DecoratedImpressionListener;
import io.split.android.client.impressions.Impression;
import io.split.android.client.impressions.ImpressionListener;

public class LocalhostImpressionsListener implements ImpressionListener, DecoratedImpressionListener {
    @Override
    public void log(Impression impression) {
    }

    @Override
    public void log(DecoratedImpression impression) {
    }

    @Override
    public void close() {
    }
}
