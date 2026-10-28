package io.split.android.client.perf;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.concurrent.TimeUnit;

import io.split.android.client.PerfSeedSpec;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Answers the SDK's polling requests with "nothing changed": the flags and rule-based segments
 * come back empty at the change number that was asked for, and the memberships are the seeded
 * ones. Push is reported as disabled. Every response is delayed by {@code latencyMs}.
 */
public class StubDispatcher extends Dispatcher {

    private static final String SPLIT_CHANGES = "/splitChanges";
    private static final String MEMBERSHIPS = "/memberships";
    private static final String AUTH = "/auth";

    private final long mLatencyMs;
    private Long mFirstFlagsSince;

    public StubDispatcher(long latencyMs) {
        mLatencyMs = latencyMs;
    }

    /** The since of the first flags request, or null when none arrived. */
    public synchronized Long firstFlagsSince() {
        return mFirstFlagsSince;
    }

    @Override
    public MockResponse dispatch(RecordedRequest request) {
        HttpUrl url = request.getRequestUrl();
        String path = url == null ? "" : url.encodedPath();
        MockResponse response = new MockResponse().setResponseCode(200);
        if (path.contains(SPLIT_CHANGES)) {
            response.setBody(splitChanges(url));
        } else if (path.contains(MEMBERSHIPS)) {
            response.setBody(memberships());
        } else if (path.endsWith(AUTH)) {
            response.setBody("{\"pushEnabled\": false }");
        }
        if (mLatencyMs > 0) {
            // Headers rather than body: a response without a body would otherwise not be delayed.
            response.setHeadersDelay(mLatencyMs, TimeUnit.MILLISECONDS);
        }
        return response;
    }

    private String splitChanges(HttpUrl url) {
        long since = longParameter(url, "since", -1);
        long rbSince = longParameter(url, "rbSince", since);
        synchronized (this) {
            if (mFirstFlagsSince == null) {
                mFirstFlagsSince = since;
            }
        }
        return "{\"ff\":{\"splits\":[],\"since\":" + since + ",\"till\":" + since + "},"
                + "\"rbs\":{\"d\":[],\"s\":" + rbSince + ",\"t\":" + rbSince + "}}";
    }

    private static long longParameter(HttpUrl url, String name, long fallback) {
        String value = url.queryParameter(name);
        return value == null ? fallback : Long.parseLong(value);
    }

    private static String memberships() {
        JsonArray names = new JsonArray();
        for (String segment : PerfSeedSpec.SEGMENTS) {
            JsonObject entry = new JsonObject();
            entry.addProperty("n", segment);
            names.add(entry);
        }
        JsonObject ms = new JsonObject();
        ms.add("k", names);
        ms.addProperty("cn", PerfSeedSpec.MEMBERSHIPS_CN);
        JsonObject ls = new JsonObject();
        ls.add("k", new JsonArray());
        ls.addProperty("cn", PerfSeedSpec.MEMBERSHIPS_CN);
        JsonObject body = new JsonObject();
        body.add("ms", ms);
        body.add("ls", ls);
        return body.toString();
    }
}
