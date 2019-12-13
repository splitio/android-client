package io.split.android.client.service;

public interface HttpResponseParser<T> {
    T parse(String responseData) throws HttpResponseParserException;
}
