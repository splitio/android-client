package io.split.android.client.track;

import com.google.common.base.Strings;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

import io.split.android.client.dtos.ChunkHeader;
import io.split.android.client.dtos.Event;
import io.split.android.client.storage.FileStorage;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

public class TracksFileStorage extends FileStorage implements ITracksStorage {

    private static final String FILE_NAME_PREFIX = "SPLITIO.events_chunk_id_";
    private static final String FILE_NAME_TEMPLATE = FILE_NAME_PREFIX + "%s.jsonl";
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private final static Type EVENT_CHUNK_TYPE = new TypeToken<ChunkHeader>() {
    }.getType();
    private final static Type EVENT_ROW_TYPE = new TypeToken<Event>() {
    }.getType();

    public TracksFileStorage(@NotNull File rootFolder, @NotNull String folderName) {
        super(rootFolder, folderName);
    }

    public Map<String, EventsChunk> read() throws IOException {

        Map<String, EventsChunk> tracks = new HashMap<>();
        List<String> tracksFiles = getAllIds(FILE_NAME_PREFIX);

        for (String fileName : tracksFiles) {
            FileInputStream inputStream = null;
            Scanner sc = null;
            try {
                File chunkFile = new File(_dataFolder, fileName);
                inputStream = new FileInputStream(chunkFile);
                sc = new Scanner(inputStream, "UTF-8");
                EventsChunk eventsChunk = null;
                if (sc.hasNextLine()) {
                    ChunkHeader chunkHeader = null;
                    String chunkLine = sc.nextLine();
                    if (!Strings.isNullOrEmpty(chunkLine)) {
                        try {
                            chunkHeader = Json.fromJson(chunkLine, EVENT_CHUNK_TYPE);
                        } catch (JsonSyntaxException e) {
                            chunkHeader = new ChunkHeader(UUID.randomUUID().toString(), 1);
                        }
                    } else {
                        continue;
                    }
                    if (chunkHeader != null) {
                        eventsChunk = new EventsChunk(chunkHeader.getId(), chunkHeader.getAttempt());
                        while (sc.hasNextLine()) {
                            String jsonEvent = null;
                            try {
                                jsonEvent = sc.nextLine();
                                eventsChunk.addEvent(Json.fromJson(jsonEvent, EVENT_ROW_TYPE));
                            } catch (JsonSyntaxException e){
                                Logger.e("Could not parse event: " + jsonEvent + " from file: " + fileName);
                            }
                        }

                    }
                }
                if(eventsChunk.getEvents().size() > 0) {
                    tracks.put(eventsChunk.getId(), eventsChunk);
                }

                if (sc.ioException() != null) {
                    Logger.e("An error occurs parsing track events from JsonL files: " + sc.ioException().getLocalizedMessage());
                }

            } catch (FileNotFoundException e) {
                Logger.w("No cached track files found");
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (sc != null) {
                    sc.close();
                }
            }
        }
        return tracks;
    }

    public void write(Map<String, EventsChunk> tracks) throws IOException {
        Set<String> filesToRemove = new HashSet(getAllIds(FILE_NAME_PREFIX));
        for (EventsChunk chunk : tracks.values()) {
            FileWriter fileWriter = null;
            List<Event> events = chunk.getEvents();
            if (events != null && events.size() > 0) {
                try {
                    String fileName = String.format(FILE_NAME_TEMPLATE, chunk.getId());
                    filesToRemove.remove(fileName);
                    File file = new File(_dataFolder, fileName);
                    fileWriter = new FileWriter(file);
                    ChunkHeader chunkHeader = new ChunkHeader(chunk.getId(), chunk.getAttempt());
                    String jsonChunkHeader = Json.toJson(chunkHeader);
                    fileWriter.write(String.format(jsonChunkHeader));
                    fileWriter.write(LINE_SEPARATOR);
                    for(Event event : events) {
                        String jsonEvent = Json.toJson(event);
                        fileWriter.write(jsonEvent);
                        fileWriter.write(LINE_SEPARATOR);
                    }

                } catch (IOException ex) {
                    throw new IOException("Error writing track events chunk: " + FILE_NAME_TEMPLATE);
                } finally {
                    if(fileWriter != null) {
                        fileWriter.close();
                    }
                }
            }
        }
        for(String fileName : filesToRemove) {
            delete(fileName);
        }
    }
}
