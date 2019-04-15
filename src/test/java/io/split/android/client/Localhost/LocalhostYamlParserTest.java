package io.split.android.client.Localhost;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import java.util.Map;
import io.split.android.client.dtos.Split;
import io.split.android.helpers.ResourcesFileStorage;

public class LocalhostYamlParserTest {

    LocalhostFileParser parser;

    @Before
    public void setup() {
        parser = new LocalhostYamlFileParser(new ResourcesFileStorage());
    }

    @Test
    public void testCorrectFormat() {

        Map<String, Split> splits = parser.parse("splits.yaml");

        Assert.assertEquals(9, splits.size());

        Split split0 = splits.get("split_0");
        Split split1 = splits.get("split_1");
        Split split1_hasKey = splits.get("split_1:haskey");
        Split split2 = splits.get("split_2");
        Split myFeature = splits.get("my_feature");
        Split otherFeature3 = splits.get("other_feature_3");
        Split xFeature = splits.get("x_feature");
        Split xFeature_theKey = splits.get("x_feature:thekey");
        Split otherFeature = splits.get("other_feature");
        Split otherFeature2 = splits.get("other_feature_2");

        Assert.assertNotNull(split0);
        Assert.assertNull(split1);
        Assert.assertNotNull(split1_hasKey);
        Assert.assertNotNull(split2);
        Assert.assertNotNull(myFeature);
        Assert.assertNotNull(otherFeature3);
        Assert.assertNotNull(xFeature);
        Assert.assertNotNull(otherFeature);
        Assert.assertNotNull(otherFeature2);

        Assert.assertEquals("split_0", split0.name);
        Assert.assertEquals("off", split0.defaultTreatment);
        Assert.assertNotNull(split0.configurations);
        Assert.assertEquals("{ \"size\" : 20 }", split0.configurations.get("off"));

        Assert.assertEquals("split_1:haskey", split1_hasKey.name);
        Assert.assertEquals("on", split1_hasKey.defaultTreatment);
        Assert.assertNull(split1_hasKey.configurations);

        Assert.assertEquals("split_2", split2.name);
        Assert.assertEquals("off", split2.defaultTreatment);
        Assert.assertNotNull(split2.configurations);
        Assert.assertEquals("{ \"size\" : 20 }", split2.configurations.get("off"));

        Assert.assertEquals("my_feature", myFeature.name);
        Assert.assertEquals("on", myFeature.defaultTreatment);
        Assert.assertNotNull(myFeature.configurations);
        Assert.assertEquals("{\"desc\" : \"this applies only to ON treatment\"}", myFeature.configurations.get("on"));

        Assert.assertEquals("other_feature_3", otherFeature3.name);
        Assert.assertEquals("off", otherFeature3.defaultTreatment);
        Assert.assertNull(otherFeature3.configurations);

        Assert.assertEquals("x_feature:thekey", xFeature_theKey.name);
        Assert.assertEquals("on", xFeature_theKey.defaultTreatment);
        Assert.assertNull(xFeature_theKey.configurations);

        Assert.assertEquals("x_feature", xFeature.name);
        Assert.assertEquals("off", xFeature.defaultTreatment);
        Assert.assertNotNull(xFeature.configurations);
        Assert.assertEquals("{\"desc\" : \"this applies only to OFF and only for only_key. The rest will receive ON\"}", xFeature.configurations.get("off"));

        Assert.assertEquals("other_feature", otherFeature.name);
        Assert.assertEquals("on", otherFeature.defaultTreatment);
        Assert.assertNull(otherFeature.configurations);

        Assert.assertEquals("other_feature_2", otherFeature2.name);
        Assert.assertEquals("on", otherFeature2.defaultTreatment);
        Assert.assertNull(otherFeature2.configurations);

    }

    @Test
    public void testWrongYamlSyntax() {
        Map<String, Split> splits = parser.parse("splits_no_yaml.yaml");
        Assert.assertNull(splits);
    }

    @Test
    public void testMissingTreatment() {
        Map<String, Split> splits = parser.parse("splits_missing_treatment.yaml");

        Assert.assertEquals(2, splits.size());

        Split split0 = splits.get("split_0");
        Split split1 = splits.get("split_1");
        Split split1HasKey = splits.get("split_1:haskey");

        Assert.assertEquals("split_0", split0.name);
        Assert.assertEquals("off", split0.defaultTreatment);
        Assert.assertEquals("{ \"size\" : 20 }", split0.configurations.get("off"));

        Assert.assertNull(split1);

        Assert.assertEquals("split_1:haskey", split1HasKey.name);
        Assert.assertEquals("control", split1HasKey.defaultTreatment);
        Assert.assertNull(split1HasKey.configurations);

    }

    @Test
    public void testMissingNameInFirstSplit() {
        Map<String, Split> splits = parser.parse("splits_missing_name.yaml");
        Assert.assertNull(splits);
    }

}
