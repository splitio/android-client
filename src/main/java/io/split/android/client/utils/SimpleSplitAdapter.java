package io.split.android.client.utils;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.HashSet;

import io.split.android.client.dtos.SimpleSplit;

public final class SimpleSplitAdapter extends TypeAdapter<SimpleSplit> {

    @Override
    public void write(JsonWriter out, SimpleSplit value) throws IOException {
        out.beginObject();
        out.name("name").value(value.name);
        out.name("trafficTypeName").value(value.trafficTypeName);
        out.name("sets");
        if (value.sets != null) {
            out.beginArray();
            for (String set : value.sets) {
                out.value(set);
            }
            out.endArray();
        } else {
            out.nullValue();
        }
        out.endObject();
    }

    @Override
    public SimpleSplit read(JsonReader in) throws IOException {
        SimpleSplit result = new SimpleSplit();
        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "name":
                    result.name = in.nextString();
                    break;
                case "trafficTypeName":
                    result.trafficTypeName = in.nextString();
                    break;
                case "sets":
                    if (in.peek() != JsonToken.NULL) {
                        HashSet<String> set = new HashSet<>();
                        in.beginArray();
                        while (in.hasNext()) {
                            set.add(in.nextString());
                        }
                        in.endArray();
                        result.sets = set;
                    } else {
                        in.nextNull();
                    }
                    break;
                default:
                    in.skipValue();
            }
        }
        in.endObject();
        return result;
    }
}