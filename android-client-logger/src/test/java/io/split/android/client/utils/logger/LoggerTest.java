package io.split.android.client.utils.logger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LoggerTest {

    LogPrinterStub printer;

    @Before
    public void setUp() {
        printer = new LogPrinterStub();
        Logger.instance().setPrinter(printer);
    }

    @Test
    public void testNone() {
        Logger.instance().setLevel(Level.NONE);

        logAll();

        Assert.assertFalse(printer.isCalled(Level.VERBOSE));
        Assert.assertFalse(printer.isCalled(Level.DEBUG));
        Assert.assertFalse(printer.isCalled(Level.INFO));
        Assert.assertFalse(printer.isCalled(Level.WARNING));
        Assert.assertFalse(printer.isCalled(Level.ERROR));
        Assert.assertFalse(printer.isCalled(Level.ASSERT));
    }

    @Test
    public void testVerbose() {
        Logger.instance().setLevel(Level.VERBOSE);

        logAll();

        Assert.assertTrue(printer.isCalled(Level.VERBOSE));
        Assert.assertTrue(printer.isCalled(Level.DEBUG));
        Assert.assertTrue(printer.isCalled(Level.INFO));
        Assert.assertTrue(printer.isCalled(Level.WARNING));
        Assert.assertTrue(printer.isCalled(Level.ERROR));
        Assert.assertTrue(printer.isCalled(Level.ASSERT));
    }

    @Test
    public void testDebug() {
        Logger.instance().setLevel(Level.DEBUG);

        logAll();

        Assert.assertFalse(printer.isCalled(Level.VERBOSE));
        Assert.assertTrue(printer.isCalled(Level.DEBUG));
        Assert.assertTrue(printer.isCalled(Level.INFO));
        Assert.assertTrue(printer.isCalled(Level.WARNING));
        Assert.assertTrue(printer.isCalled(Level.ERROR));
        Assert.assertTrue(printer.isCalled(Level.ASSERT));
    }

    @Test
    public void testInfo() {
        Logger.instance().setLevel(Level.INFO);

        logAll();

        Assert.assertFalse(printer.isCalled(Level.VERBOSE));
        Assert.assertFalse(printer.isCalled(Level.DEBUG));
        Assert.assertTrue(printer.isCalled(Level.INFO));
        Assert.assertTrue(printer.isCalled(Level.WARNING));
        Assert.assertTrue(printer.isCalled(Level.ERROR));
        Assert.assertTrue(printer.isCalled(Level.ASSERT));
    }

    @Test
    public void testWarning() {
        Logger.instance().setLevel(Level.WARNING);

        logAll();

        Assert.assertFalse(printer.isCalled(Level.VERBOSE));
        Assert.assertFalse(printer.isCalled(Level.DEBUG));
        Assert.assertFalse(printer.isCalled(Level.INFO));
        Assert.assertTrue(printer.isCalled(Level.WARNING));
        Assert.assertTrue(printer.isCalled(Level.ERROR));
        Assert.assertTrue(printer.isCalled(Level.ASSERT));
    }

    @Test
    public void testError() {
        Logger.instance().setLevel(Level.ERROR);

        logAll();

        Assert.assertFalse(printer.isCalled(Level.VERBOSE));
        Assert.assertFalse(printer.isCalled(Level.DEBUG));
        Assert.assertFalse(printer.isCalled(Level.INFO));
        Assert.assertFalse(printer.isCalled(Level.WARNING));
        Assert.assertTrue(printer.isCalled(Level.ERROR));
        Assert.assertTrue(printer.isCalled(Level.ASSERT));
    }

    @Test
    public void testAssert() {
        Logger.instance().setLevel(Level.ASSERT);

        logAll();

        Assert.assertFalse(printer.isCalled(Level.VERBOSE));
        Assert.assertFalse(printer.isCalled(Level.DEBUG));
        Assert.assertFalse(printer.isCalled(Level.INFO));
        Assert.assertFalse(printer.isCalled(Level.WARNING));
        Assert.assertFalse(printer.isCalled(Level.ERROR));
        Assert.assertTrue(printer.isCalled(Level.ASSERT));
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
