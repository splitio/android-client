package io.split.android.client.service.sseclient.notifications;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import io.split.android.client.exceptions.MySegmentsParsingException;
import io.split.android.client.utils.Base64Util;
import io.split.android.client.utils.CompressionUtil;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;
import io.split.android.client.utils.MurmurHash3;
import io.split.android.client.utils.StringHelper;

import static java.lang.Math.abs;

public class MySegmentsV2PayloadDecoder {
    public final long DIVISOR = 8L;

    public String decodeAsString(String payload, CompressionUtil compressionUtil) throws MySegmentsParsingException {
        byte[] decoded = decodeAsBytes(payload, compressionUtil);
        return StringHelper.stringFromBytes(decoded);
    }

    public byte[] decodeAsBytes(String payload, CompressionUtil compressionUtil) throws MySegmentsParsingException {

        byte[] decoded = Base64Util.bytesDecode(payload);
        if (decoded == null) {
            throw new MySegmentsParsingException("Could not decode payload");
        }

        byte[] decompressed = compressionUtil.decompress(decoded);
//        int i =0;
//        for (byte b : decompressed) {
//            //String s = "00000000" + Integer.toBinaryString(b);
////            System.out.println(s);
//            System.out.println(i + " -> " + b + " (" + s.substring(s.length() - 8, s.length()) + ")");
//            i++;
//        }
        if (decompressed == null) {
            throw new MySegmentsParsingException("Could not decompress payload");
        }
        return decompressed;
    }

    public boolean hasKey(byte[] keyMap, String userKey) {
        BigInteger unsignedHash = MurmurHash3.unsignedHash128x64(userKey.getBytes())[0];
        long index = unsignedHash.remainder(BigInteger.valueOf(keyMap.length)).longValue();
        int internal = (int) (index / DIVISOR);
        byte offset = (byte) (index % DIVISOR);
        if (internal > keyMap.length - 1) {
            return false;
        }
        return (keyMap[internal] & 1 << offset) != 0;
    }
}
