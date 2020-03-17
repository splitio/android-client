package io.split.android.client.service.sseauthentication;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.MySegment;
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
