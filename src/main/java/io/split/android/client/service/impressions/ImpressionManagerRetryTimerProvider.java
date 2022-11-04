package io.split.android.client.service.impressions;

import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;

interface ImpressionManagerRetryTimerProvider {

    RetryBackoffCounterTimer getUniqueKeysTimer();

    RetryBackoffCounterTimer getImpressionsTimer();

    RetryBackoffCounterTimer getImpressionsCountTimer();
}
