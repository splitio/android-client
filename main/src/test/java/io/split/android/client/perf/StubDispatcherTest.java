package io.split.android.client.perf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.After;
import org.junit.Test;

import java.io.IOException;

import io.split.android.client.PerfSeedSpec;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockWebServer;

public class StubDispatcherTest {

    private static final long SINCE = 1_700_000_000_000L;

    private MockWebServer mServer;
    private final OkHttpClient mClient = new OkHttpClient();

    private StubDispatcher start(long latencyMs) throws IOException {
        StubDispatcher dispatcher = new StubDispatcher(latencyMs);
        mServer = new MockWebServer();
        mServer.setDispatcher(dispatcher);
        mServer.start();
        return dispatcher;
    }

    @After
    public void stop() throws IOException {
        if (mServer != null) {
            mServer.shutdown();
        }
    }

    private Response get(String path) throws IOException {
        return mClient.newCall(new Request.Builder().url(mServer.url(path)).build()).execute();
    }

    private JsonObject getJson(String path) throws IOException {
        try (Response response = get(path)) {
            assertEquals(200, response.code());
            return JsonParser.parseString(response.body().string()).getAsJsonObject();
        }
    }

    @Test
    public void splitChangesEchoesTheRequestedSince() throws IOException {
        start(0);
        JsonObject body = getJson("/api/splitChanges/v2?s=1.3&since=" + SINCE + "&rbSince=1700000000001");
        JsonObject ff = body.getAsJsonObject("ff");
        assertEquals(SINCE, ff.get("since").getAsLong());
        assertEquals(SINCE, ff.get("till").getAsLong());
        assertEquals(0, ff.getAsJsonArray("splits").size());
        JsonObject rbs = body.getAsJsonObject("rbs");
        assertEquals(1700000000001L, rbs.get("s").getAsLong());
        assertEquals(1700000000001L, rbs.get("t").getAsLong());
        assertEquals(0, rbs.getAsJsonArray("d").size());
    }

    @Test
    public void rbsSinceDefaultsToTheFlagsSince() throws IOException {
        start(0);
        JsonObject rbs = getJson("/api/splitChanges/v2?s=1.3&since=" + SINCE).getAsJsonObject("rbs");
        assertEquals(SINCE, rbs.get("s").getAsLong());
        assertEquals(SINCE, rbs.get("t").getAsLong());
    }

    @Test
    public void firstFlagsSinceIsTheFirstRequested() throws IOException {
        StubDispatcher dispatcher = start(0);
        assertNull(dispatcher.firstFlagsSince());
        getJson("/api/splitChanges/v2?s=1.3&since=" + SINCE);
        getJson("/api/splitChanges/v2?s=1.3&since=" + (SINCE + 5));
        assertEquals(Long.valueOf(SINCE), dispatcher.firstFlagsSince());
    }

    @Test
    public void membershipsListTheSeededSegments() throws IOException {
        start(0);
        JsonObject body = getJson("/api/memberships/perf_user_1");
        JsonObject ms = body.getAsJsonObject("ms");
        assertEquals(PerfSeedSpec.MEMBERSHIPS_CN, ms.get("cn").getAsLong());
        JsonArray names = ms.getAsJsonArray("k");
        assertEquals(PerfSeedSpec.SEGMENTS.length, names.size());
        for (int i = 0; i < names.size(); i++) {
            assertEquals(PerfSeedSpec.SEGMENTS[i], names.get(i).getAsJsonObject().get("n").getAsString());
        }
        assertEquals("seg_0", names.get(0).getAsJsonObject().get("n").getAsString());
        JsonObject ls = body.getAsJsonObject("ls");
        assertEquals(PerfSeedSpec.MEMBERSHIPS_CN, ls.get("cn").getAsLong());
        assertEquals(0, ls.getAsJsonArray("k").size());
    }

    @Test
    public void authDisablesPush() throws IOException {
        start(0);
        assertEquals(false, getJson("/api/auth").get("pushEnabled").getAsBoolean());
    }

    @Test
    public void unknownPathIsAnEmpty200() throws IOException {
        start(0);
        try (Response response = get("/api/somethingElse")) {
            assertEquals(200, response.code());
            assertEquals("", response.body().string());
        }
    }

    @Test
    public void latencyDelaysEveryResponse() throws IOException {
        start(300);
        long begin = System.nanoTime();
        get("/api/auth").close();
        assertTrue(elapsedMs(begin) >= 250);
        begin = System.nanoTime();
        get("/api/somethingElse").close();
        assertTrue(elapsedMs(begin) >= 250);
    }

    @Test
    public void noLatencyDoesNotDelay() throws IOException {
        start(0);
        get("/api/auth").close(); // warm the connection
        long begin = System.nanoTime();
        get("/api/auth").close();
        assertTrue(elapsedMs(begin) < 250);
    }

    private static long elapsedMs(long beginNanos) {
        return (System.nanoTime() - beginNanos) / 1_000_000;
    }
}
