package io.split.android.client.storage.cipher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;

public class CBCCipherBatchCoreTest {

    private final JceCbcCipherProvider mProvider = new JceCbcCipherProvider();
    private final JceCbcCipherProvider mOtherKeyProvider =
            new JceCbcCipherProvider(JceCbcCipherProvider.OTHER_KEY, JceCbcCipherProvider.API_KEY);
    private final byte[] mIv = Arrays.copyOf(JceCbcCipherProvider.API_KEY.getBytes(StandardCharsets.UTF_8), 16);

    @Test
    public void concatenatedDecryptEqualsPerValueDecrypt() throws Exception {
        Random random = new Random(7);
        int compared = 0;
        int rejected = 0;
        int badLastValue = 0;
        for (int trial = 0; trial < 500; trial++) {
            int count = 1 + random.nextInt(40);
            List<byte[]> values = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                int length = random.nextInt(3) == 0 ? random.nextInt(16) : random.nextInt(600);
                StringBuilder text = new StringBuilder();
                for (int c = 0; c < length; c++) {
                    text.append((char) (32 + random.nextInt(90)));
                }
                JceCbcCipherProvider provider = random.nextInt(4) == 0 ? mOtherKeyProvider : mProvider;
                values.add(provider.newCipher(Cipher.ENCRYPT_MODE).doFinal(text.toString().getBytes(StandardCharsets.UTF_8)));
            }

            List<String> batch;
            try {
                batch = CBCCipher.decryptConcatenated(mProvider.newCipher(Cipher.DECRYPT_MODE), mIv, values);
            } catch (BadPaddingException e) {
                assertNull(perValue(values.get(count - 1)));
                badLastValue++;
                continue;
            }

            assertEquals(count, batch.size());
            for (int i = 0; i < count; i++) {
                assertEquals(perValue(values.get(i)), batch.get(i));
                compared++;
                if (batch.get(i) == null) {
                    rejected++;
                }
            }
        }
        assertTrue("batches with a bad last value: " + badLastValue, badLastValue > 0);
        assertTrue("rejected values: " + rejected, rejected > 0);
        assertTrue("compared values: " + compared, compared > 5000);
    }

    @Test
    public void singleBlockLastValueDecrypts() throws Exception {
        byte[] first = encrypt("a");
        byte[] second = encrypt("b");

        List<String> result = CBCCipher.decryptConcatenated(
                mProvider.newCipher(Cipher.DECRYPT_MODE), mIv, Arrays.asList(first, second));

        assertEquals(Arrays.asList("a", "b"), result);
        byte[] withoutSeparators = concat(first, second);
        assertThrows(BadPaddingException.class,
                () -> mProvider.newCipher(Cipher.DECRYPT_MODE).doFinal(withoutSeparators));
    }

    @Test
    public void emptyPlaintextsDecryptToEmptyStrings() throws Exception {
        List<byte[]> values = Arrays.asList(encrypt(""), encrypt("x"), encrypt(""), encrypt("y"), encrypt(""));

        List<String> result = CBCCipher.decryptConcatenated(mProvider.newCipher(Cipher.DECRYPT_MODE), mIv, values);

        assertEquals(Arrays.asList("", "x", "", "y", ""), result);
        assertEquals(16, values.get(0).length);
    }

    @Test
    public void everyPlaintextLengthAroundTheBlockSizeDecryptsInTheMiddle() throws Exception {
        List<byte[]> values = new ArrayList<>();
        List<String> expected = new ArrayList<>();
        for (int length = 0; length <= 50; length++) {
            String text = repeat('a', length);
            values.add(encrypt(text));
            expected.add(text);
        }
        values.add(encrypt("last"));
        expected.add("last");

        List<String> result = CBCCipher.decryptConcatenated(mProvider.newCipher(Cipher.DECRYPT_MODE), mIv, values);

        assertEquals(expected, result);
    }

    @Test
    public void middleValueWithInvalidPaddingIsRejectedAndItsNeighboursAreNot() throws Exception {
        int[][] invalidTails = {
                {0},
                {17},
                {255},
                {2, 3},
                {1, 2},
                {7, 7, 7, 7, 7, 7, 9, 7},
        };
        for (int[] tail : invalidTails) {
            byte[] block = new byte[16];
            Arrays.fill(block, (byte) 'z');
            for (int i = 0; i < tail.length; i++) {
                block[16 - tail.length + i] = (byte) tail[i];
            }
            List<byte[]> values = Arrays.asList(encrypt("before"), rawBlock(block), encrypt("after"));

            List<String> result = CBCCipher.decryptConcatenated(mProvider.newCipher(Cipher.DECRYPT_MODE), mIv, values);

            assertEquals(Arrays.toString(tail), "before", result.get(0));
            assertNull(Arrays.toString(tail), result.get(1));
            assertEquals(Arrays.toString(tail), "after", result.get(2));
            assertNull(Arrays.toString(tail), perValue(values.get(1)));
        }
    }

    @Test
    public void middleValueWithPartialPaddingStillValidIsStrippedExactly() throws Exception {
        byte[] block = new byte[16];
        Arrays.fill(block, (byte) 'z');
        Arrays.fill(block, 13, 16, (byte) 3);
        List<byte[]> values = Arrays.asList(rawBlock(block), encrypt("after"));

        List<String> result = CBCCipher.decryptConcatenated(mProvider.newCipher(Cipher.DECRYPT_MODE), mIv, values);

        assertEquals(repeat('z', 13), result.get(0));
    }

    @Test
    public void middleValueWhosePaddingByteExceedsTheBlockSizeIsRejected() throws Exception {
        for (int pad : new int[]{17, 32, 255}) {
            byte[] plain = new byte[16 * ((pad + 15) / 16 + 1)];
            Arrays.fill(plain, (byte) 'z');
            Arrays.fill(plain, plain.length - pad, plain.length, (byte) pad);
            byte[] crafted = rawBlock(plain);
            List<byte[]> values = Arrays.asList(encrypt("before"), crafted, encrypt("after"));

            List<String> result = CBCCipher.decryptConcatenated(mProvider.newCipher(Cipher.DECRYPT_MODE), mIv, values);

            assertEquals("pad " + pad, "before", result.get(0));
            assertNull("pad " + pad, result.get(1));
            assertEquals("pad " + pad, "after", result.get(2));
        }
    }

    @Test
    public void lastValueWithInvalidPaddingThrows() throws Exception {
        byte[] block = new byte[16];
        Arrays.fill(block, (byte) 'z');
        block[15] = 5;
        List<byte[]> values = Arrays.asList(encrypt("before"), rawBlock(block));

        assertThrows(BadPaddingException.class, () -> CBCCipher.decryptConcatenated(
                mProvider.newCipher(Cipher.DECRYPT_MODE), mIv, values));
    }

    @Test
    public void exceptionsOtherThanBadPaddingPropagate() {
        Cipher notInitialized;
        try {
            notInitialized = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }

        assertThrows(IllegalStateException.class, () -> CBCCipher.decryptConcatenated(
                notInitialized, mIv, Arrays.asList(new byte[16], new byte[16])));
    }

    private String perValue(byte[] value) {
        try {
            return new String(mProvider.newCipher(Cipher.DECRYPT_MODE).doFinal(value), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            return null;
        }
    }

    private byte[] encrypt(String text) throws GeneralSecurityException {
        return mProvider.newCipher(Cipher.ENCRYPT_MODE).doFinal(text.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] rawBlock(byte[] plain) {
        return mProvider.encryptWithoutPadding(plain);
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.write(part, 0, part.length);
        }
        return out.toByteArray();
    }

    private static String repeat(char c, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, c);
        return new String(chars);
    }
}
