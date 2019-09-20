package fake;

import io.split.android.client.TrackClient;
import io.split.android.client.dtos.Event;

public class TrackClientStub implements TrackClient {
    @Override
    public boolean track(Event event) {
        return false;
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void close() {

    }

    @Override
    public void flush() {

    }

    @Override
    public void saveToDisk() {

    }
}
