package io.split.android.client.localhost;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import io.split.android.client.dtos.ConditionType;
import io.split.android.client.dtos.Split;
import io.split.android.helpers.FileHelper;

@SuppressWarnings("ConstantConditions")
public class LocalhostYamlParserTest {

    LocalhostFileParser parser;
    FileHelper mFileHelper = new FileHelper();

    @Before
    public void setup() {
        parser = new LocalhostYamlFileParser();
    }

    @Test
    public void testCorrectFormat() {

        Map<String, Split> splits = parser.parse(mFileHelper.loadFileContent("splits.yaml"));

        Assert.assertEquals(8, splits.size());

        Split split0 = splits.get("split_0");
        Split split1 = splits.get("split_1");
        Split split2 = splits.get("split_2");
        Split myFeature = splits.get("my_feature");
        Split otherFeature3 = splits.get("other_feature_3");
        Split xFeature = splits.get("x_feature");
        Split otherFeature = splits.get("other_feature");
        Split otherFeature2 = splits.get("other_feature_2");

        Assert.assertNotNull(split0);
        Assert.assertNotNull(split1);
        Assert.assertNotNull(split2);
        Assert.assertNotNull(myFeature);
        Assert.assertNotNull(otherFeature3);
        Assert.assertNotNull(xFeature);
        Assert.assertNotNull(otherFeature);
        Assert.assertNotNull(otherFeature2);

        Assert.assertEquals("split_0", split0.name);
        Assert.assertEquals("control", split0.defaultTreatment);
        Assert.assertNotNull(split0.configurations);
        Assert.assertEquals("{ \"size\" : 20 }", split0.configurations.get("off"));
        Assert.assertEquals(1, split0.conditions.size());

        Assert.assertEquals("split_2", split2.name);
        Assert.assertEquals("control", split2.defaultTreatment);
        Assert.assertNotNull(split2.configurations);
        Assert.assertEquals("{ \"size\" : 20 }", split2.configurations.get("off"));
        Assert.assertEquals(1, split0.conditions.size());

        Assert.assertEquals("my_feature", myFeature.name);
        Assert.assertEquals("control", myFeature.defaultTreatment);
        Assert.assertNotNull(myFeature.configurations);
        Assert.assertEquals("{\"desc\" : \"this applies only to ON treatment\"}", myFeature.configurations.get("on"));
        Assert.assertEquals(2, myFeature.conditions.size());
        Assert.assertEquals(ConditionType.WHITELIST, myFeature.conditions.get(0).conditionType);
        Assert.assertEquals("white", myFeature.conditions.get(0).partitions.get(0).treatment);
        Assert.assertEquals(ConditionType.ROLLOUT, myFeature.conditions.get(1).conditionType);
        Assert.assertEquals("on", myFeature.conditions.get(1).partitions.get(0).treatment);

        Assert.assertEquals("other_feature_3", otherFeature3.name);
        Assert.assertEquals("control", otherFeature3.defaultTreatment);
        Assert.assertNull(otherFeature3.configurations);
        Assert.assertEquals(1, otherFeature3.conditions.size());
        Assert.assertEquals(ConditionType.ROLLOUT, otherFeature3.conditions.get(0).conditionType);
        Assert.assertEquals("off", otherFeature3.conditions.get(0).partitions.get(0).treatment);

        Assert.assertEquals("x_feature", xFeature.name);
        Assert.assertEquals("control", xFeature.defaultTreatment);
        Assert.assertNotNull(xFeature.configurations);
        Assert.assertEquals("{\"desc\" : \"this applies only to OFF and only for only_key. The rest will receive ON\"}", xFeature.configurations.get("off"));
        Assert.assertEquals(2, xFeature.conditions.size());
        Assert.assertEquals(ConditionType.WHITELIST, xFeature.conditions.get(0).conditionType);
        Assert.assertEquals("on", xFeature.conditions.get(0).partitions.get(0).treatment);
        Assert.assertEquals(ConditionType.ROLLOUT, xFeature.conditions.get(1).conditionType);
        Assert.assertEquals("off", xFeature.conditions.get(1).partitions.get(0).treatment);

        Assert.assertEquals("other_feature", otherFeature.name);
        Assert.assertEquals("control", otherFeature.defaultTreatment);
        Assert.assertNull(otherFeature.configurations);
        Assert.assertEquals(ConditionType.ROLLOUT, otherFeature.conditions.get(0).conditionType);
        Assert.assertEquals("on", otherFeature.conditions.get(0).partitions.get(0).treatment);

        Assert.assertEquals("other_feature_2", otherFeature2.name);
        Assert.assertEquals("control", otherFeature2.defaultTreatment);
        Assert.assertNull(otherFeature2.configurations);
        Assert.assertEquals(ConditionType.ROLLOUT, otherFeature2.conditions.get(0).conditionType);
        Assert.assertEquals("on", otherFeature2.conditions.get(0).partitions.get(0).treatment);

    }

    @Test
    public void testWrongYamlSyntax() {
        Map<String, Split> splits = parser.parse(mFileHelper.loadFileContent("splits_no_yaml.yaml"));
        Assert.assertNull(splits);
    }

    @Test
    public void testMissingTreatment() {
        Map<String, Split> splits = parser.parse(mFileHelper.loadFileContent("splits_missing_treatment.yaml"));

        Assert.assertEquals(1, splits.size());

        Split split0 = splits.get("split_0");
        Split split1 = splits.get("split_1");

        Assert.assertEquals("split_0", split0.name);
        Assert.assertEquals("control", split0.defaultTreatment);
        Assert.assertEquals(1, split0.conditions.size());
        Assert.assertEquals("{ \"size\" : 20 }", split0.configurations.get("off"));

        Assert.assertNull(split1);
    }

    @Test
    public void testMissingNameInFirstSplit() {
        Map<String, Split> splits = parser.parse(mFileHelper.loadFileContent("splits_missing_name.yaml"));
        Assert.assertNull(splits);
    }

    @Test
    public void testNonStringValueInTreatmentOnlyExcludesFailingSplit() {
        Map<String, Split> splits = parser.parse(mFileHelper.loadFileContent("splits_incorrect_value.yaml"));
        Assert.assertEquals(4, splits.size());
    }

}
