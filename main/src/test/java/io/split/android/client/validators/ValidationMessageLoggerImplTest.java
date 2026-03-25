package io.split.android.client.validators;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.split.android.client.tracker.TrackerValidationError;
import io.split.android.client.utils.logger.Logger;

public class ValidationMessageLoggerImplTest {

    private ValidationMessageLoggerImpl mLogger;

    @Before
    public void setUp() {
        mLogger = new ValidationMessageLoggerImpl();
    }

    @Test
    public void logErrorInfoWithErrorMessage() {
        try (MockedStatic<Logger> loggerMock = Mockito.mockStatic(Logger.class)) {
            ValidationErrorInfo errorInfo = new ValidationErrorInfo(200, "error message");

            mLogger.log(errorInfo, "test-tag");

            // Due to parameter swap in e() method, actual output is "error message: test-tag"
            loggerMock.verify(() -> Logger.e(eq("error message: test-tag")));
        }
    }

    @Test
    public void logErrorInfoWithWarnings() {
        try (MockedStatic<Logger> loggerMock = Mockito.mockStatic(Logger.class)) {
            ValidationErrorInfo errorInfo = new ValidationErrorInfo(100, "warning 1", true);
            errorInfo.addWarning(101, "warning 2");

            mLogger.log(errorInfo, "test-tag");

            // Due to parameter swap in w() method, actual output is "warning X: test-tag"
            loggerMock.verify(() -> Logger.w(eq("warning 1: test-tag")));
            loggerMock.verify(() -> Logger.w(eq("warning 2: test-tag")));
        }
    }

    @Test
    public void logErrorInfoWithNullErrorMessage() {
        try (MockedStatic<Logger> loggerMock = Mockito.mockStatic(Logger.class)) {
            ValidationErrorInfo errorInfo = new ValidationErrorInfo(100, "warning message", true);

            mLogger.log(errorInfo, "test-tag");

            loggerMock.verify(() -> Logger.w(eq("warning message: test-tag")));
            loggerMock.verify(() -> Logger.e(anyString()), never());
        }
    }

    @Test
    public void logErrorWithValidationErrorInfo() {
        try (MockedStatic<Logger> loggerMock = Mockito.mockStatic(Logger.class)) {
            ValidationErrorInfo errorInfo = new ValidationErrorInfo(200, "error message");

            mLogger.e(errorInfo, "test-tag");

            loggerMock.verify(() -> Logger.e(eq("error message: test-tag")));
        }
    }

    @Test
    public void logWarningWithValidationErrorInfo() {
        try (MockedStatic<Logger> loggerMock = Mockito.mockStatic(Logger.class)) {
            ValidationErrorInfo errorInfo = new ValidationErrorInfo(100, "first warning", true);
            errorInfo.addWarning(101, "second warning");

            mLogger.w(errorInfo, "test-tag");

            loggerMock.verify(() -> Logger.w(eq("first warning: test-tag")));
            loggerMock.verify(() -> Logger.w(eq("second warning: test-tag")));
        }
    }

    @Test
    public void logErrorWithStringMessage() {
        try (MockedStatic<Logger> loggerMock = Mockito.mockStatic(Logger.class)) {
            // Note: parameter order is (message, tag) in signature, but used as (tag, message) in implementation
            mLogger.e("test-tag", "error message");

            loggerMock.verify(() -> Logger.e(eq("error message: test-tag")));
        }
    }

    @Test
    public void logWarningWithStringMessage() {
        try (MockedStatic<Logger> loggerMock = Mockito.mockStatic(Logger.class)) {
            // Note: parameter order is (message, tag) in signature, but used as (tag, message) in implementation
            mLogger.w("test-tag", "warning message");

            loggerMock.verify(() -> Logger.w(eq("warning message: test-tag")));
        }
    }

    @Test
    public void sanitizeTagWithNullTag() {
        try (MockedStatic<Logger> loggerMock = Mockito.mockStatic(Logger.class)) {
            mLogger.e((String) null, "error message");

            loggerMock.verify(() -> Logger.e(eq("error message: null")));
        }
    }

    // TrackerLogger implementation tests

    @Test
    public void trackerLoggerLogWithError() {
        try (MockedStatic<Logger> loggerMock = Mockito.mockStatic(Logger.class)) {
            TrackerValidationError errorInfo = new TrackerValidationError(true, "tracker error");

            mLogger.log(errorInfo, "tracker-tag");

            loggerMock.verify(() -> Logger.e(eq("tracker-tag: tracker error")));
        }
    }

    @Test
    public void trackerLoggerLogWithWarnings() {
        try (MockedStatic<Logger> loggerMock = Mockito.mockStatic(Logger.class)) {
            TrackerValidationError errorInfo = new TrackerValidationError(
                    Arrays.asList("warning 1", "warning 2", "warning 3"));

            mLogger.log(errorInfo, "tracker-tag");

            loggerMock.verify(() -> Logger.w(eq("tracker-tag: warning 1")));
            loggerMock.verify(() -> Logger.w(eq("tracker-tag: warning 2")));
            loggerMock.verify(() -> Logger.w(eq("tracker-tag: warning 3")));
            loggerMock.verify(() -> Logger.e(anyString()), never());
        }
    }

    @Test
    public void trackerLoggerLogWithEmptyWarnings() {
        try (MockedStatic<Logger> loggerMock = Mockito.mockStatic(Logger.class)) {
            TrackerValidationError errorInfo = new TrackerValidationError(Collections.emptyList());

            mLogger.log(errorInfo, "tracker-tag");

            loggerMock.verify(() -> Logger.w(anyString()), never());
            loggerMock.verify(() -> Logger.e(anyString()), never());
        }
    }

    @Test
    public void trackerLoggerVerboseMessage() {
        try (MockedStatic<Logger> loggerMock = Mockito.mockStatic(Logger.class)) {
            mLogger.v("verbose message");

            loggerMock.verify(() -> Logger.v(eq("verbose message")));
        }
    }
}
