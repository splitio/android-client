package io.split.sharedtest.helper;

import java.util.concurrent.CountDownLatch;

import io.split.android.client.SplitClient;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.utils.logger.Logger;

public class SplitEventTaskHelper extends SplitEventTask {

    private CountDownLatch mlatch;
    public boolean isOnPostExecutionCalled = false;
    public SplitEventTaskHelper(CountDownLatch latch) {
        mlatch = latch;
    }

    public SplitEventTaskHelper() {
    }


    @Override
    public void onPostExecution(SplitClient client) {
        isOnPostExecutionCalled = true;
        Logger.d("TASK ON POST " + isOnPostExecutionCalled);
        if(mlatch != null) {
            mlatch.countDown();
        }
    }

    @Override
    public void onPostExecutionView(SplitClient client) {

    }

    public void setLatch(CountDownLatch latch) {
        mlatch = latch;
    }
}
