package io.split.android.client.storage.splits;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.util.Log;
import io.split.android.client.dtos.Split;
import io.split.android.client.utils.logger.LogPrinterStub;
import io.split.android.client.utils.logger.Logger;
import io.split.android.client.utils.logger.SplitLogLevel;

public class SplitsStorageImplLoadLogTest {

    private LogPrinterStub mLogPrinter;
    private SplitsStorageImpl mStorage;

    @Before
    public void setUp() {
        PersistentSplitsStorage persistent = mock(PersistentSplitsStorage.class);
        Split split = new Split();
        split.name = "flag";
        when(persistent.getSnapshot()).thenReturn(new SplitsSnapshot(
                Collections.singletonList(split), 1L, 2L, "", "",
                new HashMap<>(), new HashMap<>()));
        mStorage = new SplitsStorageImpl(persistent);
        mLogPrinter = new LogPrinterStub();
        Logger.instance().setPrinter(mLogPrinter);
    }

    private List<String> verboseMessages() {
        return new ArrayList<>(mLogPrinter.getLoggedMessages().get(Log.VERBOSE));
    }

    @After
    public void tearDown() {
        Logger.instance().setLevel(SplitLogLevel.NONE);
    }

    @Test
    public void loadLocalLogsOneVerboseTimingMessage() {
        Logger.instance().setLevel(SplitLogLevel.VERBOSE);

        mStorage.loadLocal();

        List<String> verbose = verboseMessages();
        assertEquals(1, verbose.size());
        assertTrue(verbose.get(0).contains("loadLocal"));
        assertTrue(verbose.get(0).contains("(1 flags)"));
    }

    @Test
    public void loadLocalLogsNothingWhenLevelIsNone() {
        Logger.instance().setLevel(SplitLogLevel.NONE);

        mStorage.loadLocal();

        assertEquals(0, verboseMessages().size());
    }
}
