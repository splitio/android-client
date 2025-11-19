package io.split.android.client.utils;

import java.util.Arrays;
import java.util.zip.Inflater;

import io.split.android.client.service.ServiceConstants;
import io.split.android.client.utils.logger.Logger;

public class Zlib implements CompressionUtil {

    @Override
    public byte[] decompress(byte[] input) {
        if (input == null || input.length == 0) {
            return null;
        }
        try {
            Inflater inflater = new Inflater();
            inflater.setInput(input);
            byte[] result = new byte[ServiceConstants.MY_SEGMENT_V2_DATA_SIZE];
            int resultLength = inflater.inflate(result);
            inflater.end();
            return Arrays.copyOfRange(result, 0, resultLength);
        } catch (java.util.zip.DataFormatException e) {
            Logger.e("DataFormatException error: " + e.getLocalizedMessage());
        } catch (Exception e) {
            Logger.e("Error decompressing: " + e.getLocalizedMessage());
        }
        return null;
    }
}
