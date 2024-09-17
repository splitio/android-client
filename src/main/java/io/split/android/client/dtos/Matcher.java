package io.split.android.client.dtos;

import com.google.gson.annotations.SerializedName;

/**
 * A leaf class representing a matcher.
 *
 */
public class Matcher {
    @SerializedName("keySelector")
    public KeySelector keySelector;
    @SerializedName("matcherType")
    public MatcherType matcherType;
    @SerializedName("negate")
    public boolean negate;
    @SerializedName("userDefinedSegmentMatcherData")
    public UserDefinedSegmentMatcherData userDefinedSegmentMatcherData;
    @SerializedName("userDefinedLargeSegmentMatcherData")
    public UserDefinedLargeSegmentMatcherData userDefinedLargeSegmentMatcherData;
    @SerializedName("whitelistMatcherData")
    public WhitelistMatcherData whitelistMatcherData;
    @SerializedName("unaryNumericMatcherData")
    public UnaryNumericMatcherData unaryNumericMatcherData;
    @SerializedName("betweenMatcherData")
    public BetweenMatcherData betweenMatcherData;
    @SerializedName("dependencyMatcherData")
    public DependencyMatcherData dependencyMatcherData;
    @SerializedName("booleanMatcherData")
    public Boolean booleanMatcherData;
    @SerializedName("stringMatcherData")
    public String stringMatcherData;
    @SerializedName("betweenStringMatcherData")
    public BetweenStringMatcherData betweenStringMatcherData;
}
