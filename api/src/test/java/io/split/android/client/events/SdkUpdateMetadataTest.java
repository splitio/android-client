package io.split.android.client.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SdkUpdateMetadataTest {

    @Test
    public void getNamesReturnsNullWhenConstructedWithNull() {
        SdkUpdateMetadata metadata = new SdkUpdateMetadata(null, null);

        assertNull(metadata.getNames());
    }

    @Test
    public void getNamesReturnsEmptyListWhenConstructedWithEmptyList() {
        SdkUpdateMetadata metadata = new SdkUpdateMetadata(null, Collections.emptyList());

        assertEquals(Collections.emptyList(), metadata.getNames());
    }

    @Test
    public void getNamesReturnsListWhenConstructedWithList() {
        List<String> names = Arrays.asList("flag1", "flag2", "flag3");
        SdkUpdateMetadata metadata = new SdkUpdateMetadata(SdkUpdateMetadata.Type.FLAGS_UPDATE, names);

        assertEquals(names, metadata.getNames());
    }

    @Test
    public void getNamesReturnsSingleItemList() {
        List<String> names = Collections.singletonList("singleFlag");
        SdkUpdateMetadata metadata = new SdkUpdateMetadata(SdkUpdateMetadata.Type.FLAGS_UPDATE, names);

        assertEquals(names, metadata.getNames());
        assertEquals(1, metadata.getNames().size());
        assertEquals("singleFlag", metadata.getNames().get(0));
    }

    @Test
    public void getTypeReturnsNullWhenConstructedWithNull() {
        SdkUpdateMetadata metadata = new SdkUpdateMetadata(null, null);

        assertNull(metadata.getType());
    }

    @Test
    public void getTypeReturnsFlagsUpdateWhenConstructedWithFlagsUpdate() {
        SdkUpdateMetadata metadata = new SdkUpdateMetadata(SdkUpdateMetadata.Type.FLAGS_UPDATE, null);

        assertEquals(SdkUpdateMetadata.Type.FLAGS_UPDATE, metadata.getType());
    }

    @Test
    public void getTypeReturnsSegmentsUpdateWhenConstructedWithSegmentsUpdate() {
        SdkUpdateMetadata metadata = new SdkUpdateMetadata(SdkUpdateMetadata.Type.SEGMENTS_UPDATE, null);

        assertEquals(SdkUpdateMetadata.Type.SEGMENTS_UPDATE, metadata.getType());
    }

    @Test
    public void flagsUpdateMetadataContainsBothTypeAndNames() {
        List<String> flags = Arrays.asList("flag1", "flag2");
        SdkUpdateMetadata metadata = new SdkUpdateMetadata(SdkUpdateMetadata.Type.FLAGS_UPDATE, flags);

        assertEquals(SdkUpdateMetadata.Type.FLAGS_UPDATE, metadata.getType());
        assertEquals(flags, metadata.getNames());
    }

    @Test
    public void segmentsUpdateMetadataContainsBothTypeAndNames() {
        List<String> segments = Arrays.asList("segment1", "segment2");
        SdkUpdateMetadata metadata = new SdkUpdateMetadata(SdkUpdateMetadata.Type.SEGMENTS_UPDATE, segments);

        assertEquals(SdkUpdateMetadata.Type.SEGMENTS_UPDATE, metadata.getType());
        assertEquals(segments, metadata.getNames());
    }
}
