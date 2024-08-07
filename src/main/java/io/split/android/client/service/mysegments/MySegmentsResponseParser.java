package io.split.android.client.service.mysegments;

import com.google.gson.JsonSyntaxException;

import io.split.android.client.dtos.MySegmentsResponse;
import io.split.android.client.service.http.HttpResponseParser;
import io.split.android.client.service.http.HttpResponseParserException;
import io.split.android.client.utils.Json;

public class MySegmentsResponseParser implements HttpResponseParser<MySegmentsResponse> {

    @Override
    public MySegmentsResponse parse(String responseData) throws HttpResponseParserException {
        try {
            return Json.fromJson(responseData, MySegmentsResponse.class);
        } catch (JsonSyntaxException e) {
            throw new HttpResponseParserException("Syntax error parsing my segments http response: " + e.getLocalizedMessage());
        } catch (Exception e) {
            throw new HttpResponseParserException("Unknown error parsing my segments http response: " + e.getLocalizedMessage());
        }
    }
}
