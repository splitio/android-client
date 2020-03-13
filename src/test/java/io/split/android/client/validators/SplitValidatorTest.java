package io.split.android.client.validators;

import org.junit.Assert;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import io.split.android.client.dtos.Split;
import io.split.android.engine.experiments.SplitFetcher;
import io.split.android.engine.segments.RefreshableMySegmentsFetcherProvider;
import io.split.android.fake.RefreshableMySegmentsFetcherProviderStub;
import io.split.android.fake.SplitFetcherStub;
import io.split.android.helpers.FileHelper;

public class SplitValidatorTest {

    private SplitValidator validator;
    private final  String  tag = "SplitNameValidatorTests";

    @Before
    public void setUp() {
        validator = new SplitValidatorImpl();
    }

    @Test
    public void testValidName() {
        String splitName = "split1";
        ValidationErrorInfo errorInfo = validator.validateName(splitName);

        Assert.assertNull(errorInfo);
    }

    @Test
    public void testNullName() {
        ValidationErrorInfo errorInfo = validator.validateName(null);

        Assert.assertNotNull(errorInfo);
        Assert.assertTrue(errorInfo.isError());
        Assert.assertEquals("you passed a null split name, split name must be a non-empty string", errorInfo.getErrorMessage());
    }

    @Test
    public void testInvalidEmptyName() {
        ValidationErrorInfo errorInfo = validator.validateName("");

        Assert.assertNotNull(errorInfo);
        Assert.assertTrue(errorInfo.isError());
        Assert.assertEquals("you passed an empty split name, split name must be a non-empty string", errorInfo.getErrorMessage());
    }

    public void testInvalidAllSpacesInName() {
        ValidationErrorInfo errorInfo = validator.validateName("    ");

        Assert.assertNotNull(errorInfo);
        Assert.assertTrue(errorInfo.isError());
        Assert.assertEquals("you passed a empty split name, split name must be a non-empty string", errorInfo.getErrorMessage());
    }

    @Test
    public void testLeadingSpacesName() {
        String splitName = " splitName";
        ValidationErrorInfo errorInfo = validator.validateName(splitName);

        Assert.assertNotNull(errorInfo);
        Assert.assertFalse(errorInfo.isError());
        Assert.assertEquals("split name ' splitName' has extra whitespace, trimming", errorInfo.getWarnings().get(ValidationErrorInfo.WARNING_SPLIT_NAME_SHOULD_BE_TRIMMED));
    }

    @Test
    public void testTrailingSpacesName() {
        String splitName = "splitName ";
        ValidationErrorInfo errorInfo = validator.validateName(splitName);

        Assert.assertNotNull(errorInfo);
        Assert.assertFalse(errorInfo.isError());
        Assert.assertEquals("split name 'splitName ' has extra whitespace, trimming", errorInfo.getWarnings().get(ValidationErrorInfo.WARNING_SPLIT_NAME_SHOULD_BE_TRIMMED));
    }

    private Split createSplit(String name) {
        Split split = new Split();
        split.name = name;
        return split;
    }
}
