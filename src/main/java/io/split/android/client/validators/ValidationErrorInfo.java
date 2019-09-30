package io.split.android.client.validators;

import java.util.HashMap;
import java.util.Map;

public class ValidationErrorInfo {
    public static final int WARNING_SPLIT_NAME_SHOULD_BE_TRIMMED = 100;
    public static final int WARNING_TRAFFIC_TYPE_HAS_UPPERCASE_CHARS = 101;
    public static final int WARNING_TRAFFIC_TYPE_WITHOUT_SPLIT_IN_ENVIRONMENT = 102;

    static final int MIN_WARNING_CODE = WARNING_SPLIT_NAME_SHOULD_BE_TRIMMED;
    static final int MAX_WARNING_CODE = WARNING_TRAFFIC_TYPE_WITHOUT_SPLIT_IN_ENVIRONMENT;

    public static final int ERROR_SOME = 200;


    private Integer mError = null;
    private String mErrorMessage;
    private Map<Integer, String> mWarnings = new HashMap<>();

    @SuppressWarnings("SameParameterValue")
    ValidationErrorInfo(int code, String message) {
        this(code, message, false);
    }

    ValidationErrorInfo(int code, String message, boolean isWarning) {
        if(!isWarning){
            mError = Integer.valueOf(code);
            mErrorMessage = message;
        } else {
            mWarnings.put(Integer.valueOf(code), message);
        }
    }

    public Integer getError() {
        return mError;
    }

    public String getErrorMessage() {
        return mErrorMessage;
    }

    public Map<Integer, String> getWarnings() {
        return mWarnings;
    }

    public boolean isError() {
        return mError != null;
    }

    public void addWarning(int code, String message) {
        if(message != null) {
            mWarnings.put(Integer.valueOf(code), message);
        }
    }

    public boolean hasWarning(int code) {
        return mWarnings.get(Integer.valueOf(code)) != null;
    }

}
