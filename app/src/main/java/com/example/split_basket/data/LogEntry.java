package com.example.split_basket.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Database entity for event logs
 */
@Entity(tableName = "log_entries")
public class LogEntry {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long timestamp;
    private String actionType;
    private String description;
    private String user;

    public LogEntry(long timestamp, String actionType, String description, String user) {
        this.timestamp = timestamp;
        this.actionType = actionType;
        this.description = description;
        this.user = user;
    }

    // Getters and setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
