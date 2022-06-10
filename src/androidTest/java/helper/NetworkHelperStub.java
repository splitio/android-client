package helper;

import java.net.URI;

import io.split.android.client.utils.NetworkHelper;

public class NetworkHelperStub implements NetworkHelper {
    @Override
    public boolean isReachable(URI target) {
        return true;
    }
}
