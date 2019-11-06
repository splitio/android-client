package io.split.android.client.storage.legacy;

import java.io.IOException;
import java.util.List;

/**
 * Created by guillermo on 11/23/17.
 */

public interface IStorage {

    /**
     * Reads an element from the storage
     *
     * @param elementId Identifier for the element to be read
     * @return JSON representation of the element
     */
    String read(String elementId) throws IOException;

    /**
     * Writes an element into the storage
     *
     * @param elementId Identifier for the element to be written
     * @param content   JSON representation of the element
     * @return Whether the write was successful or not
     */
    boolean write(String elementId, String content) throws IOException;

    /**
     * Deletes an element based on it's identifier
     *
     * @param elementId Identifier for the element to be deleted
     */
    void delete(String elementId);

    /**
     * Gets all stored element Ids
     * @return Array of element Ids
     */
    String[] getAllIds();

    /**
     * Gets all stored element Ids starting with fileNamePrefix
     *
     * @param fileNamePrefix Prefix to filter returned ids
     * @return Array of element Ids
     */
    List<String> getAllIds(String fileNamePrefix);

    /**
     * Changes the Id for an element in the storage
     * @param currentId Current Id
     * @param newId New Id for the element
     * @return Whether the rename was successful or not
     */
    boolean rename(String currentId, String newId);

    /**
     * Checks existence of an element in the storage
     * @param elementId Element Id
     * @return Whether the file exists or not
     */
    boolean exists(String elementId);

    /**
     * Returns file size
     * @param elementId Element Id
     * @return File size in bytes
     */
    long fileSize(String elementId);

    /**
     * Deletes several files
     *
     * @param files list of file names to be deleted
     */
    void delete(List<String> files);

}
