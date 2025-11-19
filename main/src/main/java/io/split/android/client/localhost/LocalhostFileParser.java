package io.split.android.client.localhost;
import java.util.Map;
import io.split.android.client.dtos.Split;

public interface LocalhostFileParser {
    Map<String, Split> parse(String content);
}
