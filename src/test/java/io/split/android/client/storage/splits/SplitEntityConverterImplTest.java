package io.split.android.client.storage.splits;

import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;

import io.split.android.client.dtos.Split;
import io.split.android.client.storage.db.SplitEntity;

public class SplitEntityConverterImplTest {

    @Mock
    private SplitListTransformer<SplitEntity, Split> mEntityToSplitTransformer;
    @Mock
    private SplitListTransformer<Split, SplitEntity> mSplitToEntityTransformer;
    private SplitEntityConverterImpl mConverter;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mConverter = new SplitEntityConverterImpl(mEntityToSplitTransformer, mSplitToEntityTransformer);
    }

    @Test
    public void getFromEntityListDelegatesToTransformer() {
        ArrayList<SplitEntity> inputList = new ArrayList<>();
        SplitEntity splitEntity = new SplitEntity();
        splitEntity.setBody("{}");
        splitEntity.setName("test");
        inputList.add(splitEntity);
        mConverter.getFromEntityList(inputList);

        verify(mEntityToSplitTransformer).transform(inputList);
    }

    @Test
    public void getFromSplitListDelegatesToTransformer() {
        ArrayList<Split> inputList = new ArrayList<>();
        mConverter.getFromSplitList(inputList);

        verify(mSplitToEntityTransformer).transform(inputList);
    }
}
