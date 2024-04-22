package io.split.android.engine.experiments;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import io.split.android.client.dtos.ConditionType;
import io.split.android.client.dtos.Partition;

public class DefaultConditionsProviderTest {

    private DefaultConditionsProvider mDefaultConditionsProvider;

    @Before
    public void setUp() {
        mDefaultConditionsProvider = new DefaultConditionsProvider();
    }

    @Test
    public void defaultConditionListContainsOneCondition() {
        List<ParsedCondition> defaultConditions = mDefaultConditionsProvider.getDefaultConditions();
        assertEquals(1, defaultConditions.size());
    }

    @Test
    public void defaultConditionHasWhitelistType() {
        List<ParsedCondition> defaultConditions = mDefaultConditionsProvider.getDefaultConditions();
        assertEquals(ConditionType.WHITELIST, defaultConditions.get(0).conditionType());
    }

    @Test
    public void defaultConditionHasUnsupportedMatcherTypeLabel() {
        List<ParsedCondition> defaultConditions = mDefaultConditionsProvider.getDefaultConditions();
        assertEquals("unsupported matcher type", defaultConditions.get(0).label());
    }

    @Test
    public void defaultConditionHasControlPartition() {
        List<ParsedCondition> defaultConditions = mDefaultConditionsProvider.getDefaultConditions();
        List<Partition> partitions = defaultConditions.get(0).partitions();
        Partition partition = partitions.get(0);

        assertEquals(1, partitions.size());
        assertEquals(100, partition.size);
        assertEquals("control", partition.treatment);
    }
}
