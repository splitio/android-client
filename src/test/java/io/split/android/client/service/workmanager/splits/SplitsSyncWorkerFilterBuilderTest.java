package io.split.android.client.service.workmanager.splits;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;

import org.junit.Test;
import org.mockito.MockedStatic;

import java.util.Arrays;

import io.split.android.client.SplitFilter;

public class SplitsSyncWorkerFilterBuilderTest {

    @Test
    public void nullFilterTypeReturnsNullSplitFilter() {
        SplitFilter splitFilter = SplitsSyncWorkerFilterBuilder.buildFilter(null, new String[0]);

        assertNull(splitFilter);
    }

    @Test
    public void byNameFilterTypeReturnsByNameSplitFilter() {
        SplitFilter splitFilter = SplitsSyncWorkerFilterBuilder.buildFilter(SplitFilter.Type.BY_NAME.queryStringField(), new String[0]);

        assertSame(splitFilter.getType(), SplitFilter.Type.BY_NAME);
    }

    @Test
    public void bySetFilterTypeReturnsBySetSplitFilter() {
        SplitFilter splitFilter = SplitsSyncWorkerFilterBuilder.buildFilter(SplitFilter.Type.BY_SET.queryStringField(), new String[0]);

        assertSame(splitFilter.getType(), SplitFilter.Type.BY_SET);
    }

    @Test
    public void byNameFilterIsBuiltWithConfiguredValues() {
        String[] filterValuesArray = new String[]{"filter1", "filter2"};
        SplitFilter splitFilter = SplitsSyncWorkerFilterBuilder.buildFilter(SplitFilter.Type.BY_NAME.queryStringField(), filterValuesArray);

        assertEquals(2, splitFilter.getValues().size());
        assertTrue(splitFilter.getValues().contains("filter1"));
        assertTrue(splitFilter.getValues().contains("filter2"));
    }

    @Test
    public void bySetFilterIsBuiltWithConfiguredValues() {
        String[] filterValuesArray = new String[]{"filter1", "filter2"};
        SplitFilter splitFilter = SplitsSyncWorkerFilterBuilder.buildFilter(SplitFilter.Type.BY_SET.queryStringField(), filterValuesArray);

        assertEquals(2, splitFilter.getValues().size());
        assertTrue(splitFilter.getValues().contains("filter1"));
        assertTrue(splitFilter.getValues().contains("filter2"));
    }

    @Test
    public void byNameFilterIsBuiltWithStaticMethod() {
        try (MockedStatic<SplitFilter> mockedStatic = mockStatic(SplitFilter.class)) {
            String[] filterValuesArray = new String[]{"filter1", "filter2"};
            SplitsSyncWorkerFilterBuilder.buildFilter(SplitFilter.Type.BY_NAME.queryStringField(), filterValuesArray);
            mockedStatic.verify(() -> SplitFilter.byName(Arrays.asList(filterValuesArray)));
        }
    }

    @Test
    public void bySetFilterIsBuiltWithStaticMethod() {
        try (MockedStatic<SplitFilter> mockedStatic = mockStatic(SplitFilter.class)) {
            String[] filterValuesArray = new String[]{"filter1", "filter2"};
            SplitsSyncWorkerFilterBuilder.buildFilter(SplitFilter.Type.BY_SET.queryStringField(), filterValuesArray);
            mockedStatic.verify(() -> SplitFilter.bySet(Arrays.asList(filterValuesArray)));
        }
    }
}
