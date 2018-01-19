package io.split.android.client.storage;

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
     */
    void write(String elementId, String content) throws IOException;

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
     * Changes the Id for an element in the storage
     * @param currentId Current Id
     * @param newId New Id for the element
     * @return Whether the rename was successful or not
     */
    boolean rename(String currentId, String newId);
}
