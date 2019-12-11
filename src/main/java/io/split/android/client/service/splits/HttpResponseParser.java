package io.split.android.client.service.splits;

public interface HttpResponseParser<T> {
    T parse(String responseData) throws HttpResponseParserException;
}
