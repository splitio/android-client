package io.split.android.client.network.eventsource;

import androidx.annotation.VisibleForTesting;

import java.util.Map;

public class EventSourceStreamParser {
    private final static String FIELD_SEPARATOR = ":";

    /**
     * This parsing implementation is based in the folowing specification:
     * https://www.w3.org/TR/2009/WD-eventsource-20090421/#references
     * Bulletpoint 7 Interpreting an event stream
     *
     * @param streamLine:    The line from the stream to be parsed
     * @param messageValues: A map where the field, value pair is should added be added
     *                       if the line contains any.
     * @return Returns true if a blank line meaning the final of an event if found.
     */
    @VisibleForTesting
    public boolean parseLineAndAppendValue(String streamLine, Map<String, String> messageValues) {

        if(streamLine == null) {
            return false;
        }

        String trimmedLine = streamLine.trim();

        if (trimmedLine.isEmpty()) {
            return true;
        }

        int separatorIndex = trimmedLine.indexOf(FIELD_SEPARATOR);

        if(separatorIndex == 0) {
            return false;
        }

        if (separatorIndex > -1) {
            String field = trimmedLine.substring(0, separatorIndex).trim();
            String value = "";
            if (separatorIndex < trimmedLine.length() - 1) {
                value = trimmedLine.substring(separatorIndex + 1, trimmedLine.length()).trim();
            }
            messageValues.put(field, value);
        } else {
            messageValues.put(trimmedLine.trim(), "");
        }
        return false;
    }
}
