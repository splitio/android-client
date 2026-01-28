package io.split.android.client.dtos;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TestImpressions {
    /* package private */ static final String FIELD_TEST_NAME = "f";
    /* package private */ static final String FIELD_KEY_IMPRESSIONS = "i";

    @SerializedName(FIELD_TEST_NAME)
    public String testName;

    @SerializedName(FIELD_KEY_IMPRESSIONS)
    public List<KeyImpression> keyImpressions;
}
