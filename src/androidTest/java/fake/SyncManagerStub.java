package fake;

import io.split.android.client.dtos.Event;
import io.split.android.client.impressions.Impression;
import io.split.android.client.service.synchronizer.SyncManager;

public class SyncManagerStub implements SyncManager {

    public boolean resumeCalled = false;
    public boolean pauseCalled = false;

    @Override
    public void start() {
    }

    @Override
    public void pause() {
        pauseCalled = true;
    }

    @Override
    public void resume() {
        resumeCalled = true;
    }

    @Override
    public void stop() {
    }

    @Override
    public void flush() {
    }

    @Override
    public void pushEvent(Event event) {
    }

    @Override
    public void pushImpression(Impression impression) {
    }
}
