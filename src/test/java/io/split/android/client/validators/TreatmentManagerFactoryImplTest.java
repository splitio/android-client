package io.split.android.client.validators;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import androidx.annotation.NonNull;

import org.junit.Test;

import io.split.android.client.FlagSetsFilter;
import io.split.android.client.attributes.AttributesMerger;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryStorage;
import io.split.android.engine.experiments.SplitParser;

public class TreatmentManagerFactoryImplTest {
    @Test
    public void instantiateWithNullFallbackTreatmentsConfigDoesNotThrow() {
        TreatmentManagerFactoryImpl treatmentManagerFactory = instantiate(null);

        assertNotNull(treatmentManagerFactory);
    }

    @Test
    public void instantiateWithNullByFactoryFallbackTreatmentsConfigDoesNotThrow() {
        TreatmentManagerFactoryImpl treatmentManagerFactory = instantiate(FallbackTreatmentsConfiguration.builder().build());

        assertNotNull(treatmentManagerFactory);
    }

    @NonNull
    private static TreatmentManagerFactoryImpl instantiate(FallbackTreatmentsConfiguration fallbackTreatments) {
        return new TreatmentManagerFactoryImpl(mock(KeyValidator.class),
                mock(SplitValidator.class),
                mock(ImpressionListener.FederatedImpressionListener.class),
                true,
                mock(AttributesMerger.class),
                mock(TelemetryStorage.class),
                mock(SplitParser.class),
                mock(FlagSetsFilter.class),
                mock(SplitsStorage.class),
                fallbackTreatments);
    }
}
