package io.split.android.client.events.executors;

import android.os.AsyncTask;

import static com.google.common.base.Preconditions.checkNotNull;

import io.split.android.client.SplitClient;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.events.SplitEventTaskMethodNotImplementedException;

/**
 * Created by sarrubia on 4/3/18.
 */

public class SplitEventExecutorWithClient extends SplitEventExecutorAbstract{

    private SplitClient _sclient;

    public SplitEventExecutorWithClient(SplitEventTask task, SplitClient client) {

        super(task);
        checkNotNull(task);
        _sclient = checkNotNull(client);
    }

    public void execute(){

        _asyncTansk = new AsyncTask<SplitClient, Void, SplitClient>() {

            @Override
            protected SplitClient doInBackground(SplitClient... splitClients) {

                if (splitClients.length > 0) {

                    SplitClient client = checkNotNull(splitClients[0]);

                    //BACKGROUND POST EXECUTION
                    try {
                        _task.onPostExecution(client);
                    } catch (SplitEventTaskMethodNotImplementedException e) {
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
                    _task.onPostExecutionView(sclient);
                } catch (SplitEventTaskMethodNotImplementedException e) {
                    //Method not implemented by user
                }
            }
        };

        _asyncTansk.execute(_sclient);
    }
}
