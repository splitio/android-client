package io.split.android.client.dtos;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

public class ExcludedSegment {

    private static final String TYPE_LARGE = "large";
    private static final String TYPE_STANDARD = "standard";
    private static final String TYPE_RULE_BASED = "rule-based";

    @SerializedName("type")
    private String mType;

    @SerializedName("name")
    private String mName;

    public ExcludedSegment() {}

    private ExcludedSegment(String name, String type) {
        mName = name;
        mType = type;
    }

    @VisibleForTesting
    public static ExcludedSegment standard(String name) {
        return new ExcludedSegment(name, TYPE_STANDARD);
    }

    @VisibleForTesting
    public static ExcludedSegment large(String name) {
        return new ExcludedSegment(name, TYPE_LARGE);
    }

    public static ExcludedSegment ruleBased(String name) {
        return new ExcludedSegment(name, TYPE_RULE_BASED);
    }

    public String getName() {
        return mName;
    }

    public boolean isStandard() {
        return TYPE_STANDARD.equals(mType);
    }

    public boolean isLarge() {
        return TYPE_LARGE.equals(mType);
    }

    public boolean isRuleBased() {
        return TYPE_RULE_BASED.equals(mType);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ExcludedSegment)) {
            return false;
        }
        ExcludedSegment other = (ExcludedSegment) obj;
        return mName.equals(other.mName) && mType.equals(other.mType);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + mName.hashCode();
        result = 31 * result + mType.hashCode();
        return result;
    }
}
