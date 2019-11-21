package io.split.android.client.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.MySegment;
import io.split.android.client.service.mysegments.MySegmentsFetcherV2;
import io.split.android.client.service.mysegments.MySegmentsSyncTask;
import io.split.android.client.storage.mysegments.MySegmentsStorage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MySegmentsSyncTaskTest {
    @Mock
    MySegmentsFetcherV2 mMySegmentsFetcher;
    @Mock
    MySegmentsStorage mySegmentsStorage;

    List<MySegment> mMySegments = null;

    @InjectMocks
    MySegmentsSyncTask mTask;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        loadMySegments();
    }

    @Test
    public void correctExecution() {
        when(mMySegmentsFetcher.execute()).thenReturn(mMySegments);

        mTask.execute();

        verify(mMySegmentsFetcher, times(1)).execute();
        verify(mySegmentsStorage, times(1)).set(any());
    }

    @Test
    public void fetcherException() {
        when(mMySegmentsFetcher.execute()).thenThrow(IllegalStateException.class);

        mTask.execute();

        verify(mMySegmentsFetcher, times(1)).execute();
        verify(mySegmentsStorage, never()).set(any());
    }

    @Test
    public void storageException() {
        when(mMySegmentsFetcher.execute()).thenReturn(mMySegments);
        doThrow(NullPointerException.class).when(mySegmentsStorage).set(any());

        mTask.execute();

        verify(mMySegmentsFetcher, times(1)).execute();
        verify(mySegmentsStorage, times(1)).set(any());
    }

    @After
    public void tearDown() {
        reset(mMySegmentsFetcher);
        reset(mySegmentsStorage);
    }

    private void loadMySegments() {
        if (mMySegments == null) {
            mMySegments = new ArrayList<>();
            for(int i=0; i<5; i++) {
                MySegment s = new MySegment();
                s.id = "id_" + i;
                s.id = "segment_" + i;
                mMySegments.add(s);
            }
        }
    }
}
