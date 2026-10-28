package io.split.android.client.storage.cipher;

import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;

import io.split.android.client.utils.Base64Util;
import io.split.android.client.utils.logger.Logger;

public class CBCCipher implements SplitCipher {

    private static final Charset CHARSET = Charset.forName("UTF-8");

    private static final int IV_LENGTH = 16;
    static final int MAX_BATCH_BYTES = 1 << 20;

    private static volatile boolean sBatchingDisabled;

    private final CipherProvider mCipherProvider;

    public CBCCipher(@NonNull String apiKey) {
        this(new CBCCipherProvider(apiKey));
    }

    @VisibleForTesting
    public CBCCipher(@NonNull CipherProvider cipherProvider) {
        mCipherProvider = cipherProvider;
    }

    @Override
    @Nullable
    public String encrypt(String data) {
        if (data == null) {
            return null;
        }

        Cipher cipher = mCipherProvider.getEncryptionCipher();
        byte[] encryptedBytes;
        try {
            encryptedBytes = cipher.doFinal(data.getBytes(CHARSET));
        } catch (Exception e) {
            Logger.e("Error encrypting data: " + e.getMessage());
            return null;
        } finally {
            mCipherProvider.release(cipher);
        }

        return Base64Util.encode(encryptedBytes);
    }

    @Override
    @Nullable
    public String decrypt(String data) {
        if (data == null) {
            return null;
        }

        Cipher cipher = mCipherProvider.getDecryptionCipher();
        try {
            byte[] bytes = cipher.doFinal(Base64Util.bytesDecode(data));

            return new String(bytes, CHARSET);
        } catch (Exception e) {
            Logger.e("Error decrypting data: input length " + data.length() + ", " + e.getClass().getSimpleName());
            return null;
        } finally {
            mCipherProvider.release(cipher);
        }
    }

    @VisibleForTesting
    public static void resetBatchingForTesting() {
        sBatchingDisabled = false;
    }

    @VisibleForTesting
    public static boolean isBatchingDisabled() {
        return sBatchingDisabled;
    }

    /**
     * Returns, in order, what {@link #decrypt(String)} returns for each value, using one cipher
     * operation per batch of values instead of one per value where possible.
     */
    @NonNull
    public List<String> decryptAll(@NonNull List<String> values) {
        String[] result = new String[values.size()];
        List<Integer> batchIndexes = new ArrayList<>();
        List<byte[]> batchValues = new ArrayList<>();
        int batchBytes = 0;
        for (int i = 0; i < result.length; i++) {
            String value = values.get(i);
            byte[] decoded = sBatchingDisabled ? null : decodeForBatch(value);
            if (decoded == null) {
                result[i] = decrypt(value);
                continue;
            }

            if (!batchValues.isEmpty() && batchBytes + IV_LENGTH + decoded.length > MAX_BATCH_BYTES) {
                flush(values, batchIndexes, batchValues, result);
                batchIndexes.clear();
                batchValues.clear();
                batchBytes = 0;
            }
            batchBytes += (batchValues.isEmpty() ? 0 : IV_LENGTH) + decoded.length;
            batchIndexes.add(i);
            batchValues.add(decoded);
        }
        flush(values, batchIndexes, batchValues, result);

        return new ArrayList<>(Arrays.asList(result));
    }

    @Nullable
    private static byte[] decodeForBatch(@Nullable String value) {
        if (value == null) {
            return null;
        }
        try {
            byte[] decoded = Base64.decode(value, Base64.DEFAULT);
            if (decoded == null || decoded.length == 0 || decoded.length % IV_LENGTH != 0) {
                return null;
            }
            return decoded;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void flush(List<String> values, List<Integer> indexes, List<byte[]> decoded, String[] result) {
        if (indexes.isEmpty()) {
            return;
        }

        List<String> plain = indexes.size() > 1 ? decryptBatch(decoded) : null;
        for (int i = 0; i < indexes.size(); i++) {
            String batched = plain != null ? plain.get(i) : null;
            result[indexes.get(i)] = batched != null ? batched : decrypt(values.get(indexes.get(i)));
        }
    }

    /** Returns null when the batch must be decrypted value by value. */
    @Nullable
    private List<String> decryptBatch(List<byte[]> decoded) {
        Cipher cipher = mCipherProvider.getDecryptionCipher();
        if (cipher == null) {
            return null;
        }

        try {
            byte[] iv = cipher.getIV();
            if (iv == null || iv.length != IV_LENGTH) {
                sBatchingDisabled = true;
                return null;
            }

            return decryptConcatenated(cipher, iv, decoded);
        } catch (BadPaddingException e) {
            return null;
        } catch (Exception e) {
            sBatchingDisabled = true;
            Logger.w("Batch decrypt of " + decoded.size() + " values failed: " + e.getClass().getSimpleName()
                    + ". Decrypting value by value");
            return null;
        } finally {
            mCipherProvider.release(cipher);
        }
    }

    @VisibleForTesting
    static List<String> decryptConcatenated(Cipher cipher, byte[] iv, List<byte[]> values) throws GeneralSecurityException {
        int total = 0;
        for (byte[] value : values) {
            total += value.length;
        }
        total += IV_LENGTH * (values.size() - 1);
        byte[] input = new byte[total];
        int offset = 0;
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                System.arraycopy(iv, 0, input, offset, IV_LENGTH);
                offset += IV_LENGTH;
            }
            byte[] value = values.get(i);
            System.arraycopy(value, 0, input, offset, value.length);
            offset += value.length;
        }

        byte[] plain = cipher.doFinal(input);

        List<String> result = new ArrayList<>(values.size());
        offset = 0;
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                offset += IV_LENGTH;
            }
            int encodedLength = values.get(i).length;
            boolean last = i == values.size() - 1;
            int length = last ? plain.length - offset : encodedLength;
            if (!last) {
                int pad = plain[offset + length - 1] & 0xff;
                boolean valid = pad >= 1 && pad <= IV_LENGTH;
                for (int b = 1; valid && b <= pad; b++) {
                    valid = (plain[offset + length - b] & 0xff) == pad;
                }
                if (!valid) {
                    result.add(null);
                    offset += encodedLength;
                    continue;
                }
                length -= pad;
            }
            result.add(new String(plain, offset, length, CHARSET));
            offset += encodedLength;
        }
        return result;
    }
}
