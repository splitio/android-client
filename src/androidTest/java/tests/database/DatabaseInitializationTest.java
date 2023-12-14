package tests.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingDeque;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import fake.HttpResponseMockDispatcher;
import fake.HttpStreamResponseMock;
import helper.IntegrationHelper;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.api.Key;
import io.split.android.client.network.HttpMethod;

public class DatabaseInitializationTest {

    private HttpClientMock mHttpClientMock;
    private Context mContext;

    @Before
    public void setUp() throws IOException {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mHttpClientMock = new HttpClientMock(new HttpResponseMockDispatcher() {
            @Override
            public HttpResponseMock getResponse(URI uri, HttpMethod method, String body) {
                return new HttpResponseMock(400, "");
            }

            @Override
            public HttpStreamResponseMock getStreamResponse(URI uri) {
                try {
                    return new HttpStreamResponseMock(400, new LinkedBlockingDeque<>(0));
                } catch (IOException e) {
                    return null;
                }
            }
        });
    }

    @Test
    public void initializationWithNullApiKeyResultsInNullFactory() throws IOException {
        SplitFactory factory = IntegrationHelper.buildFactory(null,
                new Key("matchingKey"),
                IntegrationHelper.basicConfig(),
                mContext,
                mHttpClientMock);

        assertNull(factory);
    }

    @Test
    public void initializationWithoutPrefixCreatesCorrectDatabaseName() {
        String[] initialDatabaseList = getDbList(mContext);
        SplitFactory factory = IntegrationHelper.buildFactory("abcdefghijkl",
                new Key("matchingKey"),
                IntegrationHelper.basicConfig(),
                mContext,
                mHttpClientMock);

        String[] finalDatabaseList = getDbList(mContext);

        assertNotNull(factory);
        assertEquals(0, initialDatabaseList.length);
        assertEquals("abcdijkl", finalDatabaseList[0]);
    }

    @Test
    public void initializationWithPrefixCreatesCorrectDatabaseName() {
        String[] initialDatabaseList = getDbList(mContext);
        SplitFactory factory = IntegrationHelper.buildFactory("abcdefghijkl",
                new Key("matchingKey"),
                SplitClientConfig.builder().prefix("my_prefix").build(),
                mContext,
                mHttpClientMock);

        String[] finalDatabaseList = getDbList(mContext);

        assertNotNull(factory);
        assertEquals(0, initialDatabaseList.length);
        assertEquals("my_prefixabcdijkl", finalDatabaseList[0]);
    }

    @Test
    public void factoriesWithSameSdkKeyCreateOnlyOneDatabase() {
        String[] initialDatabaseList = getDbList(mContext);
        SplitFactory factory1 = IntegrationHelper.buildFactory("abcdefghijkl",
                new Key("matchingKey"),
                IntegrationHelper.basicConfig(),
                mContext,
                mHttpClientMock);

        SplitFactory factory2 = IntegrationHelper.buildFactory("abcdefghijkl",
                new Key("matchingKey2"),
                IntegrationHelper.basicConfig(),
                mContext,
                mHttpClientMock);

        String[] finalDatabaseList = getDbList(mContext);

        assertNotNull(factory1);
        assertNotNull(factory2);
        assertEquals(0, initialDatabaseList.length);
        assertEquals(1, finalDatabaseList.length);
        assertEquals("abcdijkl", finalDatabaseList[0]);
    }

    @Test
    public void oneFactoryWithPrefixCreatesNewDatabase() throws InterruptedException {
        String[] initialDatabaseList = getDbList(mContext);
        SplitFactory factory1 = IntegrationHelper.buildFactory("abcdefghijkl",
                new Key("matchingKey"),
                SplitClientConfig.builder().build(),
                mContext,
                mHttpClientMock);

        SplitFactory factory2 = IntegrationHelper.buildFactory("abcdefghijkl",
                new Key("matchingKey"),
                SplitClientConfig.builder().prefix("my_prefix").build(),
                mContext,
                mHttpClientMock);

        Thread.sleep(200);
        String[] finalDatabaseList = getDbList(mContext);

        assertNotNull(factory1);
        assertNotNull(factory2);
        assertEquals(0, initialDatabaseList.length);
        assertEquals(2, finalDatabaseList.length);
        assertEquals("abcdijkl", finalDatabaseList[0]);
        assertEquals("my_prefixabcdijkl", finalDatabaseList[1]);
    }

    @Test
    public void usingInvalidPrefixResultsInIgnoredPrefix() {
        String[] initialDatabaseList = getDbList(mContext);

        SplitFactory factory = IntegrationHelper.buildFactory("abcdefghijkl",
                new Key("matchingKey"),
                SplitClientConfig.builder().prefix(":l").build(),
                mContext,
                mHttpClientMock);

        String[] finalDatabaseList = getDbList(mContext);

        assertNotNull(factory);
        assertEquals(0, initialDatabaseList.length);
        assertEquals(1, finalDatabaseList.length);
        assertEquals("abcdijkl", finalDatabaseList[0]);
    }

    @Test
    public void usingNullPrefixResultsInIgnoredPrefix() {
        String[] initialDatabaseList = getDbList(mContext);

        SplitFactory factory = IntegrationHelper.buildFactory("abcdefghijkl",
                new Key("matchingKey"),
                SplitClientConfig.builder().prefix(null).build(),
                mContext,
                mHttpClientMock);

        String[] finalDatabaseList = getDbList(mContext);

        assertNotNull(factory);
        assertEquals(0, initialDatabaseList.length);
        assertEquals(1, finalDatabaseList.length);
        assertEquals("abcdijkl", finalDatabaseList[0]);
    }

    private static String[] getDbList(Context context) {
        // remove -journal dbs since we're not interested in them
        return Arrays.stream(context.databaseList()).filter(db -> !db.endsWith("-journal")).toArray(String[]::new);
    }
}
