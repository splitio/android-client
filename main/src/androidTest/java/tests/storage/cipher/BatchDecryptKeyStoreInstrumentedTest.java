package tests.storage.cipher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.os.Build;

import androidx.test.filters.SdkSuppress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.split.android.client.storage.cipher.CBCCipher;

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
public class BatchDecryptKeyStoreInstrumentedTest {

    private String mApiKey;
    private CBCCipher mCipher;

    @Before
    public void setUp() {
        CBCCipher.resetBatchingForTesting();
        mApiKey = UUID.randomUUID().toString().replace("-", "") + "batch";
        mCipher = new CBCCipher(mApiKey);
    }

    @After
    public void tearDown() throws Exception {
        CBCCipher.resetBatchingForTesting();
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        keyStore.deleteEntry("split_key_" + mApiKey);
    }

    @Test
    public void twoHundredValuesDecryptLikeOneByOne() {
        List<String> plain = new ArrayList<>();
        List<String> encrypted = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            String text = "value-" + i + "-" + repeat((char) ('a' + i % 26), i * 7);
            plain.add(text);
            encrypted.add(mCipher.encrypt(text));
        }

        List<String> batched = mCipher.decryptAll(encrypted);

        assertEquals(plain, batched);
        assertEquals(perValue(encrypted), batched);
        assertFalse(CBCCipher.isBatchingDisabled());
    }

    @Test
    public void threeMegabytesOfValuesDecryptLikeOneByOne() {
        List<String> plain = new ArrayList<>();
        List<String> encrypted = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            String text = repeat((char) ('a' + i % 26), 100_000);
            plain.add(text);
            encrypted.add(mCipher.encrypt(text));
        }

        List<String> batched = mCipher.decryptAll(encrypted);

        assertEquals(plain, batched);
        assertEquals(perValue(encrypted), batched);
        assertFalse(CBCCipher.isBatchingDisabled());
    }

    private List<String> perValue(List<String> encrypted) {
        List<String> result = new ArrayList<>();
        for (String value : encrypted) {
            result.add(mCipher.decrypt(value));
        }
        return result;
    }

    private static String repeat(char c, int count) {
        char[] chars = new char[count];
        java.util.Arrays.fill(chars, c);
        return new String(chars);
    }
}
