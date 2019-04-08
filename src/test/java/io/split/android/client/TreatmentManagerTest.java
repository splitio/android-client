package io.split.android.client;

import com.google.common.base.Strings;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.Split;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.validators.KeyValidatorImpl;
import io.split.android.client.validators.SplitValidatorImpl;
import io.split.android.client.validators.TreatmentManager;
import io.split.android.client.validators.TreatmentManagerImpl;
import io.split.android.engine.experiments.SplitFetcher;
import io.split.android.engine.metrics.Metrics;
import io.split.android.engine.segments.RefreshableMySegmentsFetcherProvider;
import io.split.android.fake.ImpressionListenerMock;
import io.split.android.fake.MetricsMock;
import io.split.android.fake.RefreshableMySegmentsFetcherProviderStub;
import io.split.android.fake.SplitFetcherStub;
import io.split.android.grammar.Treatments;
import io.split.android.helpers.FileHelper;

public class TreatmentManagerTest {

    SplitFetcher splitFetcher;
    Evaluator evaluator;
    ImpressionListener impressionListener;
    Metrics metrics;

    @Before
    public void loadSplitsFromFile(){
        if(splitFetcher == null) {
            FileHelper fileHelper = new FileHelper();
            List<String> mySegments = Arrays.asList("s1", "s2", "test_copy");
            RefreshableMySegmentsFetcherProvider mySegmentsProvider = new RefreshableMySegmentsFetcherProviderStub(mySegments);
            List<Split> splits = fileHelper.loadAndParseSplitChangeFile("split_changes_1.json");
            SplitFetcher splitFetcher = new SplitFetcherStub(splits, mySegmentsProvider);
            evaluator = new EvaluatorImpl(splitFetcher);
        }
        impressionListener = new ImpressionListenerMock();
        metrics = new MetricsMock();
    }

    @Test
    public void testBasicEvaluationNoConfig() {
        String matchingKey = "the_key";
        String splitName = "FACUNDO_TEST";

        TreatmentManager treatmentManager = createTreatmentManager(matchingKey, matchingKey);
        SplitResult splitResult = treatmentManager.getTreatmentWithConfig(splitName, null, false, false);

        Assert.assertNotNull(splitResult);
        Assert.assertEquals("off", splitResult.getTreatment());
        Assert.assertNull(splitResult.getConfigurations());
    }

    @Test
    public void testBasicEvaluationWithConfig() {
        String matchingKey = "the_key";
        String splitName = "Test";

        TreatmentManager treatmentManager = createTreatmentManager(matchingKey, matchingKey);
        SplitResult splitResult = treatmentManager.getTreatmentWithConfig(splitName, null, false, false);

        Assert.assertNotNull(splitResult);
        Assert.assertEquals("off", splitResult.getTreatment());
        Assert.assertNotNull(splitResult.getConfigurations());
    }

    @Test
    public void testBasicEvaluations() {
        String matchingKey = "thekey";
        List<String> splitList = Arrays.asList("FACUNDO_TEST", "testo2222", "Test");

        TreatmentManager treatmentManager = createTreatmentManager(matchingKey, matchingKey);
        Map<String, SplitResult> splitResultList = treatmentManager.getTreatmentsWithConfig(splitList, null, false, false);

        SplitResult r1 = splitResultList.get("FACUNDO_TEST");
        SplitResult r2 = splitResultList.get("testo2222");
        SplitResult r3 = splitResultList.get("Test");

        Assert.assertNotNull(r1);
        Assert.assertEquals("off", r1.getTreatment());
        Assert.assertNull(r1.getConfigurations());

        Assert.assertNotNull(r2);
        Assert.assertEquals("pesto", r2.getTreatment());
        Assert.assertNull(r2.getConfigurations());

        Assert.assertNotNull(r3);
        Assert.assertEquals("off", r3.getTreatment());
        Assert.assertNotNull(r3.getConfigurations());
    }

    @Test
    public void testClientIsDestroyed() {
        String matchingKey = "nico_test";
        String splitName = "FACUNDO_TEST";
        List<String> splitList = Arrays.asList("FACUNDO_TEST", "a_new_split_2", "benchmark_jw_1");
        TreatmentManager treatmentManager = createTreatmentManager(matchingKey, matchingKey);

        String treatment = treatmentManager.getTreatment(splitName, null, true, false);
        SplitResult splitResult = treatmentManager.getTreatmentWithConfig(splitName, null, true, false);
        Map<String, String> treatmentList = treatmentManager.getTreatments(splitList, null, true, false);
        Map<String, SplitResult> splitResultList = treatmentManager.getTreatmentsWithConfig(splitList, null, true, false);
        assertControl(splitList, treatment, treatmentList, splitResult, splitResultList);
    }

    @Test
    public void testNonExistingSplits() {
        String matchingKey = "nico_test";
        String splitName = "NON_EXISTING_1";
        List<String> splitList = Arrays.asList("NON_EXISTING_1", "NON_EXISTING_2", "NON_EXISTING_3");
        TreatmentManager treatmentManager = createTreatmentManager(matchingKey, matchingKey);

        String treatment = treatmentManager.getTreatment(splitName, null, false, false);
        SplitResult splitResult = treatmentManager.getTreatmentWithConfig(splitName, null, false, false);
        Map<String, String> treatmentList = treatmentManager.getTreatments(splitList, null, false, false);
        Map<String, SplitResult> splitResultList = treatmentManager.getTreatmentsWithConfig(splitList, null, false, false);
        assertControl(splitList, treatment, treatmentList, splitResult, splitResultList);
    }

    @Test
    public void testEmtpySplit() {
        String matchingKey = "nico_test";
        String splitName = "";
        List<String> splitList = new ArrayList<>();
        TreatmentManager treatmentManager = createTreatmentManager(matchingKey, matchingKey);

        String treatment = treatmentManager.getTreatment(splitName, null, false, false);
        SplitResult splitResult = treatmentManager.getTreatmentWithConfig(splitName, null, false, false);
        Map<String, String> treatmentList = treatmentManager.getTreatments(splitList, null, false, false);
        Map<String, SplitResult> splitResultList = treatmentManager.getTreatmentsWithConfig(splitList, null, false, false);

        assertControl(splitList, treatment, treatmentList, splitResult, splitResultList);
    }

    @Test
    public void testNullKey() {
        String matchingKey = null;
        String splitName = "FACUNDO_TEST";
        List<String> splitList = Arrays.asList("FACUNDO_TEST", "a_new_split_2", "benchmark_jw_1");
        TreatmentManager treatmentManager = createTreatmentManager(matchingKey, matchingKey);

        String treatment = treatmentManager.getTreatment(splitName, null, false, false);
        SplitResult splitResult = treatmentManager.getTreatmentWithConfig(splitName, null, false, false);
        Map<String, String> treatmentList = treatmentManager.getTreatments(splitList, null, false, false);
        Map<String, SplitResult> splitResultList = treatmentManager.getTreatmentsWithConfig(splitList, null, false, false);

        assertControl(splitList, treatment, treatmentList, splitResult, splitResultList);
    }

    @Test
    public void testEmptyKey() {
        String matchingKey = "";
        String splitName = "FACUNDO_TEST";
        List<String> splitList = new ArrayList<>();
        TreatmentManager treatmentManager = createTreatmentManager(matchingKey, matchingKey);

        String treatment = treatmentManager.getTreatment(splitName, null, false, false);
        SplitResult splitResult = treatmentManager.getTreatmentWithConfig(splitName, null, false, false);
        Map<String, String> treatmentList = treatmentManager.getTreatments(splitList, null, false, false);
        Map<String, SplitResult> splitResultList = treatmentManager.getTreatmentsWithConfig(splitList, null, false, false);

        assertControl(splitList, treatment, treatmentList, splitResult, splitResultList);
    }

    @Test
    public void testLongKey() {
        String matchingKey = Strings.repeat("a", 251);
        String splitName = "FACUNDO_TEST";
        List<String> splitList = new ArrayList<>();
        TreatmentManager treatmentManager = createTreatmentManager(matchingKey, matchingKey);

        String treatment = treatmentManager.getTreatment(splitName, null, false, false);
        SplitResult splitResult = treatmentManager.getTreatmentWithConfig(splitName, null, false, false);
        Map<String, String> treatmentList = treatmentManager.getTreatments(splitList, null, false, false);
        Map<String, SplitResult> splitResultList = treatmentManager.getTreatmentsWithConfig(splitList, null, false, false);

        assertControl(splitList, treatment, treatmentList, splitResult, splitResultList);
    }

    @Test
    public void testNullSplit() {
        String matchingKey = "nico_test";
        String splitName = null;
        List<String> splitList = null;
        TreatmentManager treatmentManager = createTreatmentManager(matchingKey, matchingKey);

        String treatment = treatmentManager.getTreatment(splitName, null, false, false);
        SplitResult splitResult = treatmentManager.getTreatmentWithConfig(splitName, null, false, false);
        Map<String, String> treatmentList = treatmentManager.getTreatments(splitList, null, false, false);
        Map<String, SplitResult> splitResultList = treatmentManager.getTreatmentsWithConfig(splitList, null, false, false);

        Assert.assertNotNull(treatment);
        Assert.assertEquals(Treatments.CONTROL, treatment);
    }

    private void assertControl(List<String> splitList, String treatment, Map<String, String> treatmentList, SplitResult splitResult, Map<String, SplitResult> splitResultList) {
        Assert.assertNotNull(treatment);
        Assert.assertEquals(Treatments.CONTROL, treatment);

        Assert.assertNotNull(splitResult);
        Assert.assertEquals(Treatments.CONTROL, splitResult.getTreatment());
        Assert.assertNull(splitResult.getConfigurations());

        for(String split : splitList) {
            Assert.assertNotNull(treatmentList.get(split));
        }

        for(SplitResult result : splitResultList.values()) {
            Assert.assertNotNull(treatment);
            Assert.assertEquals(Treatments.CONTROL, treatment);
        }

        for(SplitResult result : splitResultList.values()) {
            Assert.assertNotNull(splitResult);
            Assert.assertEquals(Treatments.CONTROL, splitResult.getTreatment());
            Assert.assertNull(splitResult.getConfigurations());
        }
    }

    private TreatmentManager createTreatmentManager(String matchingKey, String bucketingKey) {
        SplitClientConfig config = SplitClientConfig.builder().build();
        return new TreatmentManagerImpl(
                matchingKey, bucketingKey, evaluator,
                new KeyValidatorImpl(), new SplitValidatorImpl(), new MetricsMock(),
                new ImpressionListenerMock(), config);
    }

}
