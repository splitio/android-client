package io.split.android.client.service.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static io.split.android.client.storage.rbs.RuleBasedSegmentStorageImplTest.createRuleBasedSegment;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.RuleBasedSegment;
import io.split.android.client.dtos.Status;

public class RuleBasedSegmentChangeProcessorTest {

    private RuleBasedSegmentChangeProcessor mProcessor;

    @Before
    public void setUp() {
        mProcessor = new RuleBasedSegmentChangeProcessor();
    }

    @Test
    public void testProcess() {
        List<RuleBasedSegment> segments = new ArrayList<>();
        segments.add(createRuleBasedSegment("segment1", Status.ACTIVE));
        segments.add(createRuleBasedSegment("segment2", Status.ARCHIVED));
        segments.add(createRuleBasedSegment("segment3", Status.ACTIVE));

        ProcessedRuleBasedSegmentChange result = mProcessor.process(segments, 123L);

        assertEquals(2, result.getActive().size());
        assertEquals(1, result.getArchived().size());
        assertEquals(123L, result.getChangeNumber());
        assertTrue(result.getActive().stream().map(RuleBasedSegment::getName).anyMatch("segment1"::equals));
        assertTrue(result.getActive().stream().map(RuleBasedSegment::getName).anyMatch("segment3"::equals));
        assertTrue(result.getArchived().stream().map(RuleBasedSegment::getName).anyMatch("segment2"::equals));
    }
}
