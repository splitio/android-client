package io.split.android.client.storage.db;

import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.Query;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.utils.Logger;

public class SplitQueryDaoImpl implements SplitQueryDao {

    private final SplitRoomDatabase mDatabase;

    public SplitQueryDaoImpl(SplitRoomDatabase mDatabase) {
        this.mDatabase = mDatabase;
    }

    public List<SplitEntity> get(long rowIdFrom, int maxRows) {

        String sql =    "SELECT rowid, name, body, updated_at FROM splits WHERE rowId > ? ORDER BY rowId LIMIT ?";
        Object[] arguments = {rowIdFrom, maxRows};
        Cursor cursor = mDatabase.query(sql, arguments);

        try {
            final int rowIdIndex = getColumnIndexOrThrow(cursor, "rowid");
            final int nameIndex = getColumnIndexOrThrow(cursor, "name");
            final int bodyIndex = getColumnIndexOrThrow(cursor, "body");
            final int updatedAtIndex = getColumnIndexOrThrow(cursor, "updated_at");
            final List<SplitEntity> entities = new ArrayList<SplitEntity>(cursor.getCount());
            while (cursor.moveToNext()) {
                final SplitEntity item;
                item = new SplitEntity();
                item.setRowId(cursor.getLong(rowIdIndex));
                item.setName(cursor.getString(nameIndex));
                item.setBody(cursor.getString(bodyIndex));
                item.setUpdatedAt(cursor.getLong(updatedAtIndex));
                entities.add(item);
            }
            return entities;
        } catch (Exception e) {
            Logger.e("Error executing splits query: " + e.getLocalizedMessage());
        } finally {
            cursor.close();
        }
        return new ArrayList<>();
    }

    int getColumnIndexOrThrow(@NonNull Cursor c, @NonNull String name) {
        final int index = c.getColumnIndex(name);
        if (index >= 0) {
            return index;
        }
        return c.getColumnIndexOrThrow("`" + name + "`");
    }
}
