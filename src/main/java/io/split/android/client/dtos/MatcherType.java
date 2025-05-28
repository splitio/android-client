package io.split.android.client.dtos;

import com.google.gson.annotations.SerializedName;

public enum MatcherType {
    @SerializedName("ALL_KEYS")
    ALL_KEYS,
    @SerializedName("IN_SEGMENT")
    IN_SEGMENT,
    @SerializedName("IN_LARGE_SEGMENT")
    IN_LARGE_SEGMENT,
    @SerializedName("WHITELIST")
    WHITELIST,

    /* Numeric Matcher */
    @SerializedName("EQUAL_TO")
    EQUAL_TO,
    @SerializedName("GREATER_THAN_OR_EQUAL_TO")
    GREATER_THAN_OR_EQUAL_TO,
    @SerializedName("LESS_THAN_OR_EQUAL_TO")
    LESS_THAN_OR_EQUAL_TO,
    @SerializedName("BETWEEN")
    BETWEEN,

    /* Set Matcher */
    @SerializedName("EQUAL_TO_SET")
    EQUAL_TO_SET,
    @SerializedName("CONTAINS_ANY_OF_SET")
    CONTAINS_ANY_OF_SET,
    @SerializedName("CONTAINS_ALL_OF_SET")
    CONTAINS_ALL_OF_SET,
    @SerializedName("PART_OF_SET")
    PART_OF_SET,

    /* String Matcher */
    @SerializedName("STARTS_WITH")
    STARTS_WITH,
    @SerializedName("ENDS_WITH")
    ENDS_WITH,
    @SerializedName("CONTAINS_STRING")
    CONTAINS_STRING,
    @SerializedName("MATCHES_STRING")
    MATCHES_STRING,

    /* Boolean Matcher */
    @SerializedName("EQUAL_TO_BOOLEAN")
    EQUAL_TO_BOOLEAN,

    /* Dependency Matcher */
    @SerializedName("IN_SPLIT_TREATMENT")
    IN_SPLIT_TREATMENT,

    /* Semver */
    @SerializedName("EQUAL_TO_SEMVER")
    EQUAL_TO_SEMVER,
    @SerializedName("GREATER_THAN_OR_EQUAL_TO_SEMVER")
    GREATER_THAN_OR_EQUAL_TO_SEMVER,
    @SerializedName("LESS_THAN_OR_EQUAL_TO_SEMVER")
    LESS_THAN_OR_EQUAL_TO_SEMVER,
    @SerializedName("BETWEEN_SEMVER")
    BETWEEN_SEMVER,
    @SerializedName("IN_LIST_SEMVER")
    IN_LIST_SEMVER,

    @SerializedName("IN_RULE_BASED_SEGMENT")
    IN_RULE_BASED_SEGMENT,
}
