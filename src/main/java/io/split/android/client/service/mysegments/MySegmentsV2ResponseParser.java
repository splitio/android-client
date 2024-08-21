package io.split.android.client.service.mysegments;

import com.google.gson.JsonSyntaxException;

import io.split.android.client.dtos.MySegmentsResponse;
import io.split.android.client.service.http.HttpResponseParser;
import io.split.android.client.service.http.HttpResponseParserException;
import io.split.android.client.utils.Json;

public class MySegmentsV2ResponseParser implements HttpResponseParser<SegmentResponseV2> {

    @Override
    public SegmentResponseV2 parse(String responseData) throws HttpResponseParserException {
        try {
            // TODO legacy endpoint support
            try {
                MySegmentsResponse mySegmentsResponse = Json.fromJson(responseData, MySegmentsResponse.class);
                return new SegmentResponseV2Impl(mySegmentsResponse.getSegments());
            } catch (Exception e) {
                // This will used when the new DTO is defined
                SegmentResponseV2Impl mySegmentsResponse = Json.fromJson(responseData, SegmentResponseV2Impl.class);

                return mySegmentsResponse;
            }

        } catch (JsonSyntaxException e) {
            throw new HttpResponseParserException("Syntax error parsing my large segments http response: " + e.getLocalizedMessage());
        } catch (Exception e) {
            throw new HttpResponseParserException("Unknown error parsing my large segments http response: " + e.getLocalizedMessage());
        }
    }
}
