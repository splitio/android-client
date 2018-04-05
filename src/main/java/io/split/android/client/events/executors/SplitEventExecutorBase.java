package io.split.android.client.events.executors;

import android.os.AsyncTask;

import io.split.android.client.SplitClient;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.events.SplitEventTaskMethodNotImplementedException;
import io.split.android.client.utils.Logger;

/**
 * Created by sarrubia on 4/3/18.
 */

public abstract class SplitEventExecutorBase extends SplitEventExecutorAbstract{

    public SplitEventExecutorBase(SplitEventTask task) {
        super(task);
    }

    public void execute(SplitClient sclient){

        _asyncTansk = new AsyncTask<SplitClient, Void, SplitClient>() {

            @Override
            protected SplitClient doInBackground(SplitClient... splitClients) {

                SplitClient client = splitClients[0];

                if (shouldCallOnPostExecutionMethod(client)) {
                    //BACKGROUND POST EXECUTION
                    try {
                        _task.onPostExecution(client);
                    } catch (SplitEventTaskMethodNotImplementedException e) {
                        //Method not implemented by user
                    }
                } else {
                    cancel(true);
                    Logger.d("Cancelled, executor condition has not been approved");
                }

                return client;
            }

            @Override
            protected void onPostExecute(SplitClient sclient){
                super.onPostExecute(sclient);

                //UI POST EXECUTION
                try {
                    _task.onPostExecutionView(sclient);
                } catch (SplitEventTaskMethodNotImplementedException e) {
                    //do exception stuff
                }
            }
        };

        _asyncTansk.execute(sclient);
    }

    protected abstract boolean shouldCallOnPostExecutionMethod(SplitClient client);
}
