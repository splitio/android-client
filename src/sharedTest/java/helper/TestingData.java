package helper;

import io.split.android.client.service.sseclient.notifications.MySegmentChangeV2Notification;
import io.split.android.client.utils.Json;

public class TestingData {

    public final static String UNBOUNDED_NOTIFICATION = "{" +
            "\\\"type\\\": \\\"MY_SEGMENTS_UPDATE_V2\\\"," +
            "\\\"updateStrategy\\\": 0," +
            "\\\"compression\\\": 0," +
            "\\\"data\\\": \\\"\\\"," +
            "\\\"segmentName\\\": \\\"pepe\\\"," +
            "\\\"changeNumber\\\": 28" +
            "}";

    public final static String SEGMENT_REMOVAL_NOTIFICATION = "{" +
            "\\\"type\\\": \\\"MY_SEGMENTS_UPDATE_V2\\\"," +
            "\\\"updateStrategy\\\": 3," +
            "\\\"compression\\\": 0," +
            "\\\"data\\\": \\\"\\\"," +
            "\\\"segmentName\\\": \\\"segment1\\\"," +
            "\\\"changeNumber\\\": 28" +
            "}";

    public final static String BOUNDED_NOTIFICATION_ZLIB = "{" +
            "\"type\": \"MY_SEGMENTS_UPDATE_V2\"," +
            "\"updateStrategy\": 1," +
            "\"compression\": 2," +
            "\"data\": \"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\"" +
            "}";

    /**
      Key for unbounded payload Gzip
      603516ce-1243-400b-b919-0dce5d8aecfd
      88f8b33b-f858-4aea-bea2-a5f066bab3ce
      375903c8-6f62-4272-88f1-f8bcd304c7ae
      18c936ad-0cd2-490d-8663-03eaa23a5ef1
      bfd4a824-0cde-4f11-9700-2b4c5ad6f719
      4588c4f6-3d18-452a-bc4a-47d7abfd23df
      42bcfe02-d268-472f-8ed5-e6341c33b4f7
      2a7cae0e-85a2-443e-9d7c-7157b7c5960a
      4b0b0467-3fe1-43d1-a3d5-937c0a5473b1
      09025e90-d396-433a-9292-acef23cf0ad1
      [11288179738259047283 10949366897533296036 9142072388263950989 51944159202969851
      8492584437244343049 11382796718859679607 11383137936375052427 17699699514337596928 17001541343685934583 8355202062888946034]
     */
    public final static String BOUNDED_NOTIFICATION_GZIP = "{" +
            "\"type\": \"MY_SEGMENTS_UPDATE_V2\"," +
            "\"updateStrategy\": 1," +
            "\"compression\": 2," +
            "\"data\": \"H4sIAAAAAAAA/2IYBfgAx0A7YBTgB4wD7YABAAID7QC6g5EYy8MEMA20A+gMFAbaAYMZDPXqlGWgHTAKRsEoGAWjgCzQQFjJkKqiiPAPAQAIAAD//5L7VQwAEAAA\"" +
            "}";

    public final static String ESCAPED_BOUNDED_NOTIFICATION_GZIP = "{" +
            "\\\"type\\\": \\\"MY_SEGMENTS_UPDATE_V2\\\"," +
            "\\\"updateStrategy\\\": 1," +
            "\\\"compression\\\": 2," +
            "\\\"data\\\": \\\"H4sIAAAAAAAA/2IYBfgAx0A7YBTgB4wD7YABAAID7QC6g5EYy8MEMA20A+gMFAbaAYMZDPXqlGWgHTAKRsEoGAWjgCzQQFjJkKqiiPAPAQAIAAD//5L7VQwAEAAA\\\"" +
            "}";

    /**
     * Keylist payload Gzip
     * {"a":[1573573083296714675,8482869187405483569],"r":[8031872927333060586,6829471020522910836]}
     * = a: [key1, key2] , r: [key3, key4]
     */
    public final static String KEY_LIST_NOTIFICATION_GZIP = "{" +
            "\"type\": \"MY_SEGMENTS_UPDATE_V2\"," +
            "\"updateStrategy\": 2," +
            "\"compression\": 1," +
            "\"data\": \"H4sIAAAAAAAA/wTAsRHDUAgD0F2ofwEIkPAqPhdZIW0uu/v97GPXHU004ULuMGrYR6XUbIjlXULPPse+dt1yhJibBODjrTmj3GJ4emduuDDP/w0AAP//18WLsl0AAAA=\"" +
            "}";

    public final static String ESCAPED_KEY_LIST_NOTIFICATION_GZIP = "{" +
            "\\\"type\\\": \\\"MY_SEGMENTS_UPDATE_V2\\\"," +
            "\\\"segmentName\\\": \\\"new_segment_added\\\"," +
            "\\\"updateStrategy\\\": 2," +
            "\\\"compression\\\": 1," +
            "\\\"data\\\": \\\"H4sIAAAAAAAA/wTAsRHDUAgD0F2ofwEIkPAqPhdZIW0uu/v97GPXHU004ULuMGrYR6XUbIjlXULPPse+dt1yhJibBODjrTmj3GJ4emduuDDP/w0AAP//18WLsl0AAAA=\\\"" +
            "}";

    public final static String DECOMPRESSED_KEY_LIST_PAYLOAD_GZIP = "{\"a\":[1573573083296714675,8482869187405483569],\"r\":[8031872927333060586,6829471020522910836]}";

    public static String encodedKeyListPayloadGzip() {
        return (Json.fromJson(KEY_LIST_NOTIFICATION_GZIP, MySegmentChangeV2Notification.class)).getData();
    }

    public static String encodedBoundedPayloadZlib() {
        return (Json.fromJson(BOUNDED_NOTIFICATION_ZLIB, MySegmentChangeV2Notification.class)).getData();
    }

    public static String encodedBoundedPayloadGzip() {
        return (Json.fromJson(BOUNDED_NOTIFICATION_GZIP, MySegmentChangeV2Notification.class)).getData();
    }
}
