package io.split.android.client;

import static org.mockito.Mockito.verify;

import org.junit.Test;

import java.util.Collections;
import java.util.Map;

public class SplitClientImplFlagSetsTest extends SplitClientImplBaseTest {

    @Test
    public void getTreatmentsByFlagSetDelegatesToTreatmentManager() {
        Map<String, Object> attributes = Collections.singletonMap("key", "value");
        splitClient.getTreatmentsByFlagSet("set", attributes);

        verify(treatmentManager).getTreatmentsByFlagSet("set", attributes, null, false);
    }

    @Test
    public void getTreatmentsByFlagSetsDelegatesToTreatmentManager() {
        Map<String, Object> attributes = Collections.singletonMap("key", "value");
        splitClient.getTreatmentsByFlagSets(Collections.singletonList("set"), attributes);

        verify(treatmentManager).getTreatmentsByFlagSets(Collections.singletonList("set"), attributes, null, false);
    }

    @Test
    public void getTreatmentsWithConfigByFlagSetDelegatesToTreatmentManager() {
        Map<String, Object> attributes = Collections.singletonMap("key", "value");
        splitClient.getTreatmentsWithConfigByFlagSet("set", attributes);

        verify(treatmentManager).getTreatmentsWithConfigByFlagSet("set", attributes, null, false);
    }

    @Test
    public void getTreatmentsWithConfigByFlagSetsDelegatesToTreatmentManager() {
        Map<String, Object> attributes = Collections.singletonMap("key", "value");
        splitClient.getTreatmentsWithConfigByFlagSets(Collections.singletonList("set"), attributes);

        verify(treatmentManager).getTreatmentsWithConfigByFlagSets(Collections.singletonList("set"), attributes, null, false);
    }
}
