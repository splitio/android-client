package io.split.android.client.events;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import io.split.android.client.SplitClient;

public class SdkReadyFromCacheEventTaskTest {

    @Test
    public void extendsFromSplitEventTask() {
        SdkReadyFromCacheEventTask task = new SdkReadyFromCacheEventTask() {
            @Override
            public void onPostExecution(SplitClient client, SdkReadyFromCacheMetadata metadata) {
                // no-op
            }
        };

        assertTrue(task instanceof SplitEventTask);
    }

    @Test
    public void defaultImplementationThrowsExceptionForTypedMethods() {
        SdkReadyFromCacheEventTask task = new SdkReadyFromCacheEventTask() {};

        boolean threwException = false;
        try {
            task.onPostExecution(mock(SplitClient.class), new SdkReadyFromCacheMetadata(null, null));
        } catch (SplitEventTaskMethodNotImplementedException e) {
            threwException = true;
        }

        assertTrue(threwException);
    }

    @Test
    public void defaultImplementationThrowsExceptionForTypedViewMethods() {
        SdkReadyFromCacheEventTask task = new SdkReadyFromCacheEventTask() {};

        boolean threwException = false;
        try {
            task.onPostExecutionView(mock(SplitClient.class), new SdkReadyFromCacheMetadata(null, null));
        } catch (SplitEventTaskMethodNotImplementedException e) {
            threwException = true;
        }

        assertTrue(threwException);
    }
}
