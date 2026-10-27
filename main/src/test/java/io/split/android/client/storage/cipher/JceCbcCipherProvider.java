package io.split.android.client.storage.cipher;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * JVM stand-in for the Android provider: AES/CBC with a fixed key and an IV taken from the first
 * 16 bytes of an SDK key. Counts the ciphers it hands out and records what is released to it.
 * Subclasses override the protected factories to hand out mocks.
 */
public class JceCbcCipherProvider implements CipherProvider {

    public static final String API_KEY = "abcdefghijklmnopqrstuvwxyz012345";
    public static final byte[] KEY = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);
    public static final byte[] OTHER_KEY = "fedcba9876543210".getBytes(StandardCharsets.UTF_8);

    private static final String SPEC = "AES/CBC/PKCS5Padding";

    public final AtomicInteger decryptionCalls = new AtomicInteger();
    public final AtomicInteger encryptionCalls = new AtomicInteger();
    private final List<Cipher> mReleased = Collections.synchronizedList(new ArrayList<>());

    private final byte[] mKey;
    private final byte[] mIv;

    public JceCbcCipherProvider() {
        this(KEY, API_KEY);
    }

    public JceCbcCipherProvider(byte[] key, String apiKey) {
        mKey = key;
        mIv = Arrays.copyOf(apiKey.getBytes(StandardCharsets.UTF_8), 16);
    }

    @Override
    public Cipher getEncryptionCipher() {
        encryptionCalls.incrementAndGet();
        return createEncryptionCipher();
    }

    @Override
    public Cipher getDecryptionCipher() {
        decryptionCalls.incrementAndGet();
        return createDecryptionCipher();
    }

    @Override
    public void release(Cipher cipher) {
        mReleased.add(cipher);
    }

    /** Everything passed to release, in order, nulls included. */
    public List<Cipher> released() {
        synchronized (mReleased) {
            return new ArrayList<>(mReleased);
        }
    }

    public Cipher newCipher(int mode) {
        try {
            Cipher cipher = Cipher.getInstance(SPEC);
            cipher.init(mode, new SecretKeySpec(mKey, "AES"), new IvParameterSpec(mIv));
            return cipher;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Ciphertext of exactly the given blocks, with no padding added: it decrypts to those bytes. */
    public byte[] encryptWithoutPadding(byte[] plain) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(mKey, "AES"), new IvParameterSpec(mIv));
            return cipher.doFinal(plain);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    protected Cipher createEncryptionCipher() {
        return newCipher(Cipher.ENCRYPT_MODE);
    }

    protected Cipher createDecryptionCipher() {
        return newCipher(Cipher.DECRYPT_MODE);
    }
}
