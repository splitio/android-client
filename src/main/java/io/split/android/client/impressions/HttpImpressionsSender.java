package io.split.android.client.impressions;

import io.split.android.client.dtos.TestImpressions;
import io.split.android.client.utils.Utils;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import timber.log.Timber;


import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by patricioe on 6/20/16.
 */
public class HttpImpressionsSender implements ImpressionsSender {

    private CloseableHttpClient _client;
    private URI _eventsEndpoint;

    public HttpImpressionsSender(CloseableHttpClient client, String eventsEndpoint) throws URISyntaxException {
        _client = client;
        _eventsEndpoint = new URIBuilder(eventsEndpoint).setPath("/api/testImpressions/bulk").build();
    }

    @Override
    public void post(List<TestImpressions> impressions) {

        CloseableHttpResponse response = null;

        try {
            StringEntity entity = Utils.toJsonEntity(impressions);

            HttpPost request = new HttpPost(_eventsEndpoint);
            request.setEntity(entity);

            response = _client.execute(request);

            int status = response.getStatusLine().getStatusCode();

            if (status < 200 || status >= 300) {
                Timber.w("Response status was: %i", status);
            }

        } catch (Throwable t) {
            Timber.w(t, "Exception when posting impressions %s", impressions);
        } finally {
            Utils.forceClose(response);
        }

    }

}
