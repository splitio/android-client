package io.split.android.client.service.mysegments;

import com.google.gson.JsonSyntaxException;

import io.split.android.client.dtos.AllSegmentsChange;
import io.split.android.client.service.http.HttpResponseParser;
import io.split.android.client.service.http.HttpResponseParserException;
import io.split.android.client.utils.Json;

public class AllSegmentsResponseParser implements HttpResponseParser<AllSegmentsChange> {

    @Override
    public AllSegmentsChange parse(String responseData) throws HttpResponseParserException {
        try {
            // TODO legacy endpoint support
//            try {
//                MySegmentsResponse mySegmentsResponse = Json.fromJson(responseData, MySegmentsResponse.class);
//                Logger.d("Parsed legacy segments response");
//                return new AllSegmentsChange(mySegmentsResponse.getSegments());
//            } catch (Exception e) {

            return Json.fromJson(responseData, AllSegmentsChange.class);
//            }

        } catch (JsonSyntaxException e) {
            throw new HttpResponseParserException("Syntax error parsing my large segments http response: " + e.getLocalizedMessage());
        } catch (Exception e) {
            throw new HttpResponseParserException("Unknown error parsing my large segments http response: " + e.getLocalizedMessage());
        }
    }
}
