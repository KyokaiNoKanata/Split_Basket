package com.example.split_basket;

import android.content.Context;
import android.util.Log;

import com.example.split_basket.data.LogDao;
import com.example.split_basket.data.SplitBasketDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventLogManager {
    // Event types
    public static final String EVENT_TYPE_INVENTORY_ADD = "INVENTORY_ADD";
    public static final String EVENT_TYPE_INVENTORY_REMOVE = "INVENTORY_REMOVE";
    public static final String EVENT_TYPE_INVENTORY_UPDATE = "INVENTORY_UPDATE";
    public static final String EVENT_TYPE_SHOPPING_LIST_ADD = "SHOPPING_LIST_ADD";
    public static final String EVENT_TYPE_SHOPPING_LIST_REMOVE = "SHOPPING_LIST_REMOVE";
    public static final String EVENT_TYPE_SHOPPING_LIST_CHECK = "SHOPPING_LIST_CHECK";
    public static final String EVENT_TYPE_SHOPPING_LIST_PURCHASE = "SHOPPING_LIST_PURCHASE";
    public static final String EVENT_TYPE_SHOPPING_LIST_UPDATE = "SHOPPING_LIST_UPDATE";
    public static final String EVENT_TYPE_BILL_PAY = "BILL_PAY";
    public static final String EVENT_TYPE_BILL_UPDATE = "BILL_UPDATE";
    public static final String EVENT_TYPE_BILL_ADD = "BILL_ADD";
    public static final String EVENT_TYPE_BILL_REMOVE = "BILL_REMOVE";
    private static final String TAG = "EventLogManager";
    private static EventLogManager instance;
    private final LogDao logDao;
    private final List<LogEntry> logsCache;
    private final ExecutorService executorService;

    private EventLogManager(Context context) {
        SplitBasketDatabase db = SplitBasketDatabase.getInstance(context);
        logDao = db.logDao();
        logsCache = new ArrayList<>();
        executorService = Executors.newSingleThreadExecutor();
        // Load logs in background
        loadLogsInBackground();
    }

    public static synchronized EventLogManager getInstance(Context context) {
        if (instance == null) {
            instance = new EventLogManager(context);
        }
        return instance;
    }

    /**
     * Format time as "x minutes/hours ago" format
     */
    public static String formatTimeAgo(long timestamp) {
        long currentTime = System.currentTimeMillis();
        long diff = currentTime - timestamp;

        long minutes = diff / (60 * 1000);
        long hours = diff / (60 * 60 * 1000);
        long days = diff / (24 * 60 * 60 * 1000);

        if (minutes < 60) {
            return minutes + (minutes == 1 ? " minute" : " minutes") + " ago";
        } else if (hours < 24) {
            return hours + (hours == 1 ? " hour" : " hours") + " ago";
        } else {
            return days + (days == 1 ? " day" : " days") + " ago";
        }
    }

    /**
     * Add a log entry
     *
     * @param type     Event type
     * @param content  Event content
     * @param quantity Quantity (optional)
     * @param user     User (optional)
     */
    public void addLog(String type, String content, int quantity, String user) {
        // Wrap into string and call another addLog method
        addLog(type, content + " (" + quantity + ")", user);
    }

    /**
     * Add a log entry
     *
     * @param type    Event type
     * @param content Event content
     * @param user    User (optional)
     */
    public void addLog(String type, String content, String user) {
        long timestamp = System.currentTimeMillis();

        // Create log entry string for formatting
        String userPart = user != null ? " | " + user : "";
        String logEntryString = timestamp + " | " + type + " | " + content + userPart;

        // Format log entry to user-friendly description
        String formattedDescription = formatLogEntry(logEntryString);

        // Create database LogEntry
        com.example.split_basket.data.LogEntry logEntry = new com.example.split_basket.data.LogEntry(timestamp, type,
                formattedDescription, user != null ? user : "");

        // Insert into database in background
        executorService.execute(() -> {
            logDao.insert(logEntry);
            // Update cache
            synchronized (this) {
                logsCache.add(0, new LogEntry(timestamp, type, formattedDescription, user != null ? user : ""));
            }
        });
    }

    /**
     * Load log entries from database in background
     */
    private void loadLogsInBackground() {
        executorService.execute(() -> {
            List<com.example.split_basket.data.LogEntry> dbLogs = logDao.getAllLogs();
            List<LogEntry> loadedLogs = new ArrayList<>();
            for (com.example.split_basket.data.LogEntry dbLog : dbLogs) {
                loadedLogs.add(new LogEntry(dbLog.getTimestamp(), dbLog.getActionType(), dbLog.getDescription(),
                        dbLog.getUser()));
            }
            synchronized (this) {
                logsCache.clear();
                logsCache.addAll(loadedLogs);
            }
        });
    }

    /**
     * Get all log entries (sorted by time descending)
     */
    public synchronized List<LogEntry> getAllLogs() {
        return new ArrayList<>(logsCache);
    }

    /**
     * Get all log entries (sorted by time descending) - for external calls
     */
    public synchronized List<LogEntry> getLogs() {
        return getAllLogs();
    }

    /**
     * Clear all log entries
     */
    public void clearLogs() {
        executorService.execute(() -> {
            logDao.clearAllLogs();
            // Clear cache
            synchronized (this) {
                logsCache.clear();
            }
        });
    }

    /**
     * Parse log entry into a more user-friendly display format
     */
    public String formatLogEntry(String logEntry) {
        try {
            String[] parts = logEntry.split(" \\| ", 3);
            if (parts.length < 3)
                return logEntry;

            long timestamp = Long.parseLong(parts[0]);
            String type = parts[1];
            String content = parts[2];

            // Parse user part
            String user = "";
            if (content.contains(" | ")) {
                int userIndex = content.lastIndexOf(" | ");
                user = content.substring(userIndex + 3);
                content = content.substring(0, userIndex);
            }

            // Format content by type
            String formattedContent = content;
            switch (type) {
                case EVENT_TYPE_INVENTORY_ADD:
                    formattedContent = user + " added inventory: " + content;
                    break;
                case EVENT_TYPE_INVENTORY_REMOVE:
                    formattedContent = user + " removed inventory: " + content;
                    break;
                case EVENT_TYPE_INVENTORY_UPDATE:
                    formattedContent = user + " updated inventory: " + content;
                    break;
                case EVENT_TYPE_SHOPPING_LIST_ADD:
                    formattedContent = user + " added to shopping list: " + content;
                    break;
                case EVENT_TYPE_SHOPPING_LIST_REMOVE:
                    formattedContent = user + " removed from shopping list: " + content;
                    break;
                case EVENT_TYPE_SHOPPING_LIST_CHECK:
                    formattedContent = user + " checked item: " + content;
                    break;
                case EVENT_TYPE_SHOPPING_LIST_PURCHASE:
                    formattedContent = user + " purchased item: " + content;
                    break;
                case EVENT_TYPE_SHOPPING_LIST_UPDATE:
                    formattedContent = user + " updated item: " + content;
                    break;

                case EVENT_TYPE_BILL_PAY:
                    formattedContent = user + " paid bill: " + content;
                    break;
                case EVENT_TYPE_BILL_UPDATE:
                    formattedContent = user + " updated bill: " + content;
                    break;
                case EVENT_TYPE_BILL_ADD:
                    formattedContent = user + " added bill: " + content;
                    break;
                case EVENT_TYPE_BILL_REMOVE:
                    formattedContent = user + " removed bill: " + content;
                    break;
            }

            // Format time
            String timeAgo = EventLogManager.formatTimeAgo(timestamp);

            return formattedContent + " | " + timeAgo;

        } catch (Exception e) {
            Log.e(TAG, "Failed to format log entry", e);
            return logEntry;
        }
    }

    // Log entry class for UI
    public record LogEntry(long timestamp, String actionType, String description, String user) {

        public String getItemName() {
            // Extract item name from description (simple implementation)
            return description;
        }
    }
}