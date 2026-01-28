package io.split.android.client.service.sseauthentication;

import com.google.gson.JsonSyntaxException;

import io.split.android.client.service.http.HttpResponseParser;
import io.split.android.client.service.http.HttpResponseParserException;
import io.split.android.client.service.sseclient.SseAuthenticationResponse;
import io.split.android.client.utils.Json;

public class SseAuthenticationResponseParser implements HttpResponseParser<SseAuthenticationResponse> {

    @Override
    public SseAuthenticationResponse parse(String responseData) throws HttpResponseParserException {
        try {
            return Json.fromJson(responseData, SseAuthenticationResponse.class);
        } catch (JsonSyntaxException e) {
            throw new HttpResponseParserException("Syntax error parsing my segments http response: " + e.getLocalizedMessage());
        } catch (Exception e) {
            throw new HttpResponseParserException("Unknown error parsing my segments http response: " + e.getLocalizedMessage());
        }
    }
}
