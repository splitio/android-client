package helper;

import io.split.android.client.service.sseclient.notifications.MySegmentChangeV2Notification;
import io.split.android.client.utils.Json;

public class TestingData {

    public final static String KEY_LIST_NOTIFICATION = "{" +
            "\"type\": 2," +
            "\"compression\": 2," +
            "\"data\": \"H4sIAAAAAAAA/wTAsRHDUAgD0F2ofwEIkPAqPhdZIW0uu/v97GPXHU004ULuMGrYR6XUbIjlXULPPse+dt1yhJibBODjrTmj3GJ4emduuDDP/w0AAP//18WLsl0AAAA=\"" +
            "}";

    public final static String DECOMPRESSED_KEY_LIST_PAYLOAD = "{\"a\":[1573573083296714675,8482869187405483569],\"r\":[8031872927333060586,6829471020522910836]}";

    public static String encodedKeyListPayload() {
        return (Json.fromJson(KEY_LIST_NOTIFICATION, MySegmentChangeV2Notification.class)).getData();
    }
}
