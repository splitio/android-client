package io.split.android.client.storage.splits;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.Split;
import io.split.android.client.service.executor.parallel.SplitParallelTaskExecutor;
import io.split.android.client.storage.cipher.CBCCipher;
import io.split.android.client.storage.cipher.JceCbcCipherProvider;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.SplitEntity;

public class SplitEntityToSplitTransformerTest {

    @Mock
    private SplitParallelTaskExecutor<List<Split>> mSplitTaskExecutor;
    @Mock
    private SplitCipher mSplitCipher;
    private SplitEntityToSplitTransformer mConverter;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mSplitTaskExecutor.getAvailableThreads()).thenReturn(2);
        when(mSplitCipher.decrypt(any())).then((Answer<String>) invocation -> (String) invocation.getArguments()[0]);

        mConverter = new SplitEntityToSplitTransformer(mSplitCipher);
    }

    @Test
    public void amountOfSplitsEqualsAmountOfEntities() {
        when(mSplitTaskExecutor.getAvailableThreads()).thenReturn(4);
        List<SplitEntity> mockEntities = getMockEntities(3);
        Map<String, SplitEntity> map = new HashMap<>();
        for (SplitEntity entity : mockEntities) {
            map.put(entity.getName(), entity);
        }

        List<Split> splits = mConverter.transform(map);

        assertEquals(3, splits.size());
    }

    @Test
    public void amountOfSplitsEqualsAmountOfEntitiesWhenParallel() {
        when(mSplitTaskExecutor.getAvailableThreads()).thenReturn(2);
        List<SplitEntity> mockEntities = getMockEntities(3);
        Map<String, SplitEntity> map = new HashMap<>();
        for (SplitEntity entity : mockEntities) {
            map.put(entity.getName(), entity);
        }
        when(mSplitTaskExecutor.execute(any())).thenReturn(
                Arrays.asList(Collections.singletonList(new Split()),
                        Collections.singletonList(new Split()),
                        Collections.singletonList(new Split())));

        List<Split> splits = mConverter.transform(map);

        assertEquals(3, splits.size());
    }

    @Test
    public void transformingNullReturnsEmptyList() {
        when(mSplitTaskExecutor.getAvailableThreads()).thenReturn(4);

        List<Split> splits = mConverter.transform((Map<String, SplitEntity>) null);

        assertEquals(0, splits.size());
    }

    @Test
    public void valuesAreIgnoredWhenCipherThrowsException() {
        when(mSplitTaskExecutor.getAvailableThreads()).thenReturn(4);
        List<SplitEntity> mockEntities = getMockEntities(3);
        when(mSplitCipher.decrypt(any())).thenReturn(null);

        List<Split> splits = mConverter.transform(mockEntities);

        assertEquals(0, splits.size());
    }

    @Test
    public void cbcCipherUsesOneOperationPerBatch() {
        JceCbcCipherProvider provider = new JceCbcCipherProvider();
        CBCCipher cipher = new CBCCipher(provider);
        Map<String, SplitEntity> stored = new LinkedHashMap<>();
        for (SplitEntity entity : getMockEntities(50)) {
            SplitEntity encrypted = new SplitEntity();
            encrypted.setName(cipher.encrypt(entity.getName()));
            encrypted.setBody(cipher.encrypt(entity.getBody()));
            stored.put(encrypted.getName(), encrypted);
        }
        List<String> expectedNames = new ArrayList<>();
        List<String> expectedBodies = new ArrayList<>();
        for (Map.Entry<String, SplitEntity> entry : stored.entrySet()) {
            expectedNames.add(cipher.decrypt(entry.getKey()));
            expectedBodies.add(cipher.decrypt(entry.getValue().getBody()));
        }
        provider.decryptionCalls.set(0);

        List<Split> splits = new SplitEntityToSplitTransformer(cipher).transform(stored);

        assertEquals(1, provider.decryptionCalls.get());
        assertEquals(50, splits.size());
        for (int i = 0; i < splits.size(); i++) {
            assertEquals(expectedNames.get(i), splits.get(i).name);
            assertEquals(expectedBodies.get(i), splits.get(i).json);
        }
    }

    @Test
    public void rowsWithUndecryptableNameOrBodyAreSkippedAndTheRestStayPaired() {
        JceCbcCipherProvider provider = new JceCbcCipherProvider();
        CBCCipher cipher = new CBCCipher(provider);
        byte[] corrupt = new byte[16];
        java.util.Arrays.fill(corrupt, (byte) 'z');
        corrupt[15] = 5;
        String undecryptable = java.util.Base64.getEncoder().encodeToString(provider.encryptWithoutPadding(corrupt));
        Map<String, SplitEntity> stored = new LinkedHashMap<>();
        stored.put(cipher.encrypt("one"), entityWithBody(cipher.encrypt("body-one")));
        stored.put(undecryptable, entityWithBody(cipher.encrypt("body-lost-name")));
        stored.put(cipher.encrypt("three"), entityWithBody(cipher.encrypt("body-three")));
        stored.put(cipher.encrypt("four"), entityWithBody(undecryptable));
        stored.put(cipher.encrypt("skipped"), null);
        stored.put(cipher.encrypt("six"), entityWithBody(cipher.encrypt("body-six")));

        List<Split> splits = new SplitEntityToSplitTransformer(cipher).transform(stored);

        assertEquals(3, splits.size());
        assertEquals("one", splits.get(0).name);
        assertEquals("body-one", splits.get(0).json);
        assertEquals("three", splits.get(1).name);
        assertEquals("body-three", splits.get(1).json);
        assertEquals("six", splits.get(2).name);
        assertEquals("body-six", splits.get(2).json);
    }

    private static SplitEntity entityWithBody(String body) {
        SplitEntity entity = new SplitEntity();
        entity.setBody(body);
        return entity;
    }

    private List<SplitEntity> getMockEntities(int amount) {
        ArrayList<SplitEntity> entities = new ArrayList<>();
        String jsonTemplate = "{\"name\":\"%s\", \"changeNumber\": %d}";
        long initialChangeNumber = 9999;

        for (int i = 0; i < amount; i++) {
            String splitName = "split-" + i;
            SplitEntity entity = new SplitEntity();
            entity.setName(splitName);
            entity.setBody(String.format(jsonTemplate, splitName, initialChangeNumber - i));
            entities.add(entity);
        }

        return entities;
    }
}
