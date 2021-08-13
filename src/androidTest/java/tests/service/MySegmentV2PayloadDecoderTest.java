package tests.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import helper.TestingData;
import io.split.android.client.exceptions.MySegmentsParsingException;
import io.split.android.client.service.sseclient.notifications.KeyList;
import io.split.android.client.service.sseclient.notifications.MySegmentsV2PayloadDecoder;
import io.split.android.client.service.sseclient.notifications.NotificationParser;
import io.split.android.client.utils.Gzip;
import io.split.android.client.utils.MurmurHash3;
import io.split.android.client.utils.Zlib;

public class MySegmentV2PayloadDecoderTest {

    MySegmentsV2PayloadDecoder mDecoder;
    NotificationParser mParser;
    Gzip mGzip;
    Zlib mZlib;

    @Before
    public void setup() {
        mDecoder = new MySegmentsV2PayloadDecoder();
        mParser = new NotificationParser();
        mGzip = new Gzip();
        mZlib = new Zlib();
    }

    @Test
    public void keyListGzipPayload() throws MySegmentsParsingException {

        String payload = mDecoder.decodeAsString(TestingData.encodedKeyListPayloadGzip(), mGzip);
        KeyList keyList = mParser.parseKeyList(payload);

        Set added = new HashSet<String>(keyList.getAdded());
        Set removed = new HashSet<String>(keyList.getRemoved());

        Assert.assertEquals(2, keyList.getAdded().size());
        Assert.assertEquals(2, keyList.getRemoved().size());
        Assert.assertTrue(added.contains("1573573083296714675"));
        Assert.assertTrue(added.contains("8482869187405483569"));
        Assert.assertTrue(removed.contains("8031872927333060586"));
        Assert.assertTrue(removed.contains("6829471020522910836"));
    }

    @Test
    public void boundedGzipPayload() throws MySegmentsParsingException {

        byte[] payloadGzip = mDecoder.decodeAsBytes(TestingData.encodedBoundedPayloadGzip(), mGzip);
        List<String> keys = new ArrayList<>();
        keys.add("603516ce-1243-400b-b919-0dce5d8aecfd");
        keys.add("88f8b33b-f858-4aea-bea2-a5f066bab3ce");
        keys.add("375903c8-6f62-4272-88f1-f8bcd304c7ae");
        keys.add("18c936ad-0cd2-490d-8663-03eaa23a5ef1");
        keys.add("bfd4a824-0cde-4f11-9700-2b4c5ad6f719");
        keys.add("4588c4f6-3d18-452a-bc4a-47d7abfd23df");
        keys.add("42bcfe02-d268-472f-8ed5-e6341c33b4f7");
        keys.add("2a7cae0e-85a2-443e-9d7c-7157b7c5960a");
        keys.add("4b0b0467-3fe1-43d1-a3d5-937c0a5473b1");
        keys.add("09025e90-d396-433a-9292-acef23cf0ad1");

        Map<String, Boolean> results = new HashMap<>();
        for (String key : keys) {
            BigInteger hashedKey = mDecoder.hashKey(key);
            int index = mDecoder.computeKeyIndex(hashedKey, payloadGzip.length);
            results.put(key, mDecoder.isKeyInBitmap(payloadGzip, index));
        }

        for (String key : keys) {
            Assert.assertTrue(results.get(key));
        }
    }
}
