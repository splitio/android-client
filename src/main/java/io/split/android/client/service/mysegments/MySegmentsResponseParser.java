package io.split.android.client.service.mysegments;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.MySegment;
import io.split.android.client.service.http.HttpResponseParser;
import io.split.android.client.service.http.HttpResponseParserException;
import io.split.android.client.utils.Json;

public class MySegmentsResponseParser implements HttpResponseParser<List<MySegment>> {

    static final private Type MY_SEGMENTS_RESPONSE_TYPE
            = new TypeToken<Map<String, List<MySegment>>>() {
    }.getType();

    @Override
    public List<MySegment> parse(String responseData) throws HttpResponseParserException {
        try {
            Map<String, List<MySegment>> parsedResponse = Json.fromJson(responseData, MY_SEGMENTS_RESPONSE_TYPE);
            return parsedResponse.get("mySegments");
        } catch (JsonSyntaxException e) {
            throw new HttpResponseParserException("Syntax error parsing my segments http response: " + e.getLocalizedMessage());
        } catch (Exception e) {
            throw new HttpResponseParserException("Unknown error parsing my segments http response: " + e.getLocalizedMessage());
        }
    }
}
