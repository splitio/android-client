package io.split.android.client.service.sseclient.notifications;

import java.nio.charset.Charset;

import io.split.android.client.exceptions.MySegmentsParsingException;
import io.split.android.client.utils.Base64Util;
import io.split.android.client.utils.CompressionUtil;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.StringHelper;

public class KeyListParser {

    public KeyList parse(String payload, CompressionUtil compressionUtil) throws MySegmentsParsingException {

        byte[] decoded = Base64Util.bytesDecode(payload);
        if(decoded == null) {
            throw new MySegmentsParsingException("Could not decode payload");
        }

        byte[] decompressed = compressionUtil.decompress(decoded);
        if(decompressed == null) {
            throw new MySegmentsParsingException("Could not decompress payload");
        }

        KeyList keyList;
        try {
            keyList = Json.fromJson(StringHelper.stringFromBytes(decompressed), KeyList.class);
        } catch (Exception ex) {
            throw new MySegmentsParsingException("Unable to parse Json Key List: " + ex.getLocalizedMessage());
        }
        return keyList;
    }
}
