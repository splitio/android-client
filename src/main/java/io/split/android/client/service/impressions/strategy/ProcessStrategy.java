package io.split.android.client.service.impressions.strategy;

import java.util.List;

import io.split.android.client.impressions.Impression;

public interface ProcessStrategy {

    void apply(List<Impression> impressions);
}
