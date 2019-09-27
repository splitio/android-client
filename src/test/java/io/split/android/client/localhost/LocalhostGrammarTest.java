package io.split.android.client.localhost;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LocalhostGrammarTest {

    LocalhostGrammar localhostGrammar;

    @Before
    public void setup() {
        localhostGrammar = new LocalhostGrammar();
    }

    @Test
    public void testBuildSplitOnlyName() {
        String fullName = localhostGrammar.buildSplitKeyName("split", null);
        Assert.assertEquals("split", fullName);
    }

    @Test
    public void testBuildSplitKeyName() {
        String fullName = localhostGrammar.buildSplitKeyName("split", "key");
        Assert.assertEquals("split:key", fullName);
    }

    @Test
    public void testBuildNullSplit() {
        String fullName = localhostGrammar.buildSplitKeyName(null, "key");
        Assert.assertNull(fullName);
    }

    @Test
    public void testBuildEmptySplit() {
        String fullName = localhostGrammar.buildSplitKeyName("", "key");
        Assert.assertNull(fullName);
    }

    @Test
    public void testBuildEmptyKey() {
        String fullName = localhostGrammar.buildSplitKeyName("split", "");
        Assert.assertEquals("split", fullName);
    }

    @Test
    public void testBuildBothNull() {
        String fullName = localhostGrammar.buildSplitKeyName(null, null);
        Assert.assertNull(fullName);
    }

    @Test
    public void testSplitOnly() {
        String name = localhostGrammar.getSplitName("split");
        Assert.assertEquals("split", name);
    }

    @Test
    public void testSplitKey() {
        String name = localhostGrammar.getSplitName("split:key");
        Assert.assertEquals("split", name);
    }

    @Test
    public void testNull() {
        String name = localhostGrammar.getSplitName(null);
        Assert.assertNull(name);
    }

    @Test
    public void testEmpty() {
        String name = localhostGrammar.getSplitName("");
        Assert.assertNull(name);
    }


}
