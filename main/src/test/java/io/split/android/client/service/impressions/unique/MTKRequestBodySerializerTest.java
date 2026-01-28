package io.split.android.client.service.impressions.unique;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MTKRequestBodySerializerTest {

    private MTKRequestBodySerializer mMTKRequestBodySerializer;

    @Before
    public void setUp() {
        mMTKRequestBodySerializer = new MTKRequestBodySerializer();
    }

    @Test
    public void serialize() {

        String expectedString = "{\"keys\":[{\"k\":\"key1\",\"fs\":[\"features@_1\",\"features-1\",\"features,1\"]},{\"k\":\"key2\",\"fs\":[\"features_00\",\"features+00\",\"FEATURES\"]}]}";

        Set<String> key1Set = new HashSet<>();
        key1Set.add("features,1");
        key1Set.add("features-1");
        key1Set.add("features@_1");

        Set<String> key2Set = new HashSet<>();
        key2Set.add("features+00");
        key2Set.add("features_00");
        key2Set.add("FEATURES");

        MTK mtk = new MTK(Arrays.asList(new UniqueKey("key1", key1Set), new UniqueKey("key2", key2Set)));

        String serializedString = mMTKRequestBodySerializer.serialize(mtk);

        assertEquals(expectedString, serializedString);
    }
}
