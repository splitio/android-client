package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.google.common.base.Preconditions.checkNotNull;

public class HttpRequestImpl implements HttpRequest {

    private static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");
    private OkHttpClient mOkHttpClient;
    private URI mUri;
    private String mBody;
    private HttpMethod mHttpMethod;
    private Map<String, String> mHeaders;


    HttpRequestImpl(@NonNull OkHttpClient okHttpClient, @NonNull URI uri,
                    @NonNull HttpMethod httpMethod,
                    @Nullable String body, @NonNull Map<String, String> headers) {
        mOkHttpClient = checkNotNull(okHttpClient);
        mUri = checkNotNull(uri);
        mHttpMethod = checkNotNull(httpMethod);
        mBody = body;
        mHeaders = new HashMap<>(checkNotNull(headers));
    }

    @Override
    public HttpResponse execute() throws HttpException {

        switch (mHttpMethod) {
            case GET:
                return getRequest();
            case POST: {
                try {
                    return postRequest();
                } catch (IOException e) {
                    throw new HttpException("Error serializing request body: " + e.getLocalizedMessage());
                }
            }
            default:
                throw new IllegalArgumentException("Request HTTP Method not valid: " + mHttpMethod.name());
        }
    }

    private HttpResponse getRequest() throws HttpException {
        URL url;
        HttpResponse response;
        try {
            url = mUri.toURL();
            Request.Builder requestBuilder = new Request.Builder()
                    .url(url);
            addHeaders(requestBuilder);
            Request okHttpRequest = requestBuilder.build();
            Response okHttpResponse = mOkHttpClient.newCall(okHttpRequest).execute();
            response = buildResponse(okHttpResponse);

        } catch (MalformedURLException e) {
            throw new HttpException("URL is malformed: " + e.getLocalizedMessage());
        } catch (ProtocolException e) {
            throw new HttpException("Http method not allowed: " + e.getLocalizedMessage());
        } catch (IOException e) {
            throw new HttpException("Something happened while retrieving data: " + e.getLocalizedMessage());
        }
        return response;
    }

    private HttpResponse postRequest() throws IOException {

        if(mBody == null) {
            throw new IOException("Json data is null");
        }

        URL url = mUri.toURL();
        RequestBody body = RequestBody.create(JSON, mBody);
        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(body);

        addHeaders(builder);
        Request httpOkRequest = builder.build();
        Response httpOkResponse = mOkHttpClient.newCall(httpOkRequest).execute();
        HttpResponse httpResponse = buildResponse(httpOkResponse);
        return httpResponse;
    }

    private void addHeaders(Request.Builder request) {
        for (Map.Entry<String, String> entry : mHeaders.entrySet()) {
            request.header(entry.getKey(), entry.getValue());
        }
    }

    private HttpResponse buildResponse(Response okHttpResponse) throws IOException {
        int responseCode = okHttpResponse.code();
        if (responseCode >= HttpURLConnection.HTTP_OK && responseCode < 300) {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    okHttpResponse.body().byteStream()));

            String inputLine;
            StringBuilder responseData = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                responseData.append(inputLine);
            }
            in.close();

            return new HttpResponseImpl(responseCode, (responseData.length() > 0 ? responseData.toString() : null));
        }
        return new HttpResponseImpl(responseCode);
    }

}
