package tests.localhost;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import androidx.test.platform.app.InstrumentationRegistry;

import helper.FileHelper;
import io.split.android.client.localhost.LocalhostFileParser;
import io.split.android.client.localhost.LocalhostPropertiesFileParser;
import io.split.android.client.dtos.Split;

public class LocalhostPropertiesParserTest {
    LocalhostFileParser parser;

    @Before
    public void setup() {
        parser = new LocalhostPropertiesFileParser();
    }

    @Test
    public void testFile() {
        String content = new FileHelper().loadFileContent(
                InstrumentationRegistry.getInstrumentation().getContext(),
                "splits1.properties");
        Map<String, Split> splits = parser.parse(content);
        Split split1 = splits.get("split1");
        Split split2 = splits.get("split2");
        Split split3 = splits.get("split3");

        Assert.assertNotNull(split1);
        Assert.assertNotNull(split2);
        Assert.assertNotNull(split3);

        Assert.assertEquals("split1", split1.name);
        Assert.assertEquals("on", split1.defaultTreatment);

        Assert.assertEquals("split2", split2.name);
        Assert.assertEquals("off", split2.defaultTreatment);

        Assert.assertEquals("split3", split3.name);
        Assert.assertEquals("u1", split3.defaultTreatment);
    }
}
