package io.split.android.client.service.mysegments;

import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.MyLargeSegmentsResponse;
import io.split.android.client.dtos.MySegment;
import io.split.android.client.service.http.HttpResponseParser;
import io.split.android.client.service.http.HttpResponseParserException;
import io.split.android.client.utils.Json;

public class MyLargeSegmentsResponseParser implements HttpResponseParser<List<MySegment>> {

    @Override
    public List<MySegment> parse(String responseData) throws HttpResponseParserException {
        try {
            return mapToMySegmentsList(Json.fromJson(responseData, MyLargeSegmentsResponse.class).getMyLargeSegments());
        } catch (JsonSyntaxException e) {
            throw new HttpResponseParserException("Syntax error parsing my large segments http response: " + e.getLocalizedMessage());
        } catch (Exception e) {
            throw new HttpResponseParserException("Unknown error parsing my large segments http response: " + e.getLocalizedMessage());
        }
    }

    private static List<MySegment> mapToMySegmentsList(List<String> names) {
        List<MySegment> mySegments = new ArrayList<>();

        for (String name : names) {
            mySegments.add(MySegment.create(name));
        }

        return mySegments;
    }
}
