package tests.integration.shared;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class ServerMock {

    private final MockWebServer mWebServer = new MockWebServer();
    private final List<String> mJsonChanges;
    private int mCurSplitReqId = 1;
    CountDownLatch mLatchTrack = null;

    ServerMock(List<String> jsonChanges) {
        setupServer();
        mJsonChanges = jsonChanges;
    }

    private void setupServer() {

        final Dispatcher dispatcher = new Dispatcher() {

            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (request.getPath().contains("/mySegments/key1")) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .setBody("{\"mySegments\":[{ \"id\":\"id1\", \"name\":\"segment1\"}, { \"id\":\"id1\", \"name\":\"segment2\"}]}");
                } else if (request.getPath().contains("/mySegments/key2")) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .setBody("{\"mySegments\":[{ \"id\":\"id1\", \"name\":\"segment2\"}]}");
                } else if (request.getPath().contains("/splitChanges")) {
                    int r = mCurSplitReqId;
                    mCurSplitReqId++;
                    return new MockResponse().setResponseCode(200)
                            .setBody(splitsPerRequest(r));
                } else if (request.getPath().contains("/events/bulk")) {
                    if (mLatchTrack != null) {
                        mLatchTrack.countDown();
                    }
                    return new MockResponse().setResponseCode(200);
                } else {
                    return new MockResponse().setResponseCode(404);
                }
            }
        };
        mWebServer.setDispatcher(dispatcher);
    }

    public String getServerUrl() {
        return mWebServer.url("/").toString();
    }

    private String splitsPerRequest(int reqId) {
        int req = mJsonChanges.size() - 1;
        if (reqId < req) {
            req = reqId;
        }
        return mJsonChanges.get(req);
    }
}
