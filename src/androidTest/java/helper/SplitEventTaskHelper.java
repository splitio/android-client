package helper;

import java.util.concurrent.CountDownLatch;

import io.split.android.client.SplitClient;
import io.split.android.client.events.SplitEventTask;

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
        if(mlatch != null) {
            mlatch.countDown();
        }
    }

    @Override
    public void onPostExecutionView(SplitClient client) {

    }
};
