package helper;

import io.split.android.client.service.sseclient.notifications.MembershipNotification;
import io.split.android.client.utils.Json;

public class TestingData {

    public final static String UNBOUNDED_NOTIFICATION = "{" +
            "\\\"type\\\": \\\"MEMBERSHIPS_MS_UPDATE\\\"," +
            "\\\"u\\\": 0," +
            "\\\"c\\\": 0," +
            "\\\"d\\\": \\\"\\\"," +
            "\\\"n\\\": [\\\"pepe\\\"]," +
            "\\\"cn\\\": 28" +
            "}";

    public final static String SEGMENT_REMOVAL_NOTIFICATION = "{" +
            "\\\"type\\\": \\\"MEMBERSHIPS_MS_UPDATE\\\"," +
            "\\\"u\\\": 3," +
            "\\\"c\\\": 0," +
            "\\\"d\\\": \\\"\\\"," +
            "\\\"n\\\": [\\\"segment1\\\"]," +
            "\\\"cn\\\": 28" +
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
            "\"type\": \"MEMBERSHIPS_MS_UPDATE\"," +
            "\"u\": 1," +
            "\"c\": 1," +
            "\"d\": \"H4sIAAAAAAAA/2IYBfgAx0A7YBTgB4wD7YABAAID7QC6g5EYy8MEMA20A+gMFAbaAYMZDPXqlGWgHTAKRsEoGAWjgCzQQFjJkKqiiPAPAQAIAAD//5L7VQwAEAAA\"" +
            "}";

    public final static String ESCAPED_BOUNDED_NOTIFICATION_GZIP = "{" +
            "\\\"type\\\": \\\"MEMBERSHIPS_MS_UPDATE\\\"," +
            "\\\"u\\\": 1," +
            "\\\"c\\\": 1," +
            "\\\"d\\\": \\\"H4sIAAAAAAAA/2IYBfgAx0A7YBTgB4wD7YABAAID7QC6g5EYy8MEMA20A+gMFAbaAYMZDPXqlGWgHTAKRsEoGAWjgCzQQFjJkKqiiPAPAQAIAAD//5L7VQwAEAAA\\\"" +
            "}";

    public final static String BOUNDED_NOTIFICATION_ZLIB = "{" +
            "\"type\": \"MEMBERSHIPS_MS_UPDATE\"," +
            "\"u\": 1," +
            "\"c\": 2," +
            "\"d\": \"eJxiGAX4AMdAO2AU4AeMA+2AAQACA+0AuoORGMvDBDANtAPoDBQG2gGDGQz16pRloB0wCkbBKBgFo4As0EBYyZCqoojwDwEACAAA//+W/QFR\"" +
            "}";

    public final static String ESCAPED_BOUNDED_NOTIFICATION_ZLIB = "{" +
            "\\\"type\\\": \\\"MEMBERSHIPS_MS_UPDATE\\\"," +
            "\\\"u\\\": 1," +
            "\\\"c\\\": 2," +
            "\\\"d\\\": \\\"eJxiGAX4AMdAO2AU4AeMA+2AAQACA+0AuoORGMvDBDANtAPoDBQG2gGDGQz16pRloB0wCkbBKBgFo4As0EBYyZCqoojwDwEACAAA//+W/QFR\\\"" +
            "}";


    public final static String ESCAPED_BOUNDED_NOTIFICATION_MALFORMED = "{" +
            "\\\"type\\\": \\\"MEMBERSHIPS_MS_UPDATE\\\"," +
            "\\\"u\\\": 1," +
            "\\\"c\\\": 1," +
            "\\\"d\\\": \\\"H4sIAAAAAAAAg5EYy8MEMA20A+//5L7VQwAEAAA\\\"" +
            "}";

    /**
     * Keylist payload Gzip
     * {"a":[1573573083296714675,8482869187405483569],"r":[8031872927333060586,6829471020522910836]}
     * = a: [key1, key2] , r: [key3, key4]
     */
    public final static String KEY_LIST_NOTIFICATION_GZIP = "{" +
            "\"type\": \"MEMBERSHIPS_MS_UPDATE\"," +
            "\"u\": 2," +
            "\"c\": 1," +
            "\"d\": \"H4sIAAAAAAAA/wTAsRHDUAgD0F2ofwEIkPAqPhdZIW0uu/v97GPXHU004ULuMGrYR6XUbIjlXULPPse+dt1yhJibBODjrTmj3GJ4emduuDDP/w0AAP//18WLsl0AAAA=\"" +
            "}";

    public final static String ESCAPED_KEY_LIST_NOTIFICATION_GZIP = "{" +
            "\\\"type\\\": \\\"MEMBERSHIPS_MS_UPDATE\\\"," +
            "\\\"n\\\": [\\\"new_segment_added\\\"]," +
            "\\\"u\\\": 2," +
            "\\\"c\\\": 1," +
            "\\\"d\\\": \\\"H4sIAAAAAAAA/wTAsRHDUAgD0F2ofwEIkPAqPhdZIW0uu/v97GPXHU004ULuMGrYR6XUbIjlXULPPse+dt1yhJibBODjrTmj3GJ4emduuDDP/w0AAP//18WLsl0AAAA=\\\"" +
            "}";

public final static String BOUNDED_NOTIFICATION_ZLIB_2 = "{" +
            "\"cn\": 1629754722111, " +
            "\"type\": \"MEMBERSHIPS_MS_UPDATE\"," +
            "\"u\": 1," +
            "\"c\": 2," +
            "\"d\": \"eJzMVk3OhDAIVdNFl9/22zVzEo8yR5mjT6LGsRTKg2LiW8yPUnjQB+2kIwM2ThTIKtVU1oknFcRzufz+YGYM/phnHW8sdPvs9EzXW2I+HFzhNyTNgCD/PpW9xpGiHD0Bw1U5HLSS644FbGZgoPovmjpmX5wAzhIxJyN7IAnFQWX1htj+LUl6ZQRV3umMqYG1LCrOJGLPV8+IidBQZFt6sOUA6CqsX5iEFY2gqufs2mfqRtsVWytRnO+iYMN7xIBqJhDqAydV+HidkGOGEJYvk4fhe/8iIukphG/XfFcfVxnMVcALCOF77qL/EU7ODepxlLST6qxFLYRdOyW8EBY4BqVjObnm3V5ZMkZIKf++8+hM7zM1Kd3aFqVZeSHzDQAA//+QUQ3a\"" +
            "}";
//    c: 2
//    cn: 1629754722111
//    d: "eJzMVk3OhDAIVdNFl9/22zVzEo8yR5mjT6LGsRTKg2LiW8yPUnjQB+2kIwM2ThTIKtVU1oknFcRzufz+YGYM/phnHW8sdPvs9EzXW2I+HFzhNyTNgCD/PpW9xpGiHD0Bw1U5HLSS644FbGZgoPovmjpmX5wAzhIxJyN7IAnFQWX1htj+LUl6ZQRV3umMqYG1LCrOJGLPV8+IidBQZFt6sOUA6CqsX5iEFY2gqufs2mfqRtsVWytRnO+iYMN7xIBqJhDqAydV+HidkGOGEJYvk4fhe/8iIukphG/XfFcfVxnMVcALCOF77qL/EU7ODepxlLST6qxFLYRdOyW8EBY4BqVjObnm3V5ZMkZIKf++8+hM7zM1Kd3aFqVZeSHzDQAA//+QUQ3a"
//    n: ""
//    type: "MEMBERSHIPS_MS_UPDATE"
//    u: 1

    public final static String DECOMPRESSED_KEY_LIST_PAYLOAD_GZIP = "{\"a\":[1573573083296714675,8482869187405483569],\"r\":[8031872927333060586,6829471020522910836]}";

    public static String encodedKeyListPayloadGzip() {
        return (Json.fromJson(KEY_LIST_NOTIFICATION_GZIP, MembershipNotification.class)).getData();
    }

    public static String encodedBoundedPayloadZlib() {
        return (Json.fromJson(BOUNDED_NOTIFICATION_ZLIB, MembershipNotification.class)).getData();
    }

    public static String encodedBoundedPayloadZlib2() {
        return (Json.fromJson(BOUNDED_NOTIFICATION_ZLIB_2, MembershipNotification.class)).getData();
    }

    public static String encodedBoundedPayloadGzip() {
        return (Json.fromJson(BOUNDED_NOTIFICATION_GZIP, MembershipNotification.class)).getData();
    }
}
