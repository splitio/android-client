package io.split.android.client.track;

import org.apache.http.entity.StringEntity;

import io.split.android.client.utils.Utils;

public class EventsDataString implements EventsData {

    private final String _data;

    public static EventsDataString create(String events){
        return new EventsDataString(events);
    }

    public EventsDataString(String _data) {
        this._data = _data;
    }

    @Override
    public StringEntity asJSONEntity() {
        return Utils.toJsonEntity(_data);
    }

    @Override
    public String toString(){
        return _data;
    }
}
