package io.split.android.client.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Map;

public class HttpRequestImpl implements HttpRequest {

    String mUrl;
    String mBody;
    String mHttpMethod;
    Map<String, String> mHeaders;

    HttpRequestImpl(String url, String httpMethod, String body, Map<String, String> headers) {
        mUrl = url;
        mHttpMethod = httpMethod;
        mBody = body;
        mHeaders = headers;
    }

    @Override
    public HttpResponse execute() throws IOException, ProtocolException {
        return null;
    }

    private HttpResponse sendGet() throws IOException, ProtocolException {
        HttpResponse response = null;

        URL obj = new URL(mUrl);

        HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
        connection.setRequestMethod(mHttpMethod);
        addHeaders(connection);
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            String inputLine;
            StringBuffer responseData = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                responseData.append(inputLine);
            }
            in.close();

            return new HttpResponseImpl(HttpResponse.REQUEST_STATUS_OK, responseCode, (responseData != null ? responseData.toString() : null));
        }
        return new HttpResponseImpl(HttpResponse.REQUEST_STATUS_FAIL, responseCode);

    }

    private HttpResponse sendPost() throws IOException, ProtocolException {
        HttpResponse response;

        URL obj = new URL(mUrl);
        HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
        addHeaders(connection);
        connection.setRequestMethod(mHttpMethod);
        connection.setDoOutput(true);
        OutputStream os = connection.getOutputStream();
        os.write(mBody.getBytes());
        os.flush();
        os.close();

        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            String inputLine;
            StringBuffer responseData = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                responseData.append(inputLine);
            }
            in.close();

            return new HttpResponseImpl(HttpResponse.REQUEST_STATUS_OK, responseCode, (responseData != null ? responseData.toString() : null));
        }
        return new HttpResponseImpl(HttpResponse.REQUEST_STATUS_FAIL, responseCode);

    }

    void addHeaders(HttpURLConnection connection) {
        for (Map.Entry<String, String> entry : mHeaders.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }
    }
}
