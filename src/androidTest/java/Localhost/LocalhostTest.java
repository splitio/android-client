package Localhost;

import android.content.Context;

import org.junit.Assert;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import androidx.test.platform.app.InstrumentationRegistry;
import io.split.android.client.LocalhostSplitClient;
import io.split.android.client.LocalhostSplitFactory;
import io.split.android.client.LocalhostSplitManager;
import io.split.android.client.SplitResult;
import io.split.android.client.api.SplitView;

public class LocalhostTest {

    @Before
    public void  setUp() {
    }

    @Test
    public void testUsingYamlFile() {

        Context context = InstrumentationRegistry.getInstrumentation().getContext();

        LocalhostSplitFactory factory = null;
        LocalhostSplitClient client = null;
        LocalhostSplitManager manager = null;
        try {
            factory = new LocalhostSplitFactory("key", context);
            client = (LocalhostSplitClient) factory.client();
            manager = (LocalhostSplitManager) factory.manager();

        } catch (IOException e) {
        }

        List<SplitView> splits = manager.splits();
        SplitView sv0 = manager.split("split_0");
        SplitView sv1 = manager.split("split_1");

        SplitView svx = manager.split("x_feature");

        String s0Treatment = client.getTreatment("split_0", null);
        SplitResult s0Result = client.getTreatmentWithConfig("split_0", null);

        String s1Treatment_hasKey = client.getTreatment("split_1", null);
        SplitResult s1Result_hasKey = client.getTreatmentWithConfig("split_1", null);

        String xTreatment_key = client.getTreatment("x_feature", null);
        SplitResult xResult_key = client.getTreatmentWithConfig("x_feature", null);

        String myFeatureTreatment_key = client.getTreatment("my_feature", null);
        SplitResult myFeatureResult_key = client.getTreatmentWithConfig("my_feature", null);

        String nonExistingTreatment = client.getTreatment("nonExistingTreatment", null);

        List<String> splitNames = new ArrayList<>();
        splitNames.add("split_0");
        splitNames.add("x_feature");
        splitNames.add("my_feature");
        Map<String, String> treatments = client.getTreatments(splitNames, null);
        Map<String, SplitResult> results = client.getTreatmentsWithConfig(splitNames, null);


        Assert.assertNotNull(factory);
        Assert.assertNotNull(client);
        Assert.assertNotNull(manager);

        Assert.assertEquals("off", s0Treatment);
        Assert.assertEquals("off", s0Result.treatment());
        Assert.assertEquals("{ \"size\" : 20 }", s0Result.config());

        Assert.assertEquals("control", s1Treatment_hasKey);
        Assert.assertEquals("control", s1Result_hasKey.treatment());
        Assert.assertNull(s1Result_hasKey.config());

        Assert.assertEquals("on", xTreatment_key);
        Assert.assertEquals("on", xResult_key.treatment());
        Assert.assertNull(xResult_key.config());

        Assert.assertEquals("red", myFeatureTreatment_key);
        Assert.assertEquals("red", myFeatureResult_key.treatment());
        Assert.assertNull(myFeatureResult_key.config());

        Assert.assertEquals("control", nonExistingTreatment);

        Assert.assertEquals(9, splits.size());

        Assert.assertNotNull(sv0);
        Assert.assertEquals(sv0.treatments.get(0) , "off");
        Assert.assertEquals(sv0.configs.get("off") , "{ \"size\" : 20 }");

        Assert.assertNotNull(sv1);
        Assert.assertEquals(sv1.treatments.get(0) , "on");
        Assert.assertNotNull(sv1.configs);
        Assert.assertEquals(0, sv1.configs.size());

        Assert.assertNotNull(svx);
        Assert.assertEquals(svx.treatments.get(0) , "red");
        Assert.assertEquals(svx.treatments.get(1) , "on");
        Assert.assertEquals(svx.treatments.get(2) , "off");
        Assert.assertNull(svx.configs.get("on"));
        Assert.assertEquals("{\"desc\" : \"this applies only to OFF and only for only_key. The rest will receive ON\"}", svx.configs.get("off"));
        Assert.assertNull(svx.configs.get("red"));

        Assert.assertEquals("off", treatments.get("split_0"));
        Assert.assertEquals("on", treatments.get("x_feature"));
        Assert.assertEquals("red", treatments.get("my_feature"));

        Assert.assertEquals("off", results.get("split_0").treatment());
        Assert.assertEquals("{ \"size\" : 20 }", results.get("split_0").config());

        Assert.assertEquals("on", results.get("x_feature").treatment());
        Assert.assertNull(results.get("x_feature").config());

        Assert.assertEquals("red", results.get("my_feature").treatment());
        Assert.assertNull(results.get("my_feature").config());
    }

    @Test
    public void testUsingPropertiesFile() {

        Context context = InstrumentationRegistry.getInstrumentation().getContext();

        LocalhostSplitFactory factory = null;
        LocalhostSplitClient client = null;
        LocalhostSplitManager manager = null;
        try {
            factory = new LocalhostSplitFactory("key", context, "splits_test");
            client = (LocalhostSplitClient) factory.client();
            manager = (LocalhostSplitManager) factory.manager();

        } catch (IOException e) {
        }

        List<SplitView> splits = manager.splits();
        SplitView sva = manager.split("split_a");
        SplitView svb = manager.split("split_b");
        SplitView svd = manager.split("split_d");

        Assert.assertNotNull(factory);
        Assert.assertNotNull(client);
        Assert.assertNotNull(manager);

        String ta = client.getTreatment("split_a", null);
        String tb = client.getTreatment("split_b", null);
        String tc = client.getTreatment("split_c", null);
        String td = client.getTreatment("split_d", null);

        String nonExistingTreatment = client.getTreatment("nonExistingTreatment", null);

        Assert.assertEquals("on", ta);
        Assert.assertEquals("off", tb);
        Assert.assertEquals("red", tc);
        Assert.assertEquals("control", td);

        Assert.assertEquals(3, splits.size());

        Assert.assertNotNull(sva);
        Assert.assertEquals(sva.treatments.get(0) , "on");
        Assert.assertNotNull(sva.configs);
        Assert.assertEquals(0, sva.configs.size());

        Assert.assertNotNull(svb);
        Assert.assertEquals(svb.treatments.get(0) , "off");
        Assert.assertNotNull(svb.configs);
        Assert.assertEquals(0, svb.configs.size());

        Assert.assertNull(svd);
    }

    @Test
    public void testNonExistingFile() {

        Context context = InstrumentationRegistry.getInstrumentation().getContext();

        LocalhostSplitFactory factory = null;
        LocalhostSplitClient client = null;
        LocalhostSplitManager manager = null;
        try {
            factory = new LocalhostSplitFactory("key", context, "splits_test_not_found");
            client = (LocalhostSplitClient) factory.client();
            manager = (LocalhostSplitManager) factory.manager();

        } catch (IOException e) {
        }

        List<SplitView> splits = manager.splits();
        SplitView sva = manager.split("split_a");

        Assert.assertNotNull(factory);
        Assert.assertNotNull(client);
        Assert.assertNotNull(manager);

        String ta = client.getTreatment("split_a", null);
        String tb = client.getTreatment("split_b", null);
        String tc = client.getTreatment("split_c", null);
        String td = client.getTreatment("split_d", null);

        String nonExistingTreatment = client.getTreatment("nonExistingTreatment", null);

        Assert.assertEquals("control", ta);
        Assert.assertEquals("control", tb);
        Assert.assertEquals("control", tc);
        Assert.assertEquals("control", td);

        Assert.assertEquals(0, splits.size());

        Assert.assertNull(sva);
    }

    @Test
    public void testLoadYmlFile() {

        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        LocalhostSplitFactory factory = null;
        LocalhostSplitClient client = null;
        try {
            factory = new LocalhostSplitFactory("key", context, "splits_yml");
            client = (LocalhostSplitClient) factory.client();

        } catch (IOException e) {
        }

        Assert.assertNotNull(factory);
        Assert.assertNotNull(client);

        String t = client.getTreatment("split_0", null);
        Assert.assertEquals("off", t);
    }

}
