package io.split.android.client.storage.rbs;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import org.junit.Test;

public class LazyRuleBasedSegmentStorageProviderTest {

    @Test
    public void refCanOnlyBeSetOnce() {
        LazyRuleBasedSegmentStorageProvider provider = new LazyRuleBasedSegmentStorageProvider();
        RuleBasedSegmentStorage firstInstance = mock(RuleBasedSegmentStorage.class);
        RuleBasedSegmentStorage secondInstance = mock(RuleBasedSegmentStorage.class);
        provider.set(firstInstance);
        provider.set(secondInstance);

        assertSame(firstInstance, provider.get());
        assertNotSame(secondInstance, provider.get());
    }

    @Test
    public void getReturnsNullWhenSetHasNotBeenCalled() {
        LazyRuleBasedSegmentStorageProvider provider = new LazyRuleBasedSegmentStorageProvider();
        assertNull(provider.get());
    }
}
