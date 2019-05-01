package Localhost;

import android.content.Context;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import androidx.test.InstrumentationRegistry;
import io.split.android.client.Localhost.LocalhostFileParser;
import io.split.android.client.Localhost.LocalhostPropertiesFileParser;
import io.split.android.client.dtos.Split;

public class LocalhostPropertiesParserTest {
    LocalhostFileParser parser;

    @Before
    public void setup() {
        parser = new LocalhostPropertiesFileParser(InstrumentationRegistry.getTargetContext());
    }

    @Test
    public void testFile() {
        Map<String, Split> splits = parser.parse("splits1.properties");
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
