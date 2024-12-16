package io.split.android.client.dtos;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.split.android.client.utils.Json;

public class SplitDeserializationTest {

    @Test
    public void trackImpressionsDefaultsToTrueWhenNotPresentInSplit() {
        assertTrue(Json.fromJson(getTestSplit(null), Split.class).trackImpressions);
    }

    @Test
    public void trackImpressionsValueIsParsedCorrectly() {
        assertTrue(Json.fromJson(getTestSplit(true), Split.class).trackImpressions);
        assertFalse(Json.fromJson(getTestSplit(false), Split.class).trackImpressions);
    }

    private String getTestSplit(Boolean trackImpressions) {
        return "{\n" +
                ((trackImpressions != null) ? "\"trackImpressions\": " + trackImpressions + ",\n"  : "") +
                "      \"trafficTypeName\": \"client\",\n" +
                "      \"name\": \"workm\",\n" +
                "      \"trafficAllocation\": 100,\n" +
                "      \"trafficAllocationSeed\": 147392224,\n" +
                "      \"seed\": 524417105,\n" +
                "      \"status\": \"ACTIVE\",\n" +
                "      \"killed\": false,\n" +
                "      \"defaultTreatment\": \"on\",\n" +
                "      \"changeNumber\": 1602796638344,\n" +
                "      \"algo\": 2,\n" +
                "      \"configurations\": {},\n" +
                "      \"conditions\": [\n" +
                "        {\n" +
                "          \"conditionType\": \"ROLLOUT\",\n" +
                "          \"matcherGroup\": {\n" +
                "            \"combiner\": \"AND\",\n" +
                "            \"matchers\": [\n" +
                "              {\n" +
                "                \"keySelector\": {\n" +
                "                  \"trafficType\": \"client\",\n" +
                "                  \"attribute\": null\n" +
                "                },\n" +
                "                \"matcherType\": \"IN_SEGMENT\",\n" +
                "                \"negate\": false,\n" +
                "                \"userDefinedSegmentMatcherData\": {\n" +
                "                  \"segmentName\": \"new_segment\"\n" +
                "                },\n" +
                "                \"whitelistMatcherData\": null,\n" +
                "                \"unaryNumericMatcherData\": null,\n" +
                "                \"betweenMatcherData\": null,\n" +
                "                \"booleanMatcherData\": null,\n" +
                "                \"dependencyMatcherData\": null,\n" +
                "                \"stringMatcherData\": null\n" +
                "              }\n" +
                "            ]\n" +
                "          },\n" +
                "          \"partitions\": [\n" +
                "            {\n" +
                "              \"treatment\": \"on\",\n" +
                "              \"size\": 0\n" +
                "            },\n" +
                "            {\n" +
                "              \"treatment\": \"off\",\n" +
                "              \"size\": 0\n" +
                "            },\n" +
                "            {\n" +
                "              \"treatment\": \"free\",\n" +
                "              \"size\": 100\n" +
                "            },\n" +
                "            {\n" +
                "              \"treatment\": \"conta\",\n" +
                "              \"size\": 0\n" +
                "            }\n" +
                "          ],\n" +
                "          \"label\": \"in segment new_segment\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"conditionType\": \"ROLLOUT\",\n" +
                "          \"matcherGroup\": {\n" +
                "            \"combiner\": \"AND\",\n" +
                "            \"matchers\": [\n" +
                "              {\n" +
                "                \"keySelector\": {\n" +
                "                  \"trafficType\": \"client\",\n" +
                "                  \"attribute\": null\n" +
                "                },\n" +
                "                \"matcherType\": \"ALL_KEYS\",\n" +
                "                \"negate\": false,\n" +
                "                \"userDefinedSegmentMatcherData\": null,\n" +
                "                \"whitelistMatcherData\": null,\n" +
                "                \"unaryNumericMatcherData\": null,\n" +
                "                \"betweenMatcherData\": null,\n" +
                "                \"booleanMatcherData\": null,\n" +
                "                \"dependencyMatcherData\": null,\n" +
                "                \"stringMatcherData\": null\n" +
                "              }\n" +
                "            ]\n" +
                "          },\n" +
                "          \"partitions\": [\n" +
                "            {\n" +
                "              \"treatment\": \"on\",\n" +
                "              \"size\": 100\n" +
                "            },\n" +
                "            {\n" +
                "              \"treatment\": \"off\",\n" +
                "              \"size\": 0\n" +
                "            },\n" +
                "            {\n" +
                "              \"treatment\": \"free\",\n" +
                "              \"size\": 0\n" +
                "            },\n" +
                "            {\n" +
                "              \"treatment\": \"conta\",\n" +
                "              \"size\": 0\n" +
                "            }\n" +
                "          ],\n" +
                "          \"label\": \"default rule\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }";
    }
}
