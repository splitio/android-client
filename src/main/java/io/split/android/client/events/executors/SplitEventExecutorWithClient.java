package io.split.android.client.events.executors;

import android.os.AsyncTask;

import io.split.android.client.SplitClient;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.events.SplitEventTaskMethodNotImplementedException;
import io.split.android.client.utils.Logger;

/**
 * Created by sarrubia on 4/3/18.
 */

public class SplitEventExecutorWithClient extends SplitEventExecutorAbstract{

    private SplitClient _sclient;

    public SplitEventExecutorWithClient(SplitEventTask task, SplitClient client) {
        super(task);
        _sclient = client;
    }

    public void execute(){

        _asyncTansk = new AsyncTask<SplitClient, Void, SplitClient>() {

            @Override
            protected SplitClient doInBackground(SplitClient... splitClients) {

                SplitClient client = splitClients[0];

                //BACKGROUND POST EXECUTION
                try {
                    _task.onPostExecution(client);
                } catch (SplitEventTaskMethodNotImplementedException e) {
                    //Method not implemented by user
                }

                return client;
            }

            @Override
            protected void onPostExecute(SplitClient sclient){


                //UI POST EXECUTION
                try {
                    _task.onPostExecutionView(sclient);
                } catch (SplitEventTaskMethodNotImplementedException e) {
                    //Method not implemented by user
                }
            }
        };

        _asyncTansk.execute(_sclient);
    }
}
