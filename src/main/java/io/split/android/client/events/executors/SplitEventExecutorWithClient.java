package io.split.android.client.events.executors;

import android.os.AsyncTask;

import static com.google.common.base.Preconditions.checkNotNull;

import io.split.android.client.SplitClient;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.events.SplitEventTaskMethodNotImplementedException;

/**
 * Created by sarrubia on 4/3/18.
 */

public class SplitEventExecutorWithClient extends SplitEventExecutorAbstract {

    private final SplitClient mSplitClient;

    public SplitEventExecutorWithClient(SplitEventTask task, SplitClient client) {

        super(task);
        checkNotNull(task);
        mSplitClient = checkNotNull(client);
    }

    public void execute(){

        mAsyncTask = new AsyncTask<SplitClient, Void, SplitClient>() {

            @Override
            protected SplitClient doInBackground(SplitClient... splitClients) {

                if (splitClients.length > 0) {

                    SplitClient client = checkNotNull(splitClients[0]);

                    //BACKGROUND POST EXECUTION
                    try {
                        mTask.onPostExecution(client);
                    } catch (Exception e) {
                        //Method not implemented by user
                    }

                    return client;
                }
                return null;
            }

            @Override
            protected void onPostExecute(SplitClient sclient){

                checkNotNull(sclient);

                //UI POST EXECUTION
                try {
                    mTask.onPostExecutionView(sclient);
                } catch (Exception e) {
                    //Method not implemented by user
                }
            }
        };

        mAsyncTask.execute(mSplitClient);
    }
}
