package io.split.android.client.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SdkUpdateMetadataTest {

    @Test
    public void getUpdatedFlagsReturnsNullWhenConstructedWithNull() {
        SdkUpdateMetadata metadata = new SdkUpdateMetadata(null);

        assertNull(metadata.getUpdatedFlags());
    }

    @Test
    public void getUpdatedFlagsReturnsEmptyListWhenConstructedWithEmptyList() {
        SdkUpdateMetadata metadata = new SdkUpdateMetadata(Collections.emptyList());

        assertEquals(Collections.emptyList(), metadata.getUpdatedFlags());
    }

    @Test
    public void getUpdatedFlagsReturnsListWhenConstructedWithList() {
        List<String> flags = Arrays.asList("flag1", "flag2", "flag3");
        SdkUpdateMetadata metadata = new SdkUpdateMetadata(flags);

        assertEquals(flags, metadata.getUpdatedFlags());
    }

    @Test
    public void getUpdatedFlagsReturnsSingleItemList() {
        List<String> flags = Collections.singletonList("singleFlag");
        SdkUpdateMetadata metadata = new SdkUpdateMetadata(flags);

        assertEquals(flags, metadata.getUpdatedFlags());
        assertEquals(1, metadata.getUpdatedFlags().size());
        assertEquals("singleFlag", metadata.getUpdatedFlags().get(0));
    }
}

