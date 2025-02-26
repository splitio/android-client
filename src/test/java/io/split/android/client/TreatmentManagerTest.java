package io.split.android.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static io.split.android.client.TreatmentLabels.DEFINITION_NOT_FOUND;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.attributes.AttributesManager;
import io.split.android.client.attributes.AttributesMerger;
import io.split.android.client.dtos.Split;
import io.split.android.client.events.ListenableEventsManager;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.impressions.DecoratedImpression;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorageContainer;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryStorageProducer;
import io.split.android.client.utils.Utils;
import io.split.android.client.validators.FlagSetsValidatorImpl;
import io.split.android.client.validators.KeyValidator;
import io.split.android.client.validators.KeyValidatorImpl;
import io.split.android.client.validators.SplitValidator;
import io.split.android.client.validators.SplitValidatorImpl;
import io.split.android.client.validators.TreatmentManager;
import io.split.android.client.validators.TreatmentManagerImpl;
import io.split.android.client.validators.ValidationMessageLogger;
import io.split.android.client.validators.ValidationMessageLoggerImpl;
import io.split.android.engine.experiments.ParserCommons;
import io.split.android.engine.experiments.SplitParser;
import io.split.android.fake.SplitEventsManagerStub;
import io.split.android.grammar.Treatments;
import io.split.android.helpers.FileHelper;

@SuppressWarnings("ConstantConditions")
public class TreatmentManagerTest {

    Evaluator evaluator;
    ImpressionListener.FederatedImpressionListener impressionListener;
    ListenableEventsManager eventsManagerStub;
    AttributesManager attributesManager = mock(AttributesManager.class);
    TelemetryStorageProducer telemetryStorageProducer = mock(TelemetryStorageProducer.class);
    private FlagSetsFilter mFlagSetsFilter;
    TreatmentManagerImpl treatmentManager;
    private SplitsStorage mSplitsStorage;
    private ValidationMessageLogger mValidationMessageLogger;

    @Before
    public void loadSplitsFromFile() {
        mFlagSetsFilter = new FlagSetsFilterImpl(new HashSet<>());
        mSplitsStorage = mock(SplitsStorage.class);
        mValidationMessageLogger = mock(ValidationMessageLogger.class);
        treatmentManager = initializeTreatmentManager();
        if (evaluator == null) {
            FileHelper fileHelper = new FileHelper();
            MySegmentsStorageContainer mySegmentsStorageContainer = mock(MySegmentsStorageContainer.class);
            MySegmentsStorageContainer myLargeSegmentsStorageContainer = mock(MySegmentsStorageContainer.class);
            MySegmentsStorage mySegmentsStorage = mock(MySegmentsStorage.class);
            RuleBasedSegmentStorage ruleBasedSegmentStorage = mock(RuleBasedSegmentStorage.class);
            SplitsStorage splitsStorage = mock(SplitsStorage.class);

            Set<String> mySegments = new HashSet(Arrays.asList("s1", "s2", "test_copy"));
            List<Split> splits = fileHelper.loadAndParseSplitChangeFile("split_changes_1.json");
            SplitParser splitParser = new SplitParser(new ParserCommons(mySegmentsStorageContainer, myLargeSegmentsStorageContainer, ruleBasedSegmentStorage));

            Map<String, Split> splitsMap = splitsMap(splits);
            when(splitsStorage.getAll()).thenReturn(splitsMap);
            when(splitsStorage.get("FACUNDO_TEST")).thenReturn(splitsMap.get("FACUNDO_TEST"));
            when(splitsStorage.get("testo2222")).thenReturn(splitsMap.get("testo2222"));
            when(splitsStorage.get("Test")).thenReturn(splitsMap.get("Test"));

            when(mySegmentsStorageContainer.getStorageForKey(any())).thenReturn(mySegmentsStorage);
            when(mySegmentsStorage.getAll()).thenReturn(mySegments);

            evaluator = new EvaluatorImpl(splitsStorage, splitParser);
        }
        impressionListener = mock(ImpressionListener.FederatedImpressionListener.class);
        eventsManagerStub = new SplitEventsManagerStub();
    }

    @Test
    public void testBasicEvaluationNoConfig() {
        String matchingKey = "the_key";
        String splitName = "FACUNDO_TEST";

        TreatmentManager treatmentManager = createTreatmentManager(matchingKey, matchingKey);
        SplitResult splitResult = treatmentManager.getTreatmentWithConfig(splitName, null, false);

        Assert.assertNotNull(splitResult);
        Assert.assertEquals("off", splitResult.treatment());
        Assert.assertNull(splitResult.config());
    }

    @Test
    public void testBasicEvaluationWithConfig() {
        String matchingKey = "the_key";
        String splitName = "Test";

        TreatmentManager treatmentManager = createTreatmentManager(matchingKey, matchingKey);
        SplitResult splitResult = treatmentManager.getTreatmentWithConfig(splitName, null, false);

        Assert.assertNotNull(splitResult);
        Assert.assertEquals("off", splitResult.treatment());
        Assert.assertNotNull(splitResult.config());
    }

    @Test
    public void testBasicEvaluations() {
        String matchingKey = "thekey";
        List<String> splitList = Arrays.asList("FACUNDO_TEST", "testo2222", "Test");

        TreatmentManager treatmentManager = createTreatmentManager(matchingKey, matchingKey);
        Map<String, SplitResult> splitResultList = treatmentManager.getTreatmentsWithConfig(splitList, null, false);

        SplitResult r1 = splitResultList.get("FACUNDO_TEST");
        SplitResult r2 = splitResultList.get("testo2222");
        SplitResult r3 = splitResultList.get("Test");

        Assert.assertNotNull(r1);
        Assert.assertEquals("off", r1.treatment());
        Assert.assertNull(r1.config());

        Assert.assertNotNull(r2);
        Assert.assertEquals("pesto", r2.treatment());
        Assert.assertNull(r2.config());

        Assert.assertNotNull(r3);
        Assert.assertEquals("off", r3.treatment());
        Assert.assertNotNull(r3.config());
    }

    @Test
    public void testClientIsDestroyed() {
        String matchingKey = "nico_test";
        String splitName = "FACUNDO_TEST";
        List<String> splitList = Arrays.asList("FACUNDO_TEST", "a_new_split_2", "benchmark_jw_1");
        TreatmentManager treatmentManager = createTreatmentManager(matchingKey, matchingKey);

        String treatment = treatmentManager.getTreatment(splitName, null, true);
        SplitResult splitResult = treatmentManager.getTreatmentWithConfig(splitName, null, true);
        Map<String, String> treatmentList = treatmentManager.getTreatments(splitList, null, true);
        Map<String, SplitResult> splitResultList = treatmentManager.getTreatmentsWithConfig(splitList, null, true);
        assertControl(splitList, treatment, treatmentList, splitResult, splitResultList);
    }

    @Test
    public void testNonExistingSplits() {
        String matchingKey = "nico_test";
        String splitName = "NON_EXISTING_1";
        List<String> splitList = Arrays.asList("NON_EXISTING_1", "NON_EXISTING_2", "NON_EXISTING_3");
        TreatmentManager treatmentManager = createTreatmentManager(matchingKey, matchingKey);

        String treatment = treatmentManager.getTreatment(splitName, null, false);
        SplitResult splitResult = treatmentManager.getTreatmentWithConfig(splitName, null, false);
        Map<String, String> treatmentList = treatmentManager.getTreatments(splitList, null, false);
        Map<String, SplitResult> splitResultList = treatmentManager.getTreatmentsWithConfig(splitList, null, false);
        assertControl(splitList, treatment, treatmentList, splitResult, splitResultList);
    }

    @Test
    public void testEmptySplit() {
        String matchingKey = "nico_test";
        String splitName = "";
        List<String> splitList = new ArrayList<>();
        TreatmentManager treatmentManager = createTreatmentManager(matchingKey, matchingKey);

        String treatment = treatmentManager.getTreatment(splitName, null, false);
        SplitResult splitResult = treatmentManager.getTreatmentWithConfig(splitName, null, false);
        Map<String, String> treatmentList = treatmentManager.getTreatments(splitList, null, false);
        Map<String, SplitResult> splitResultList = treatmentManager.getTreatmentsWithConfig(splitList, null, false);

        assertControl(splitList, treatment, treatmentList, splitResult, splitResultList);
    }

    @Test
    public void testNullKey() {
        String matchingKey = null;
        String splitName = "FACUNDO_TEST";
        List<String> splitList = Arrays.asList("FACUNDO_TEST", "a_new_split_2", "benchmark_jw_1");
        TreatmentManager treatmentManager = createTreatmentManager(matchingKey, matchingKey);

        String treatment = treatmentManager.getTreatment(splitName, null, false);
        SplitResult splitResult = treatmentManager.getTreatmentWithConfig(splitName, null, false);
        Map<String, String> treatmentList = treatmentManager.getTreatments(splitList, null, false);
        Map<String, SplitResult> splitResultList = treatmentManager.getTreatmentsWithConfig(splitList, null, false);

        assertControl(splitList, treatment, treatmentList, splitResult, splitResultList);
    }

    @Test
    public void testEmptyKey() {
        String matchingKey = "";
        String splitName = "FACUNDO_TEST";
        List<String> splitList = new ArrayList<>();
        TreatmentManager treatmentManager = createTreatmentManager(matchingKey, matchingKey);

        String treatment = treatmentManager.getTreatment(splitName, null, false);
        SplitResult splitResult = treatmentManager.getTreatmentWithConfig(splitName, null, false);
        Map<String, String> treatmentList = treatmentManager.getTreatments(splitList, null, false);
        Map<String, SplitResult> splitResultList = treatmentManager.getTreatmentsWithConfig(splitList, null, false);

        assertControl(splitList, treatment, treatmentList, splitResult, splitResultList);
    }

    @Test
    public void testLongKey() {
        String matchingKey = Utils.repeat("a", 251);
        String splitName = "FACUNDO_TEST";
        List<String> splitList = new ArrayList<>();
        TreatmentManager treatmentManager = createTreatmentManager(matchingKey, matchingKey);

        String treatment = treatmentManager.getTreatment(splitName, null, false);
        SplitResult splitResult = treatmentManager.getTreatmentWithConfig(splitName, null, false);
        Map<String, String> treatmentList = treatmentManager.getTreatments(splitList, null, false);
        Map<String, SplitResult> splitResultList = treatmentManager.getTreatmentsWithConfig(splitList, null, false);

        assertControl(splitList, treatment, treatmentList, splitResult, splitResultList);
    }

    @Test
    public void testNullSplit() {
        String matchingKey = "nico_test";
        String splitName = null;
        List<String> splitList = null;
        TreatmentManager treatmentManager = createTreatmentManager(matchingKey, matchingKey);

        String treatment = treatmentManager.getTreatment(splitName, null, false);
        SplitResult splitResult = treatmentManager.getTreatmentWithConfig(splitName, null, false);
        Map<String, String> treatmentList = treatmentManager.getTreatments(splitList, null, false);
        Map<String, SplitResult> splitResultList = treatmentManager.getTreatmentsWithConfig(splitList, null, false);

        Assert.assertNotNull(treatment);
        Assert.assertEquals(Treatments.CONTROL, treatment);
    }

    @Test
    public void testDefinitionNotFoundLabel() {
        Evaluator evaluatorMock = mock(Evaluator.class);
        when(evaluatorMock.getTreatment(eq("matching_key"), eq("bucketing_key"), eq("FACUNDO_TEST"), any()))
                .thenReturn(new EvaluationResult("off", DEFINITION_NOT_FOUND));

        TreatmentManagerImpl tManager = initializeTreatmentManager(evaluatorMock);

        tManager.getTreatment("FACUNDO_TEST", null, false);
        verifyNoInteractions(impressionListener);
    }

    @Test
    public void getTreatmentTakesValuesFromAttributesManagerIntoAccount() {

        treatmentManager.getTreatment("test_split", new HashMap<>(), false);

        verify(attributesManager).getAllAttributes();
    }

    @Test
    public void getTreatmentWithConfigTakesValuesFromAttributesManagerIntoAccount() {

        treatmentManager.getTreatmentWithConfig("test_split", new HashMap<>(), false);

        verify(attributesManager).getAllAttributes();
    }

    @Test
    public void getTreatmentsTakesValuesFromAttributesManagerIntoAccount() {
        ArrayList<String> splits = new ArrayList<>();
        splits.add("test_split_1");
        splits.add("test_split_2");

        treatmentManager.getTreatments(splits, new HashMap<>(), false);

        verify(attributesManager).getAllAttributes();
    }

    @Test
    public void getTreatmentsWithConfigTakesValuesFromAttributesManagerIntoAccount() {
        ArrayList<String> splits = new ArrayList<>();
        splits.add("test_split_1");
        splits.add("test_split_2");

        treatmentManager.getTreatmentsWithConfig(splits, new HashMap<>(), false);

        verify(attributesManager).getAllAttributes();
    }

    @Test
    public void evaluationWhenNotReadyLogsCorrectMessage() {
        ValidationMessageLogger validationMessageLogger = mock(ValidationMessageLogger.class);
        SplitValidator splitValidator = mock(SplitValidator.class);
        Evaluator evaluatorMock = mock(Evaluator.class);
        ListenableEventsManager eventsManager = mock(ListenableEventsManager.class);
        when(evaluatorMock.getTreatment(eq("my_key"), eq(null), eq("test_split"), anyMap()))
                .thenReturn(new EvaluationResult("test", "test"));
        when(splitValidator.validateName(any())).thenReturn(null);
        when(splitValidator.splitNotFoundMessage(any())).thenReturn(null);
        when(eventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY)).thenReturn(false);
        when(eventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY_FROM_CACHE)).thenReturn(false);
        createTreatmentManager("my_key", null, validationMessageLogger, splitValidator, evaluatorMock, eventsManager)
                .getTreatment("test_split", null, false);

        verify(validationMessageLogger).w(eq("the SDK is not ready, results may be incorrect for feature flag test_split. Make sure to wait for SDK readiness before using this method"), any());
    }

    @Test
    public void trackValueFromEvaluationResultGetsPassedInToImpression() {
        Evaluator evaluatorMock = mock(Evaluator.class);
        when(evaluatorMock.getTreatment(eq("matching_key"), eq("bucketing_key"), eq("test_impressions_disabled"), eq(new HashMap<>())))
                .thenReturn(new EvaluationResult("test", "test", true));
        TreatmentManagerImpl tManager = initializeTreatmentManager(evaluatorMock);

        tManager.getTreatment("test_impressions_disabled", null, false);

        verify(impressionListener).log(argThat((DecoratedImpression decoratedImpression) -> {
            return decoratedImpression.isImpressionsDisabled();
        }));
    }

    private void assertControl(List<String> splitList, String treatment, Map<String, String> treatmentList, SplitResult splitResult, Map<String, SplitResult> splitResultList) {
        Assert.assertNotNull(treatment);
        Assert.assertEquals(Treatments.CONTROL, treatment);

        Assert.assertNotNull(splitResult);
        Assert.assertEquals(Treatments.CONTROL, splitResult.treatment());
        Assert.assertNull(splitResult.config());

        for (String split : splitList) {
            Assert.assertNotNull(treatmentList.get(split));
        }

        for (SplitResult result : splitResultList.values()) {
            Assert.assertNotNull(treatment);
            Assert.assertEquals(Treatments.CONTROL, treatment);
        }

        for (SplitResult result : splitResultList.values()) {
            Assert.assertNotNull(splitResult);
            Assert.assertEquals(Treatments.CONTROL, splitResult.treatment());
            Assert.assertNull(splitResult.config());
        }
    }

    private TreatmentManager createTreatmentManager(String matchingKey, String bucketingKey) {
        return createTreatmentManager(matchingKey, bucketingKey, new ValidationMessageLoggerImpl(), new SplitValidatorImpl(), evaluator, eventsManagerStub);
    }

    private TreatmentManager createTreatmentManager(String matchingKey, String bucketingKey, ValidationMessageLogger validationLogger, SplitValidator splitValidator, Evaluator evaluator, ListenableEventsManager eventsManager) {

        SplitClientConfig config = SplitClientConfig.builder().build();
        return new TreatmentManagerImpl(
                matchingKey, bucketingKey, evaluator,
                new KeyValidatorImpl(), splitValidator,
                mock(ImpressionListener.FederatedImpressionListener.class), config.labelsEnabled(), eventsManager,
                mock(AttributesManager.class), mock(AttributesMerger.class),
                mock(TelemetryStorageProducer.class), mFlagSetsFilter, mSplitsStorage, validationLogger, new FlagSetsValidatorImpl());
    }

    private TreatmentManagerImpl initializeTreatmentManager() {
        return initializeTreatmentManager(mock(Evaluator.class));
    }

    private TreatmentManagerImpl initializeTreatmentManager(Evaluator evaluator) {
        ListenableEventsManager eventsManager = mock(ListenableEventsManager.class);

        Mockito.when(eventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY)).thenReturn(true);
        Mockito.when(eventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY_FROM_CACHE)).thenReturn(true);
        Mockito.when(evaluator.getTreatment(eq("matching_key"), eq("bucketing_key"), eq("test_split"), anyMap())).thenReturn(new EvaluationResult("test", "test"));
        Mockito.when(evaluator.getTreatment(eq("matching_key"), eq("bucketing_key"), eq("test_split_1"), anyMap())).thenReturn(new EvaluationResult("test", "test"));
        Mockito.when(evaluator.getTreatment(eq("matching_key"), eq("bucketing_key"), eq("test_split_2"), anyMap())).thenReturn(new EvaluationResult("test", "test"));

        return new TreatmentManagerImpl(
                "matching_key",
                "bucketing_key",
                evaluator,
                mock(KeyValidator.class),
                mock(SplitValidator.class),
                impressionListener,
                SplitClientConfig.builder().build().labelsEnabled(),
                eventsManager,
                attributesManager,
                mock(AttributesMerger.class),
                telemetryStorageProducer,
                mFlagSetsFilter,
                mSplitsStorage,
                new ValidationMessageLoggerImpl(), new FlagSetsValidatorImpl());
    }

    private Map<String, Split> splitsMap(List<Split> splits) {
        Map<String, Split> splitsMap = new HashMap<>();
        for (Split split : splits) {
            splitsMap.put(split.name, split);
        }
        return splitsMap;
    }

}
