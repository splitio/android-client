package io.split.android.client.dtos;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

// This code will be removed.
// Also, to avoid possible problems with json parsing
@SuppressWarnings("FieldCanBeLocal")
public class ChunkHeader {
    public final static Type CHUNK_HEADER_TYPE = new TypeToken<List<ChunkHeader>>() {
    }.getType();

    private String id;
    private int attempt;
    private long timestamp;

    public ChunkHeader(String id, int attempt, long timestamp) {
        this.id = id;
        this.attempt = attempt;
        this.timestamp = timestamp;
    }

    public ChunkHeader(String id, int attempt) {
        this(id, attempt, 0);
    }

    public String getId() {
        return id;
    }

    public int getAttempt() {
        return attempt;
    }

    public int getTimestamp() {
        return attempt;
    }

}