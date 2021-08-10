package tests.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import helper.TestingData;
import io.split.android.client.exceptions.MySegmentsParsingException;
import io.split.android.client.service.sseclient.notifications.KeyList;
import io.split.android.client.service.sseclient.notifications.KeyListParser;
import io.split.android.client.utils.Gzip;
import io.split.android.client.utils.Zlib;

public class KeyListParserTest {

    KeyListParser mParser;
    Gzip mGzip;
    Zlib mZlib;

    @Before
    public void setup() {
        mParser = new KeyListParser();
        mGzip = new Gzip();
        mZlib = new Zlib();
    }

    @Test
    public void testZlibPayload() throws MySegmentsParsingException {

        KeyList keyList = mParser.parse(TestingData.encodedKeyListPayload(), mGzip);

        Set added = new HashSet<String>(keyList.getAdded());
        Set removed = new HashSet<String>(keyList.getRemoved());

        Assert.assertEquals(2, keyList.getAdded().size());
        Assert.assertEquals(2, keyList.getRemoved().size());
        Assert.assertTrue(added.contains("1573573083296714675"));
        Assert.assertTrue(added.contains("8482869187405483569"));
        Assert.assertTrue(removed.contains("8031872927333060586"));
        Assert.assertTrue(removed.contains("6829471020522910836"));
    }
}
