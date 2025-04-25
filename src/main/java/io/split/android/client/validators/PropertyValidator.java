package io.split.android.client.validators;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

public interface PropertyValidator {

    Result validate(Map<String, Object> properties);

    Result validate(Map<String, Object> properties, int initialSizeInBytes, String validationTag);

    class Result {

        private final boolean mIsValid;
        @Nullable
        private final Map<String, Object> mValidatedProperties;
        private final int mSizeInBytes;
        @Nullable
        private final String mErrorMessage;

        private Result(boolean isValid, Map<String, Object> properties, int sizeInBytes, String errorMessage) {
            mIsValid = isValid;
            mValidatedProperties = properties;
            mSizeInBytes = sizeInBytes;
            mErrorMessage = errorMessage;
        }

        public boolean isValid() {
            return mIsValid;
        }

        @Nullable
        public Map<String, Object> getValidatedProperties() {
            return mValidatedProperties;
        }

        public int getSizeInBytes() {
            return mSizeInBytes;
        }

        @Nullable
        public String getErrorMessage() {
            return mErrorMessage;
        }

        @NonNull
        public static Result valid(Map<String, Object> properties, int sizeInBytes) {
            return new Result(true, properties, sizeInBytes, null);
        }

        @NonNull
        public static Result invalid(String errorMessage, int sizeInBytes) {
            return new Result(false, null, sizeInBytes, errorMessage);
        }
    }
}
