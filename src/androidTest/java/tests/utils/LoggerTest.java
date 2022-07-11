package tests.utils;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fake.LogPrinterStub;
import io.split.android.client.utils.logger.LogPrinter;
import io.split.android.client.utils.logger.Logger;
import io.split.android.client.utils.logger.SplitLogLevel;

public class LoggerTest {

    Context mContext;
    LogPrinterStub printer;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        printer = new LogPrinterStub();
        Logger.instance().setPrinter(printer);
    }

    @Test
    public void testNone() {
        Logger.instance().setLevel(SplitLogLevel.NONE);

        logAll();

        Assert.assertFalse(printer.isCalled(SplitLogLevel.VERBOSE));
        Assert.assertFalse(printer.isCalled(SplitLogLevel.DEBUG));
        Assert.assertFalse(printer.isCalled(SplitLogLevel.INFO));
        Assert.assertFalse(printer.isCalled(SplitLogLevel.WARNING));
        Assert.assertFalse(printer.isCalled(SplitLogLevel.ERROR));
        Assert.assertFalse(printer.isCalled(SplitLogLevel.ASSERT));
    }

    @Test
    public void testVerbose() {
        Logger.instance().setLevel(SplitLogLevel.VERBOSE);

        logAll();

        Assert.assertTrue(printer.isCalled(SplitLogLevel.VERBOSE));
        Assert.assertFalse(printer.isCalled(SplitLogLevel.DEBUG));
        Assert.assertFalse(printer.isCalled(SplitLogLevel.INFO));
        Assert.assertFalse(printer.isCalled(SplitLogLevel.WARNING));
        Assert.assertFalse(printer.isCalled(SplitLogLevel.ERROR));
        Assert.assertFalse(printer.isCalled(SplitLogLevel.ASSERT));
    }

    @Test
    public void testDebug() {
        Logger.instance().setLevel(SplitLogLevel.DEBUG);

        logAll();

        Assert.assertTrue(printer.isCalled(SplitLogLevel.VERBOSE));
        Assert.assertTrue(printer.isCalled(SplitLogLevel.DEBUG));
        Assert.assertFalse(printer.isCalled(SplitLogLevel.INFO));
        Assert.assertFalse(printer.isCalled(SplitLogLevel.WARNING));
        Assert.assertFalse(printer.isCalled(SplitLogLevel.ERROR));
        Assert.assertFalse(printer.isCalled(SplitLogLevel.ASSERT));
    }

    @Test
    public void testInfo() {
        Logger.instance().setLevel(SplitLogLevel.INFO);

        logAll();

        Assert.assertTrue(printer.isCalled(SplitLogLevel.VERBOSE));
        Assert.assertTrue(printer.isCalled(SplitLogLevel.DEBUG));
        Assert.assertTrue(printer.isCalled(SplitLogLevel.INFO));
        Assert.assertFalse(printer.isCalled(SplitLogLevel.WARNING));
        Assert.assertFalse(printer.isCalled(SplitLogLevel.ERROR));
        Assert.assertFalse(printer.isCalled(SplitLogLevel.ASSERT));
    }

    @Test
    public void testWarning() {
        Logger.instance().setLevel(SplitLogLevel.WARNING);

        logAll();

        Assert.assertTrue(printer.isCalled(SplitLogLevel.VERBOSE));
        Assert.assertTrue(printer.isCalled(SplitLogLevel.DEBUG));
        Assert.assertTrue(printer.isCalled(SplitLogLevel.INFO));
        Assert.assertTrue(printer.isCalled(SplitLogLevel.WARNING));
        Assert.assertFalse(printer.isCalled(SplitLogLevel.ERROR));
        Assert.assertFalse(printer.isCalled(SplitLogLevel.ASSERT));
    }

    @Test
    public void testError() {
        Logger.instance().setLevel(SplitLogLevel.ERROR);

        logAll();

        Assert.assertTrue(printer.isCalled(SplitLogLevel.VERBOSE));
        Assert.assertTrue(printer.isCalled(SplitLogLevel.DEBUG));
        Assert.assertTrue(printer.isCalled(SplitLogLevel.INFO));
        Assert.assertTrue(printer.isCalled(SplitLogLevel.WARNING));
        Assert.assertTrue(printer.isCalled(SplitLogLevel.ERROR));
        Assert.assertFalse(printer.isCalled(SplitLogLevel.ASSERT));
    }

    @Test
    public void testAssert() {
        Logger.instance().setLevel(SplitLogLevel.ASSERT);

        logAll();

        Assert.assertTrue(printer.isCalled(SplitLogLevel.VERBOSE));
        Assert.assertTrue(printer.isCalled(SplitLogLevel.DEBUG));
        Assert.assertTrue(printer.isCalled(SplitLogLevel.INFO));
        Assert.assertTrue(printer.isCalled(SplitLogLevel.WARNING));
        Assert.assertTrue(printer.isCalled(SplitLogLevel.ERROR));
        Assert.assertTrue(printer.isCalled(SplitLogLevel.ASSERT));
    }

    void logAll() {
        Logger.v("log");
        Logger.d("log");
        Logger.i("log");
        Logger.w("log");
        Logger.e("log");
        Logger.wtf("log");
    }
}
