package io.split.android.client.service.rules;

import com.google.gson.JsonSyntaxException;

import io.split.android.client.dtos.TargetingRulesChange;
import io.split.android.client.service.http.HttpResponseParser;
import io.split.android.client.service.http.HttpResponseParserException;
import io.split.android.client.utils.Json;

public class TargetingRulesResponseParser implements HttpResponseParser<TargetingRulesChange> {

    @Override
    public TargetingRulesChange parse(String responseData) throws HttpResponseParserException {
        try {
            return Json.fromJson(responseData, TargetingRulesChange.class);
//            return TargetingRulesChange.create(Json.fromJson(responseData, SplitChange.class));
        } catch (JsonSyntaxException e) {
            throw new HttpResponseParserException("Syntax error parsing my segments http response: " + e.getLocalizedMessage());
        } catch (Exception e) {
            throw new HttpResponseParserException("Unknown error parsing my segments http response: " + e.getLocalizedMessage());
        }
    }
}
