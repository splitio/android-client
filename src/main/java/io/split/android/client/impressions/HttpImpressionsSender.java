package io.split.android.client.impressions;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import io.split.android.client.dtos.TestImpressions;
import io.split.android.client.utils.Utils;
import timber.log.Timber;

/**
 * Created by patricioe on 6/20/16.
 */
public class HttpImpressionsSender implements ImpressionsSender {

    private CloseableHttpClient _client;
    private URI _eventsEndpoint;
    private ImpressionsStorageManager _storageManager;

    public HttpImpressionsSender(CloseableHttpClient client, String eventsEndpoint, ImpressionsStorageManager impressionsStorageManager) throws URISyntaxException {
        _client = client;
        _eventsEndpoint = new URIBuilder(eventsEndpoint).setPath("/api/testImpressions/bulk").build();
        _storageManager = impressionsStorageManager;
    }

    @Override
    public void post(List<TestImpressions> impressions) {

        if (!Utils.isReachable(_eventsEndpoint)) {
            Timber.d("%s is NOT REACHABLE. Avoid trying to send impressions this time.", _eventsEndpoint.getHost());
            return;
        }

        synchronized (this) {
            String[] chunkNames = _storageManager.getAllChunkNames();
            Timber.i("Posting %d Split impressions", chunkNames.length);

            for (String chunkId :
                    chunkNames) {
                try {
                    String json = _storageManager.readStringChunk(chunkId);

                    StringEntity entity = new StringEntity(json, "UTF-8");
                    entity.setContentType("application/json");

                    if (post(entity)) {
                        _storageManager.chunkSucceeded(chunkId);
                    } else {
                        _storageManager.chunkFailed(chunkId);
                    }

                } catch (UnsupportedEncodingException e) {
                    Timber.e(e);
                    _storageManager.chunkFailed(chunkId);
                }
            }
        }
    }

    private boolean post(StringEntity entity) {

        CloseableHttpResponse response = null;

        try {

            HttpPost request = new HttpPost(_eventsEndpoint);
            request.setEntity(entity);

            response = _client.execute(request);

            int status = response.getStatusLine().getStatusCode();
            String reason = response.getStatusLine().getReasonPhrase();
            if (status < 200 || status >= 300) {
                Timber.w("Response status was: %d. Reason: %s", status, reason);
                return false;
            }
            Timber.i("Entity sent: %s", entity);

            return true;
        } catch (Throwable t) {
            Timber.w(t, "Exception when posting impressions %s", entity);
            return false;
        } finally {
            Utils.forceClose(response);
        }

    }

}
