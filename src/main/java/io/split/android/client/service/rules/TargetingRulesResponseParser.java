package io.split.android.client.service.rules;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.StringReader;

import io.split.android.client.dtos.SplitChange;
import io.split.android.client.dtos.TargetingRulesChange;
import io.split.android.client.service.http.HttpResponseParser;
import io.split.android.client.service.http.HttpResponseParserException;
import io.split.android.client.utils.Json;

public class TargetingRulesResponseParser implements HttpResponseParser<TargetingRulesChange> {

    @Override
    public TargetingRulesChange parse(String responseData) throws HttpResponseParserException {
        try {
            if (responseData == null || responseData.isEmpty()) {
                return null;
            }

            if (isNewDto(responseData)) {
                // New DTO: parse as TargetingRulesChange
                return Json.fromJson(responseData, TargetingRulesChange.class);
            } else {
                // Legacy DTO: parse as SplitChange, wrap as TargetingRulesChange
                SplitChange splitChange = Json.fromJson(responseData, SplitChange.class);
                if (splitChange == null) {
                    return null;
                }
                return TargetingRulesChange.create(splitChange);
            }
        } catch (Exception e) {
            throw new HttpResponseParserException("Error parsing splitChanges http response: " + e.getLocalizedMessage());
        }
    }

    private boolean isNewDto(String json) throws Exception {
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            reader.setLenient(true);
            if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                reader.beginObject();
                if (reader.hasNext()) {
                    String name = reader.nextName();
                    if (newFieldNameIsPresent(name)) {
                        return true;
                    }

                    reader.skipValue();
                    while (reader.hasNext()) {
                        name = reader.nextName();
                        if (newFieldNameIsPresent(name)) {
                            return true;
                        }
                        reader.skipValue();
                    }
                }
            } else {
                throw new HttpResponseParserException("Error parsing splitChanges http response: not a JSON object");
            }
            return false;
        }
    }

    private static boolean newFieldNameIsPresent(String name) {
        return "ff".equals(name) || "rbs".equals(name);
    }
}
