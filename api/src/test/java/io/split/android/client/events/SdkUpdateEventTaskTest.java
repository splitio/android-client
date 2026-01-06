package io.split.android.client.events;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import io.split.android.client.SplitClient;

public class SdkUpdateEventTaskTest {

    @Test
    public void extendsFromSplitEventTask() {
        SdkUpdateEventTask task = new SdkUpdateEventTask() {
            @Override
            public void onPostExecution(SplitClient client, SdkUpdateMetadata metadata) {
                // no-op
            }
        };

        assertTrue(task instanceof SplitEventTask);
    }

    @Test
    public void defaultImplementationThrowsExceptionForTypedMethods() {
        SdkUpdateEventTask task = new SdkUpdateEventTask() {};

        boolean threwException = false;
        try {
            task.onPostExecution(mock(SplitClient.class), new SdkUpdateMetadata(null));
        } catch (SplitEventTaskMethodNotImplementedException e) {
            threwException = true;
        }

        assertTrue(threwException);
    }

    @Test
    public void defaultImplementationThrowsExceptionForTypedViewMethods() {
        SdkUpdateEventTask task = new SdkUpdateEventTask() {};

        boolean threwException = false;
        try {
            task.onPostExecutionView(mock(SplitClient.class), new SdkUpdateMetadata(null));
        } catch (SplitEventTaskMethodNotImplementedException e) {
            threwException = true;
        }

        assertTrue(threwException);
    }
}
