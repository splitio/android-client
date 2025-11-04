package io.split.android.client;

import static org.mockito.Mockito.verify;

import androidx.annotation.NonNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SplitClientImplEvaluationOptionsTest extends SplitClientImplBaseTest {

    @Test
    public void getTreatmentDelegatesToTreatmentManager() {
        Map<String, Object> attrs = getAttrs();
        EvaluationOptions evaluationOptions = getEvaluationOptions();
        splitClient.getTreatment("test", attrs, evaluationOptions);

        verify(treatmentManager).getTreatment("test", attrs, evaluationOptions, false);
    }

    @Test
    public void getTreatmentsDelegatesToTreatmentManager() {
        Map<String, Object> attrs = getAttrs();
        EvaluationOptions evaluationOptions = getEvaluationOptions();
        List<String> flags = Arrays.asList("test", "test2");
        splitClient.getTreatments(flags, attrs, evaluationOptions);

        verify(treatmentManager).getTreatments(flags, attrs, evaluationOptions, false);
    }

    @Test
    public void getTreatmentWithConfigDelegatesToTreatmentManager() {
        Map<String, Object> attrs = getAttrs();
        EvaluationOptions evaluationOptions = getEvaluationOptions();
        splitClient.getTreatmentWithConfig("test", attrs, evaluationOptions);

        verify(treatmentManager).getTreatmentWithConfig("test", attrs, evaluationOptions, false);
    }

    @Test
    public void getTreatmentsWithConfigDelegatesToTreatmentManager() {
        Map<String, Object> attrs = getAttrs();
        EvaluationOptions evaluationOptions = getEvaluationOptions();
        List<String> flags = Arrays.asList("test", "test2");
        splitClient.getTreatmentsWithConfig(flags, attrs, evaluationOptions);

        verify(treatmentManager).getTreatmentsWithConfig(flags, attrs, evaluationOptions, false);
    }

    @Test
    public void getTreatmentsByFlagSetDelegatesToTreatmentManager() {
        Map<String, Object> attrs = getAttrs();
        EvaluationOptions evaluationOptions = getEvaluationOptions();
        splitClient.getTreatmentsByFlagSet("test", attrs, evaluationOptions);

        verify(treatmentManager).getTreatmentsByFlagSet("test", attrs, evaluationOptions, false);
    }

    @Test
    public void getTreatmentsByFlagSetsDelegatesToTreatmentManager() {
        Map<String, Object> attrs = getAttrs();
        EvaluationOptions evaluationOptions = getEvaluationOptions();
        List<String> flagSets = Arrays.asList("test", "test2");
        splitClient.getTreatmentsByFlagSets(flagSets, attrs, evaluationOptions);

        verify(treatmentManager).getTreatmentsByFlagSets(flagSets, attrs, evaluationOptions, false);
    }

    @Test
    public void getTreatmentsWithConfigByFlagSetDelegatesToTreatmentManager() {
        Map<String, Object> attrs = getAttrs();
        EvaluationOptions evaluationOptions = getEvaluationOptions();
        splitClient.getTreatmentsWithConfigByFlagSet("test", attrs, evaluationOptions);

        verify(treatmentManager).getTreatmentsWithConfigByFlagSet("test", attrs, evaluationOptions, false);
    }

    @Test
    public void getTreatmentsWithConfigByFlagSetsDelegatesToTreatmentManager() {
        Map<String, Object> attrs = getAttrs();
        EvaluationOptions evaluationOptions = getEvaluationOptions();
        List<String> flagSets = Arrays.asList("test", "test2");
        splitClient.getTreatmentsWithConfigByFlagSets(flagSets, attrs, evaluationOptions);

        verify(treatmentManager).getTreatmentsWithConfigByFlagSets(flagSets, attrs, evaluationOptions, false);
    }

    @NonNull
    private static EvaluationOptions getEvaluationOptions() {
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("key", "value");
        properties.put("key2", 2);
        return new EvaluationOptions(properties);
    }

    private static Map<String, Object> getAttrs() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("key", "value");
        return attrs;
    }
}
