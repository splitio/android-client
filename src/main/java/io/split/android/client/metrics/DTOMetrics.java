package io.split.android.client.metrics;

import io.split.android.client.dtos.Counter;
import io.split.android.client.dtos.Latency;

/**
 * Created by adilaijaz on 6/14/16.
 */
public interface DTOMetrics {
    void time(Latency dto);

    void count(Counter dto);
}
