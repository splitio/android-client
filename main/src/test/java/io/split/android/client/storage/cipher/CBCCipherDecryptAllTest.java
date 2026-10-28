package io.split.android.client.storage.cipher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.util.Log;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.ProviderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Cipher;

import io.split.android.client.utils.logger.LogPrinterImpl;
import io.split.android.client.utils.logger.LogPrinterStub;
import io.split.android.client.utils.logger.Logger;
import io.split.android.client.utils.logger.SplitLogLevel;

public class CBCCipherDecryptAllTest {

    private static final byte[] BAD_PADDING_BLOCK = invalidPaddingBlock();

    private JceCbcCipherProvider mProvider;
    private CBCCipher mCipher;
    private JceCbcCipherProvider mOtherKeyProvider;
    private CBCCipher mOtherKeyCipher;
    private LogPrinterStub mPrinter;

    @Before
    public void setUp() {
        CBCCipher.resetBatchingForTesting();
        mProvider = new JceCbcCipherProvider();
        mCipher = new CBCCipher(mProvider);
        mOtherKeyProvider = new JceCbcCipherProvider(JceCbcCipherProvider.OTHER_KEY, JceCbcCipherProvider.API_KEY);
        mOtherKeyCipher = new CBCCipher(mOtherKeyProvider);
        mPrinter = new LogPrinterStub();
        Logger.instance().setPrinter(mPrinter);
        Logger.instance().setLevel(SplitLogLevel.VERBOSE);
    }

    @After
    public void tearDown() {
        CBCCipher.resetBatchingForTesting();
        Logger.instance().setLevel(SplitLogLevel.NONE);
        Logger.instance().setPrinter(new LogPrinterImpl());
    }

    @Test
    public void matchesDecryptForEveryValue() {
        String wrapped = wrapLines(mCipher.encrypt(repeat('w', 700)));
        List<String> values = Arrays.asList(
                mCipher.encrypt("first"),
                null,
                "",
                "\n",
                "!!! not base64 !!!",
                "abc",
                mCipher.encrypt(""),
                mCipher.encrypt(repeat('l', 5000)),
                mOtherKeyCipher.encrypt("wrong key in the middle"),
                wrapped,
                mCipher.encrypt("short"),
                mOtherKeyCipher.encrypt("wrong key last"));
        List<String> withValidLast = new ArrayList<>(values.subList(0, values.size() - 1));
        withValidLast.add(mCipher.encrypt("valid last"));

        for (List<String> list : Arrays.asList(values, withValidLast)) {
            List<String> expected = perValue(list);
            mProvider.decryptionCalls.set(0);

            List<String> result = mCipher.decryptAll(list);

            assertEquals(expected, result);
            assertEquals("first", result.get(0));
            assertEquals(repeat('w', 700), result.get(9));
            assertNull(result.get(1));
            assertEquals("", result.get(6));
        }
    }

    @Test
    public void emptyListReturnsEmptyListWithoutAnOperation() {
        List<String> result = mCipher.decryptAll(new ArrayList<>());

        assertTrue(result.isEmpty());
        assertEquals(0, mProvider.decryptionCalls.get());
    }

    @Test
    public void rejectedValueIsRetriedAlone() {
        List<String> values = Arrays.asList(
                mCipher.encrypt("one"), encode(BAD_PADDING_BLOCK), mCipher.encrypt("three"));
        mProvider.decryptionCalls.set(0);

        List<String> result = mCipher.decryptAll(values);

        assertEquals(Arrays.asList("one", null, "three"), result);
        assertEquals(2, mProvider.decryptionCalls.get());
        assertFalse(CBCCipher.isBatchingDisabled());
    }

    @Test
    public void retryVerdictWinsOverManualCheck() {
        Cipher lax = mock(Cipher.class);
        try {
            when(lax.doFinal(any(byte[].class))).thenReturn("lax".getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        JceCbcCipherProvider provider = new JceCbcCipherProvider() {
            @Override
            protected Cipher createDecryptionCipher() {
                return decryptionCalls.get() == 2 ? lax : super.createDecryptionCipher();
            }
        };
        CBCCipher cipher = new CBCCipher(provider);
        List<String> values = Arrays.asList(
                cipher.encrypt("one"), encode(BAD_PADDING_BLOCK), cipher.encrypt("three"));

        List<String> result = cipher.decryptAll(values);

        assertEquals(Arrays.asList("one", "lax", "three"), result);
        assertEquals(2, provider.decryptionCalls.get());
        assertTrue(provider.released().contains(lax));
    }

    @Test
    public void badPaddingFallsBackWithoutSwitchingOff() {
        List<String> first = Arrays.asList(
                mCipher.encrypt("one"), mCipher.encrypt("two"), encode(BAD_PADDING_BLOCK));
        mProvider.decryptionCalls.set(0);

        List<String> result = mCipher.decryptAll(first);

        assertEquals(Arrays.asList("one", "two", null), result);
        assertEquals(1 + 3, mProvider.decryptionCalls.get());
        assertFalse(CBCCipher.isBatchingDisabled());
        assertTrue(mPrinter.getLoggedMessages().get(Log.WARN).isEmpty());

        mProvider.decryptionCalls.set(0);
        List<String> second = mCipher.decryptAll(Arrays.asList(
                mCipher.encrypt("three"), mCipher.encrypt("four"), mCipher.encrypt("five")));

        assertEquals(Arrays.asList("three", "four", "five"), second);
        assertEquals(1, mProvider.decryptionCalls.get());
    }

    @Test
    public void otherExceptionSwitchesBatchingOff() throws Exception {
        Cipher failing = mock(Cipher.class);
        when(failing.getIV()).thenReturn(new byte[16]);
        when(failing.doFinal(any(byte[].class))).thenThrow(new ProviderException("keystore refused"));
        AtomicBoolean handedOut = new AtomicBoolean();
        JceCbcCipherProvider provider = new JceCbcCipherProvider() {
            @Override
            protected Cipher createDecryptionCipher() {
                return handedOut.compareAndSet(false, true) ? failing : super.createDecryptionCipher();
            }
        };
        CBCCipher cipher = new CBCCipher(provider);
        List<String> encrypted = Arrays.asList(
                cipher.encrypt("one"), cipher.encrypt("two"), cipher.encrypt("three"));

        List<String> result = cipher.decryptAll(encrypted);

        assertEquals(Arrays.asList("one", "two", "three"), result);
        assertEquals(1 + 3, provider.decryptionCalls.get());
        assertTrue(CBCCipher.isBatchingDisabled());
        assertTrue(provider.released().contains(failing));

        provider.decryptionCalls.set(0);
        List<String> later = cipher.decryptAll(Arrays.asList(cipher.encrypt("four"), cipher.encrypt("five")));

        assertEquals(Arrays.asList("four", "five"), later);
        assertEquals(2, provider.decryptionCalls.get());

        List<String> warnings = new ArrayList<>(mPrinter.getLoggedMessages().get(Log.WARN));
        assertEquals(warnings.toString(), 1, warnings.size());
        assertTrue(warnings.get(0), warnings.get(0).contains("3"));
        assertTrue(warnings.get(0), warnings.get(0).contains("ProviderException"));
        for (String stored : encrypted) {
            assertFalse(warnings.get(0).contains(stored));
        }
        assertFalse(warnings.get(0).contains("keystore refused"));
    }

    @Test
    public void nullCipherFallsBackAndKeepsBatching() {
        AtomicBoolean returnedNull = new AtomicBoolean();
        JceCbcCipherProvider provider = new JceCbcCipherProvider() {
            @Override
            protected Cipher createDecryptionCipher() {
                return returnedNull.compareAndSet(false, true) ? null : super.createDecryptionCipher();
            }
        };
        CBCCipher cipher = new CBCCipher(provider);
        List<String> encrypted = Arrays.asList(
                cipher.encrypt("one"), cipher.encrypt("two"), cipher.encrypt("three"));

        List<String> result = cipher.decryptAll(encrypted);

        assertEquals(Arrays.asList("one", "two", "three"), result);
        assertEquals(1 + 3, provider.decryptionCalls.get());
        assertFalse(CBCCipher.isBatchingDisabled());
        assertEquals(0, Collections.frequency(provider.released(), null));

        provider.decryptionCalls.set(0);
        assertEquals(Arrays.asList("four", "five"),
                cipher.decryptAll(Arrays.asList(cipher.encrypt("four"), cipher.encrypt("five"))));
        assertEquals(1, provider.decryptionCalls.get());
    }

    @Test
    public void nullCipherEverywhereReleasesNullOncePerValueFromDecrypt() {
        JceCbcCipherProvider provider = new JceCbcCipherProvider() {
            @Override
            protected Cipher createDecryptionCipher() {
                return null;
            }
        };
        CBCCipher cipher = new CBCCipher(provider);
        List<String> encrypted = Arrays.asList(
                mCipher.encrypt("one"), mCipher.encrypt("two"), mCipher.encrypt("three"));
        List<String> expected = perValue(encrypted, cipher);
        int releasedBefore = Collections.frequency(provider.released(), null);
        provider.decryptionCalls.set(0);

        List<String> result = cipher.decryptAll(encrypted);

        assertEquals(expected, result);
        assertEquals(Arrays.asList(null, null, null), result);
        assertEquals(1 + 3, provider.decryptionCalls.get());
        assertEquals(3, Collections.frequency(provider.released(), null) - releasedBefore);
        assertFalse(CBCCipher.isBatchingDisabled());
    }

    @Test
    public void invalidIvFallsBackAndSwitchesBatchingOff() {
        List<byte[]> ivs = Arrays.asList(null, new byte[0], new byte[15], new byte[17], new byte[32]);
        for (byte[] iv : ivs) {
            CBCCipher.resetBatchingForTesting();
            Cipher odd = mock(Cipher.class);
            when(odd.getIV()).thenReturn(iv);
            JceCbcCipherProvider provider = new JceCbcCipherProvider() {
                @Override
                protected Cipher createDecryptionCipher() {
                    return decryptionCalls.get() == 1 ? odd : super.createDecryptionCipher();
                }
            };
            CBCCipher cipher = new CBCCipher(provider);
            String label = iv == null ? "null" : String.valueOf(iv.length);

            List<String> result = cipher.decryptAll(Arrays.asList(
                    cipher.encrypt("one"), cipher.encrypt("two"), cipher.encrypt("three")));

            assertEquals(label, Arrays.asList("one", "two", "three"), result);
            assertEquals(label, 1 + 3, provider.decryptionCalls.get());
            assertEquals(label, 1, Collections.frequency(provider.released(), odd));
            assertTrue(label, CBCCipher.isBatchingDisabled());
            assertTrue(label, mPrinter.getLoggedMessages().get(Log.WARN).isEmpty());
        }
    }

    @Test
    public void capSplitsBatchesAtOneMegabyte() {
        List<String> values = new ArrayList<>();
        List<String> plain = new ArrayList<>();
        for (int i = 0; i < 35; i++) {
            String text = repeat((char) ('a' + i % 26), 100_000);
            plain.add(text);
            values.add(mCipher.encrypt(text));
        }
        mProvider.decryptionCalls.set(0);

        List<String> result = mCipher.decryptAll(values);

        assertEquals(plain, result);
        assertEquals(4, mProvider.decryptionCalls.get());
    }

    @Test
    public void batchThatReachesTheCapExactlyIsNotSplit() {
        List<String> values = Arrays.asList(
                mCipher.encrypt(repeat('a', 524_287)), mCipher.encrypt(repeat('b', 524_271)));
        assertEquals(CBCCipher.MAX_BATCH_BYTES, 524_288 + 16 + 524_272);
        mProvider.decryptionCalls.set(0);

        List<String> result = mCipher.decryptAll(values);

        assertEquals(Arrays.asList(repeat('a', 524_287), repeat('b', 524_271)), result);
        assertEquals(1, mProvider.decryptionCalls.get());
    }

    @Test
    public void separatorsCountTowardsTheCap() {
        List<String> values = Arrays.asList(
                mCipher.encrypt(repeat('a', 524_287)), mCipher.encrypt(repeat('b', 524_287)));
        mProvider.decryptionCalls.set(0);

        List<String> result = mCipher.decryptAll(values);

        assertEquals(Arrays.asList(repeat('a', 524_287), repeat('b', 524_287)), result);
        assertEquals(2, mProvider.decryptionCalls.get());
    }

    @Test
    public void separatorsCountTowardsTheCapWhenValuesAccumulate() {
        List<String> values = Arrays.asList(
                mCipher.encrypt(repeat('a', 262_143)),
                mCipher.encrypt(repeat('b', 262_143)),
                mCipher.encrypt(repeat('c', 524_271)));
        mProvider.decryptionCalls.set(0);

        List<String> result = mCipher.decryptAll(values);

        assertEquals(Arrays.asList(repeat('a', 262_143), repeat('b', 262_143), repeat('c', 524_271)), result);
        assertEquals(2, mProvider.decryptionCalls.get());
    }

    @Test
    public void threeValuesReachingTheCapExactlyWithSeparatorsAreNotSplit() {
        List<String> values = Arrays.asList(
                mCipher.encrypt(repeat('a', 262_143)),
                mCipher.encrypt(repeat('b', 262_143)),
                mCipher.encrypt(repeat('c', 524_255)));
        assertEquals(CBCCipher.MAX_BATCH_BYTES, 262_144 + 262_144 + 524_256 + 2 * 16);
        mProvider.decryptionCalls.set(0);

        List<String> result = mCipher.decryptAll(values);

        assertEquals(Arrays.asList(repeat('a', 262_143), repeat('b', 262_143), repeat('c', 524_255)), result);
        assertEquals(1, mProvider.decryptionCalls.get());
    }

    @Test
    public void loneBatchableValueIsDecryptedWithoutInspectingTheBatchIv() throws Exception {
        Cipher first = mock(Cipher.class);
        when(first.doFinal(any(byte[].class))).thenReturn("lone".getBytes(StandardCharsets.UTF_8));
        AtomicInteger handedOut = new AtomicInteger();
        JceCbcCipherProvider provider = new JceCbcCipherProvider() {
            @Override
            protected Cipher createDecryptionCipher() {
                return handedOut.incrementAndGet() == 2 ? first : super.createDecryptionCipher();
            }
        };
        CBCCipher cipher = new CBCCipher(provider);

        List<String> result = cipher.decryptAll(Arrays.asList(null, cipher.encrypt("lone"), "abc"));

        assertEquals(Arrays.asList(null, "lone", null), result);
        verify(first, never()).getIV();
        assertFalse(CBCCipher.isBatchingDisabled());
    }

    @Test
    public void valueLargerThanTheCapIsDecryptedAlone() {
        String big = repeat('x', 1_500_000);
        List<String> values = Arrays.asList(
                mCipher.encrypt(big), mCipher.encrypt("two"), mCipher.encrypt("three"));
        mProvider.decryptionCalls.set(0);

        List<String> result = mCipher.decryptAll(values);

        assertEquals(Arrays.asList(big, "two", "three"), result);
        assertEquals(2, mProvider.decryptionCalls.get());
    }

    @Test
    public void factoryBatchesOnlyForCBCCipher() {
        List<String> values = Arrays.asList(mCipher.encrypt("one"), mCipher.encrypt("two"), mCipher.encrypt("three"));
        mProvider.decryptionCalls.set(0);

        List<String> result = SplitCipherFactory.decryptAll(mCipher, values);

        assertEquals(Arrays.asList("one", "two", "three"), result);
        assertEquals(1, mProvider.decryptionCalls.get());
    }

    @Test
    public void factoryLoopsDecryptForOtherCiphers() {
        SplitCipher mocked = mock(SplitCipher.class);
        when(mocked.decrypt("a")).thenReturn("A");
        when(mocked.decrypt("b")).thenReturn(null);
        when(mocked.decrypt("c")).thenReturn("C");

        List<String> result = SplitCipherFactory.decryptAll(mocked, Arrays.asList("a", "b", "c"));

        assertEquals(Arrays.asList("A", null, "C"), result);
        verify(mocked).decrypt("a");
        verify(mocked).decrypt("b");
        verify(mocked).decrypt("c");
    }

    @Test
    public void factoryKeepsValuesUntouchedForNoOpCipher() {
        List<String> values = Arrays.asList("a", null, "c");

        List<String> result = SplitCipherFactory.decryptAll(new NoOpCipher(), values);

        assertEquals(values, result);
        assertNotSame(values, result);
    }

    @Test
    public void batchStartedAfterAFlushIsMeasuredFromZero() {
        String x = repeat('x', 1_000_000);
        String b = repeat('b', 262_143);
        String c = repeat('c', 262_143);
        String d = repeat('d', 524_255);
        List<String> values = Arrays.asList(
                mCipher.encrypt(x), mCipher.encrypt(b), mCipher.encrypt(c), mCipher.encrypt(d));
        mProvider.decryptionCalls.set(0);

        List<String> result = mCipher.decryptAll(values);

        assertEquals(Arrays.asList(x, b, c, d), result);
        assertEquals(2, mProvider.decryptionCalls.get());
    }

    @Test
    public void emptyValueBetweenValidOnesIsDecryptedAloneAndKeepsBatching() {
        List<String> values = Arrays.asList(mCipher.encrypt("one"), "", mCipher.encrypt("three"));
        List<String> expected = perValue(values);
        mProvider.decryptionCalls.set(0);

        List<String> result = mCipher.decryptAll(values);

        assertEquals(expected, result);
        assertEquals("one", result.get(0));
        assertEquals("three", result.get(2));
        assertEquals(2, mProvider.decryptionCalls.get());
        assertFalse(CBCCipher.isBatchingDisabled());
    }

    @Test
    public void emptyValueIsLeftOutOfTheBatchInput() {
        List<Integer> inputLengths = new ArrayList<>();
        JceCbcCipherProvider provider = new JceCbcCipherProvider() {
            @Override
            protected Cipher createDecryptionCipher() {
                Cipher real = super.createDecryptionCipher();
                Cipher recording = mock(Cipher.class);
                try {
                    when(recording.getIV()).thenReturn(real.getIV());
                    when(recording.doFinal(any(byte[].class))).thenAnswer(invocation -> {
                        byte[] input = invocation.getArgument(0);
                        inputLengths.add(input.length);
                        return real.doFinal(input);
                    });
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
                return recording;
            }
        };
        CBCCipher cipher = new CBCCipher(provider);
        String one = cipher.encrypt("one");
        String three = cipher.encrypt("three");

        List<String> result = cipher.decryptAll(Arrays.asList(one, "", three));

        assertEquals(Arrays.asList("one", "", "three"), result);
        assertEquals(Arrays.asList(0, 16 + 16 + 16), inputLengths);
    }

    @Test
    public void encryptingNullDoesNotUseTheProviderOrLog() {
        assertNull(mCipher.encrypt(null));

        assertEquals(0, mProvider.encryptionCalls.get());
        assertTrue(mPrinter.getLoggedMessages().get(Log.ERROR).isEmpty());
    }

    private List<String> perValue(List<String> values) {
        return perValue(values, mCipher);
    }

    private static List<String> perValue(List<String> values, CBCCipher cipher) {
        List<String> result = new ArrayList<>();
        for (String value : values) {
            result.add(cipher.decrypt(value));
        }
        return result;
    }

    private static byte[] invalidPaddingBlock() {
        byte[] plain = new byte[16];
        Arrays.fill(plain, (byte) 'z');
        plain[15] = 5;
        return new JceCbcCipherProvider().encryptWithoutPadding(plain);
    }

    private static String encode(byte[] bytes) {
        return java.util.Base64.getEncoder().encodeToString(bytes);
    }

    private static String wrapLines(String text) {
        StringBuilder wrapped = new StringBuilder();
        for (int i = 0; i < text.length(); i += 76) {
            wrapped.append(text, i, Math.min(text.length(), i + 76)).append('\n');
        }
        return wrapped.toString();
    }

    private static String repeat(char c, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, c);
        return new String(chars);
    }
}
