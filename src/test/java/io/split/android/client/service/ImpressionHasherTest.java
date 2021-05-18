package io.split.android.client.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.split.android.client.impressions.Impression;
import io.split.android.client.service.impressions.ImpressionHasher;

public class ImpressionHasherTest {

    Impression baseImpression;
    Long baseHash;

    @Before
    public void setup() {
        baseImpression = baseImpression();
        baseHash = ImpressionHasher.process(baseImpression);
    }

    @Test
    public void differentFeature() {

        Impression imp2 = new Impression("someKey",
                null,
                "someOtherFeature",
                "someTreatment",
                System.currentTimeMillis(),
                "someLabel",
                123L,
                null);

        Long hash2 = ImpressionHasher.process(imp2);

        Assert.assertNotEquals(baseHash, hash2);

    }

    @Test
    public void differentKey() {
        // different key
        Impression imp2 = new Impression("someOtherKey",
                null,
                "someFeature",
                "someTreatment",
                System.currentTimeMillis(),
                "someLabel",
                123L,
                null);

        Long hash2 = ImpressionHasher.process(imp2);

        Assert.assertNotEquals(baseHash, hash2);

    }

    @Test
    public void differentChangeNumber() {

        // different changeNumber
        Impression imp2 = new Impression("someKey",
                null,
                "someFeature",
                "someTreatment",
                System.currentTimeMillis(),
                "someLabel",
                456L,
                null);
        Long hash2 = ImpressionHasher.process(imp2);

        Assert.assertNotEquals(baseHash, hash2);
    }

    @Test
    public void differentLabel() {
        // different label
        Impression imp2 = new Impression("someKey",
                null,
                "someFeature",
                "someTreatment",
                System.currentTimeMillis(),
                "someOtherLabel",
                123L,
                null);
        Long hash2 = ImpressionHasher.process(imp2);

        Assert.assertNotEquals(baseHash, hash2);
    }

    @Test
    public void differentTreatment() {
        // different label
        Impression imp2 = new Impression("someKey",
                null,
                "someFeature",
                "someOtherTreatment",
                System.currentTimeMillis(),
                "someLabel",
                123L,
                null);

        Long hash2 = ImpressionHasher.process(imp2);

        Assert.assertNotEquals(baseHash, hash2);
    }

    @Test
    public void noCrashWhenSplitNull() {
        Impression imp1 = new Impression("someKey",
                null,
                null,
                "someTreatment",
                System.currentTimeMillis(),
                "someLabel",
                123L,
                null);

        Long hash = ImpressionHasher.process(imp1);

        Assert.assertNotNull(imp1);
    }

    @Test
    public void noCrashWhenSplitAndKeyNull() {

        Impression imp1 = new Impression(null,
                null,
                null,
                "someTreatment",
                System.currentTimeMillis(),
                "someLabel",
                123L,
                null);

        Long hash = ImpressionHasher.process(imp1);

        Assert.assertNotNull(imp1);
    }

    @Test
    public void noCrashWhenKeySplitChangeNumberNull() {

        Impression imp1 = new Impression(null,
                null,
                null,
                "someTreatment",
                System.currentTimeMillis(),
                "someLabel",
                null,
                null);

        Long hash = ImpressionHasher.process(imp1);

        Assert.assertNotNull(imp1);
    }

    @Test
    public void noCrashWhenKeySplitChangeNumberAppliedRuleNull() {

        Impression imp1 = new Impression(null,
                null,
                null,
                "someTreatment",
                System.currentTimeMillis(),
                null,
                null,
                null);

        Long hash = ImpressionHasher.process(imp1);

        Assert.assertNotNull(imp1);
    }

    @Test
    public void noCrashWhenOnlyAppliedRuleNotNull() {

        Impression imp1 = new Impression(null,
                null,
                null,
                null,
                System.currentTimeMillis(),
                "someLabel",
                null,
                null);

        Assert.assertNotNull(imp1);
    }

    @Test
    public void noCrashWhenNull() {

        Long hash = ImpressionHasher.process(null);

        Assert.assertNull(hash);
    }

    private Impression baseImpression() {
        return new Impression("someKey",
                null,
                "someFeature",
                "someTreatment",
                System.currentTimeMillis(),
                "someLabel",
                123L,
                null);
    }
}