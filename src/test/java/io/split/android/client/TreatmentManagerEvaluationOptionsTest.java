package io.split.android.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.util.HashMap;

import io.split.android.client.attributes.AttributesManager;
import io.split.android.client.attributes.AttributesMerger;
import io.split.android.client.events.ListenableEventsManager;
import io.split.android.client.impressions.Impression;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryStorageProducer;
import io.split.android.client.validators.FlagSetsValidatorImpl;
import io.split.android.client.validators.KeyValidator;
import io.split.android.client.validators.PropertyValidator;
import io.split.android.client.validators.SplitValidator;
import io.split.android.client.validators.TreatmentManagerImpl;
import io.split.android.client.validators.ValidationMessageLogger;

public class TreatmentManagerEvaluationOptionsTest {

    private ImpressionListener.FederatedImpressionListener mImpressionListener;
    private TreatmentManagerImpl mTreatmentManager;
    private PropertyValidator mPropertyValidator;
    private ValidationMessageLogger mValidationMessageLogger;
    private Evaluator mEvaluator;

    @Before
    public void setUp() {
        mEvaluator = mock(Evaluator.class);
        KeyValidator mKeyValidator = mock(KeyValidator.class);
        SplitValidator mSplitValidator = mock(SplitValidator.class);
        mImpressionListener = mock(ImpressionListener.FederatedImpressionListener.class);
        ListenableEventsManager mEventsManager = mock(ListenableEventsManager.class);
        AttributesManager mAttributesManager = mock(AttributesManager.class);
        AttributesMerger mAttributesMerger = mock(AttributesMerger.class);
        TelemetryStorageProducer mTelemetryStorageProducer = mock(TelemetryStorageProducer.class);
        FlagSetsFilter mFlagSetsFilter = mock(FlagSetsFilter.class);
        SplitsStorage mSplitsStorage = mock(SplitsStorage.class);
        mPropertyValidator = mock(PropertyValidator.class);
        mValidationMessageLogger = mock(ValidationMessageLogger.class);
        mTreatmentManager = new TreatmentManagerImpl(
                "matching_key",
                "bucketing_key",
                mEvaluator,
                mKeyValidator,
                mSplitValidator,
                mImpressionListener,
                SplitClientConfig.builder().build().labelsEnabled(),
                mEventsManager,
                mAttributesManager,
                mAttributesMerger,
                mTelemetryStorageProducer,
                mFlagSetsFilter,
                mSplitsStorage,
                mValidationMessageLogger,
                new FlagSetsValidatorImpl(),
                mPropertyValidator);
    }

    @Test
    public void evaluationWithValidPropertiesAddsThemToImpressionAsJsonString() {
        when(mEvaluator.getTreatment(anyString(), anyString(), anyString(), anyMap())).thenReturn(new EvaluationResult("test", "label"));
        EvaluationOptions evaluationOptions = getEvaluationOptions();
        when(mPropertyValidator.validate(any())).thenReturn(PropertyValidator.Result.valid(evaluationOptions.getProperties(), 0));

        mTreatmentManager.getTreatmentWithConfig("test", null, evaluationOptions, false);

        verify(mImpressionListener).log(argThat(new ArgumentMatcher<Impression>() {
            @Override
            public boolean matches(Impression argument) {
                return (argument.properties().equals("{\"key\":\"value\",\"key2\":2}") ||
                        argument.properties().equals("{\"key2\":2,\"key\":\"value\"}")) &&
                        argument.split().equals("test");
            }
        }));
    }

    @Test
    public void evaluationWithEmptyPropertiesAddsNullPropertiesToImpression() {
        when(mEvaluator.getTreatment(anyString(), anyString(), anyString(), anyMap())).thenReturn(new EvaluationResult("test", "label"));
        when(mPropertyValidator.validate(any())).thenReturn(PropertyValidator.Result.valid(null, 0));

        mTreatmentManager.getTreatmentWithConfig("test", null, new EvaluationOptions(new HashMap<>()), false);

        verify(mImpressionListener).log(argThat(new ArgumentMatcher<Impression>() {
            @Override
            public boolean matches(Impression argument) {
                return argument.properties() == null && argument.split().equals("test");
            }
        }));
    }

    @Test
    public void invalidPropertiesAreNotAddedToImpression() {
        when(mEvaluator.getTreatment(anyString(), anyString(), anyString(), anyMap())).thenReturn(new EvaluationResult("test", "label"));
        EvaluationOptions evaluationOptions = getEvaluationOptions();
        when(mPropertyValidator.validate(any())).thenReturn(PropertyValidator.Result.invalid("Invalid properties", 0));

        mTreatmentManager.getTreatmentWithConfig("test", null, evaluationOptions, false);

        verify(mImpressionListener).log(argThat(new ArgumentMatcher<Impression>() {
            @Override
            public boolean matches(Impression argument) {
                return argument.properties() == null && argument.split().equals("test");
            }
        }));
    }

    @Test
    public void invalidPropertiesLogsMessageInValidationMessageLogger() {
        when(mEvaluator.getTreatment(anyString(), anyString(), anyString(), anyMap())).thenReturn(new EvaluationResult("test", "label"));
        EvaluationOptions evaluationOptions = getEvaluationOptions();
        when(mPropertyValidator.validate(any())).thenReturn(PropertyValidator.Result.invalid("Invalid properties", 0));

        mTreatmentManager.getTreatmentWithConfig("test", null, evaluationOptions, false);

        verify(mValidationMessageLogger).e("Properties validation failed: Invalid properties", "getTreatmentWithConfig");
    }

    @Test
    public void propertiesAreValidatedWithPropertyValidator() {
        when(mEvaluator.getTreatment(anyString(), anyString(), anyString(), anyMap())).thenReturn(new EvaluationResult("test", "label"));
        EvaluationOptions evaluationOptions = getEvaluationOptions();

        mTreatmentManager.getTreatmentWithConfig("test", null, evaluationOptions, false);

        verify(mPropertyValidator).validate(evaluationOptions.getProperties());
    }

    @NonNull
    private static EvaluationOptions getEvaluationOptions() {
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("key", "value");
        properties.put("key2", 2);
        return new EvaluationOptions(properties);
    }
}
