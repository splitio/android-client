package io.split.android.client;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TreatmentLabelsTest {

    @Test
    public void labelValuesAreCorrect() {
        assertEquals("not in split", TreatmentLabels.NOT_IN_SPLIT);
        assertEquals("default rule", TreatmentLabels.DEFAULT_RULE);
        assertEquals("definition not found", TreatmentLabels.DEFINITION_NOT_FOUND);
        assertEquals("exception", TreatmentLabels.EXCEPTION);
        assertEquals("killed", TreatmentLabels.KILLED);
        assertEquals("not ready", TreatmentLabels.NOT_READY);
        assertEquals("targeting rule type unsupported by sdk", TreatmentLabels.UNSUPPORTED_MATCHER_TYPE);
    }
}
