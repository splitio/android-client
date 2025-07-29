package io.split.android.client.storage.db;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

public class SplitRoomDatabaseTest {

    @Mock
    private Context mockContext;
    
    @Mock
    private Context mockApplicationContext;
    
    @Mock
    private RoomDatabase.Builder<SplitRoomDatabase> mockBuilder;
    
    @Mock
    private SplitRoomDatabase mockDatabase;
    
    @Mock
    private SupportSQLiteOpenHelper mockOpenHelper;
    
    @Mock
    private SupportSQLiteDatabase mockSqliteDatabase;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        when(mockContext.getApplicationContext()).thenReturn(mockApplicationContext);
        when(mockDatabase.getOpenHelper()).thenReturn(mockOpenHelper);
        when(mockOpenHelper.getWritableDatabase()).thenReturn(mockSqliteDatabase);
    }

    @Test
    public void shouldCallGetWritableDatabaseDuringInitialization() {
        String databaseName = "test_database_unique_1";
        
        try (MockedStatic<Room> mockedRoom = mockStatic(Room.class)) {
            mockRoom(mockedRoom, databaseName);

            SplitRoomDatabase result = SplitRoomDatabase.getDatabase(mockContext, databaseName);
            
            assertNotNull("Database instance should not be null", result);
            // Verify that getWritableDatabase is called twice:
            // 1. Once for pragma setup
            // 2. Once for ensuring Room initialization before preload thread
            verify(mockOpenHelper, times(2)).getWritableDatabase();
        }
    }

    @Test
    public void shouldHandleExceptionDuringRoomInitializationGracefully() {
        String databaseName = "test_database_unique_2";
        
        try (MockedStatic<Room> mockedRoom = mockStatic(Room.class)) {
            mockRoom(mockedRoom, databaseName);

            // Make the second call to getWritableDatabase throw an exception
            when(mockOpenHelper.getWritableDatabase())
                .thenReturn(mockSqliteDatabase)
                .thenThrow(new RuntimeException("Database initialization failed")); // Second call fails
            
            SplitRoomDatabase result = SplitRoomDatabase.getDatabase(mockContext, databaseName);
            
            verify(mockOpenHelper, times(2)).getWritableDatabase();
            // Should still return the database instance despite initialization failure
            assertSame("Should return the same database instance", mockDatabase, result);
        }
    }

    @Test
    public void shouldReturnSameInstanceForSameDatabaseName() {
        String databaseName = "test_database_unique_3";
        
        try (MockedStatic<Room> mockedRoom = mockStatic(Room.class)) {
            mockRoom(mockedRoom, databaseName);

            SplitRoomDatabase instance1 = SplitRoomDatabase.getDatabase(mockContext, databaseName);
            SplitRoomDatabase instance2 = SplitRoomDatabase.getDatabase(mockContext, databaseName);
            
            assertSame("Should return the same instance for same database name", instance1, instance2);
            // getWritableDatabase should only be called during first initialization
            verify(mockOpenHelper, times(2)).getWritableDatabase();
        }
    }

    @Test
    public void shouldCallGetWritableDatabaseBeforeStartingPreloadThread() {
        String databaseName = "test_database_unique_4";
        
        try (MockedStatic<Room> mockedRoom = mockStatic(Room.class)) {
            mockRoom(mockedRoom, databaseName);

            SplitRoomDatabase.getDatabase(mockContext, databaseName);
            
            // Verify that getWritableDatabase is called twice:
            // 1. Once for pragma setup
            // 2. Once for ensuring Room initialization before preload thread
            verify(mockOpenHelper, times(2)).getWritableDatabase();
            
            // Verify the database instance is returned
            verify(mockBuilder).build();
        }
    }

    private void mockRoom(MockedStatic<Room> mockedRoom, String databaseName) {
        mockedRoom.when(() -> Room.databaseBuilder(
            eq(mockApplicationContext),
            eq(SplitRoomDatabase.class),
            eq(databaseName)
        )).thenReturn(mockBuilder);

        mockReturn();
    }

    private void mockReturn() {
        when(mockBuilder.setJournalMode(any())).thenReturn(mockBuilder);
        when(mockBuilder.fallbackToDestructiveMigration()).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockDatabase);
    }
}
