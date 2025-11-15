package com.example.split_basket.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * DAO for log entries
 */
@Dao
public interface LogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(LogEntry logEntry);

    @Query("SELECT * FROM log_entries ORDER BY timestamp DESC")
    List<LogEntry> getAllLogs();

    @Query("DELETE FROM log_entries")
    void clearAllLogs();

    @Query("SELECT COUNT(*) FROM log_entries")
    int getLogCount();
}
