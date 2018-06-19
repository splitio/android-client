package io.split.android.client.track;

import org.apache.http.entity.StringEntity;

import java.util.List;

import io.split.android.client.dtos.Event;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Utils;

public class EventsDataList implements EventsData {

    private final List<Event> _data;

    public static EventsDataList create(List<Event> events){
        return new EventsDataList(events);
    }

    public EventsDataList(List<Event> _data) {
        this._data = _data;
    }

    @Override
    public StringEntity asJSONEntity() {
        return Utils.toJsonEntity(_data);
    }

    @Override
    public String toString(){
        return Json.toJson(_data);
    }
}
