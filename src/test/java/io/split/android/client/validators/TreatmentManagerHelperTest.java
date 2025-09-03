package io.split.android.client.validators;

import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import io.split.android.client.SplitResult;
import io.split.android.client.fallback.FallbackTreatmentsConfiguration;
import io.split.android.client.fallback.FallbackTreatment;
import io.split.android.client.fallback.FallbackTreatmentsCalculator;
import io.split.android.client.fallback.FallbackTreatmentsCalculatorImpl;

public class TreatmentManagerHelperTest {

    @Test
    public void controlTreatmentsForSplitsValidatesSplitsWhenValidatorAndLoggerAreNotNull() {
        SplitValidator validator = mock(SplitValidator.class);
        ValidationMessageLogger logger = mock(ValidationMessageLogger.class);

        when(validator.validateName("split2")).thenReturn(new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "message"));

        FallbackTreatmentsCalculator calc = new FallbackTreatmentsCalculatorImpl(FallbackTreatmentsConfiguration.builder()
                .global(new FallbackTreatment("control"))
                .build());

        TreatmentManagerHelper.controlTreatmentsForSplitsWithConfig(validator, logger, Arrays.asList("split1", "split2"), "tag", SplitResult::treatment, calc);

        verify(validator).validateName("split1");
        verify(validator).validateName("split2");
        verify(logger, atMostOnce()).e("message", "tag");
    }

    @Test
    public void controlTreatmentsForSplitsWithConfigValidatesSplitsWhenValidatorAndLoggerAreNotNull() {
        SplitValidator validator = mock(SplitValidator.class);
        ValidationMessageLogger logger = mock(ValidationMessageLogger.class);

        when(validator.validateName("split2")).thenReturn(new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "message"));

        FallbackTreatmentsCalculator calc = new FallbackTreatmentsCalculatorImpl(FallbackTreatmentsConfiguration.builder()
                .global(new FallbackTreatment("control"))
                .build());

        Map<String, SplitResult> result = TreatmentManagerHelper.controlTreatmentsForSplitsWithConfig(validator, logger, Arrays.asList("split1", "split2"), "tag", TreatmentManagerImpl.ResultTransformer::identity, calc);

        verify(validator).validateName("split1");
        verify(validator).validateName("split2");
        verify(logger, atMostOnce()).e("message", "tag");
    }

    @Test
    public void controlTreatmentsForSplitsWithConfigOnlyAddsValueForValidSplits() {
        SplitValidator validator = mock(SplitValidator.class);
        ValidationMessageLogger logger = mock(ValidationMessageLogger.class);

        when(validator.validateName("split2")).thenReturn(new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "message"));

        FallbackTreatmentsCalculator calc = new FallbackTreatmentsCalculatorImpl(FallbackTreatmentsConfiguration.builder()
                .global(new FallbackTreatment("control"))
                .build());

        Map<String, SplitResult> result = TreatmentManagerHelper.controlTreatmentsForSplitsWithConfig(validator, logger, Arrays.asList("split1", "split2"), "tag", TreatmentManagerImpl.ResultTransformer::identity, calc);

        verify(validator).validateName("split1");
        verify(validator).validateName("split2");
        verify(logger, atMostOnce()).e("message", "tag");
        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.containsKey("split1"));
        Assert.assertFalse(result.containsKey("split2"));
    }

    @Test
    public void controlTreatmentsForSplitsOnlyAddsValuesForValidSplits() {
        SplitValidator validator = mock(SplitValidator.class);
        ValidationMessageLogger logger = mock(ValidationMessageLogger.class);

        when(validator.validateName("split2")).thenReturn(new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "message"));

        FallbackTreatmentsCalculator calc = new FallbackTreatmentsCalculatorImpl(FallbackTreatmentsConfiguration.builder()
                .global(new FallbackTreatment("control"))
                .build());

        Map<String, String> result = TreatmentManagerHelper.controlTreatmentsForSplitsWithConfig(validator, logger, Arrays.asList("split1", "split2"), "tag", SplitResult::treatment, calc);

        verify(validator).validateName("split1");
        verify(validator).validateName("split2");
        verify(logger, atMostOnce()).e("message", "tag");
        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.containsKey("split1"));
        Assert.assertFalse(result.containsKey("split2"));
    }
}
