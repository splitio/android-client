package io.split.android.client.utils;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import java.net.URI;
import java.util.List;


public class GenericClientUtil {

    public static<T> int POST(StringEntity entity, URI endpoint, CloseableHttpClient client) {
        CloseableHttpResponse response = null;

        try {
            HttpPost request = new HttpPost(endpoint);
            request.setEntity(entity);

            response = client.execute(request);

            int status = response.getStatusLine().getStatusCode();

            if (status < 200 || status >= 300) {
                Logger.d(String.format("Posting records returned with status: %d", status));
            }

            return status;
        } catch (Throwable t) {
            Logger.d("Posting records returned with error", t);
        } finally {
            Utils.forceClose(response);
        }
        return -1;
    }

    /**
     * HTTP POST data
     *
     * @param data
     * @param endpoint
     * @param client
     * @param <T>
     * @return -1 if the method catched an exception or http status code if the request was successful
     */
    public static<T> int POST(List<T> data, URI endpoint, CloseableHttpClient client) {
        StringEntity entity = Utils.toJsonEntity(data);
        return GenericClientUtil.POST(entity, endpoint, client);
    }
}
