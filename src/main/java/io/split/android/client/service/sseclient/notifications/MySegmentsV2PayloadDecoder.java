package io.split.android.client.service.sseclient.notifications;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.exceptions.MySegmentsParsingException;
import io.split.android.client.utils.Base64Util;
import io.split.android.client.utils.CompressionUtil;
import io.split.android.client.utils.MurmurHash3;
import io.split.android.client.utils.StringHelper;

import static java.lang.Math.abs;

public class MySegmentsV2PayloadDecoder {

    public final int FIELD_SIZE = 8;

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
        if (decompressed == null) {
            throw new MySegmentsParsingException("Could not decompress payload");
        }
        return decompressed;
    }

    public boolean isKeyInBitmap(byte[] keyMap, int index) {
        int internal = index / FIELD_SIZE;
        byte offset = (byte) (index % FIELD_SIZE);
        if (internal > keyMap.length - 1) {
            return false;
        }
        return (keyMap[internal] & 1 << offset) != 0;
    }

    public BigInteger hashKey(String key) {
        return MurmurHash3.unsignedHash128x64(key.getBytes(StringHelper.defaultCharset()))[0];
    }

    public int computeKeyIndex(BigInteger hashedKey, int keyMapLength) {
        return hashedKey.remainder(BigInteger.valueOf(keyMapLength * FIELD_SIZE)).intValue();
    }

    public KeyList.Action getKeyListAction(KeyList keyList, BigInteger hashedKey) {
        if(new HashSet<>(keyList.getAdded()).contains(hashedKey)) {
            return KeyList.Action.ADD;
        }
        if(new HashSet<>(keyList.getRemoved()).contains(hashedKey)) {
            return KeyList.Action.REMOVE;
        }
        return KeyList.Action.NONE;
    }
}
