package io.split.android.client.events;

import io.split.android.client.SplitClient;

/**
 * Created by sarrubia on 3/26/18.
 */

public class SplitEventTask {

    public void onPostExecution(SplitClient client){throw new SplitEventTaskMethodNotImplementedException();}

    public void onPostExecutionView(SplitClient client){throw new SplitEventTaskMethodNotImplementedException();}
}
