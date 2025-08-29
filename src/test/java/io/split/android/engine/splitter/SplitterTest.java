package io.split.android.engine.splitter;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.split.android.client.dtos.Partition;
import io.split.android.client.fallback.FallbackConfiguration;
import io.split.android.client.fallback.FallbackTreatment;
import io.split.android.client.fallback.FallbackTreatmentsCalculator;
import io.split.android.client.fallback.FallbackTreatmentsCalculatorImpl;

/**
 * Test for Splitter.
 */
public class SplitterTest {

    @Test
    public void works() {
        List<Partition> partitions = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            partitions.add(partition("" + i, 1));
        }

        int[] treatments = new int[100];

        int n = 100000;
        double p = 0.01d;

        for (int i = 0; i < n; i++) {
            String key = RandomStringUtils.random(20);
            String treatment = Splitter.getTreatment(key, 123, partitions, 1, new FallbackTreatmentsCalculatorImpl(FallbackConfiguration.builder().build()));
            treatments[Integer.parseInt(treatment) - 1]++;
        }

        double mean = n * p;
        double stddev = Math.sqrt(mean * (1 - p));

        int min = (int) (mean - 4 * stddev);
        int max = (int) (mean + 4 * stddev);

        for (int treatment : treatments) {
            assertThat(String.format("Value: " + treatment + " is out of range [%s, %s]", min, max), treatment >= min && treatment <= max, is(true));
        }
    }

    @Test
    public void ifHundredPercentOneTreatmentWeShortcut() {
        Partition partition = partition("on", 100);

        List<Partition> partitions = Collections.singletonList(partition);

        assertThat(Splitter.getTreatment("13", 15, partitions, 1, new FallbackTreatmentsCalculatorImpl(FallbackConfiguration.builder().build())), is(equalTo("on")));
    }

    @Test
    public void ifNoPartitionsWeReturnGetValueFromFallbackCalculator() {
        FallbackTreatmentsCalculator calculator = Mockito.mock(FallbackTreatmentsCalculator.class);

        when(calculator.resolve(anyString())).thenReturn(new FallbackTreatment("on"));

        assertEquals("on", Splitter.getTreatment("13", 15, Collections.emptyList(), 1, calculator));
        verify(calculator).resolve("13");
    }

    private Partition partition(String treatment, int size) {
        Partition p = new Partition();
        p.treatment = treatment;
        p.size = size;
        return p;
    }
}
