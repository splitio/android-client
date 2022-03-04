package io.split.android.client.validators;

import io.split.android.client.api.Key;
import io.split.android.client.attributes.AttributesManager;
import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.events.ListenableEventsManager;
import io.split.android.client.storage.mysegments.MySegmentsStorage;

public interface TreatmentManagerFactory {

    TreatmentManager getTreatmentManager(Key key, ListenableEventsManager eventsManager, AttributesManager attributesManager);
}
