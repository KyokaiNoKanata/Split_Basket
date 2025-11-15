package com.example.split_basket;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EventLogManager {
    // 事件类型
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
    private static final String PREF_NAME = "event_logs";
    private static final String KEY_LOGS = "logs_array";
    private static EventLogManager instance;
    private final SharedPreferences prefs;
    private final List<LogEntry> logsCache;

    private EventLogManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        logsCache = new ArrayList<>(getAllLogsFromStorage());
    }

    public static synchronized EventLogManager getInstance(Context context) {
        if (instance == null) {
            instance = new EventLogManager(context);
        }
        return instance;
    }

    /**
     * 格式化时间为"x minutes/hours ago"格式
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
     * 添加一条日志记录
     *
     * @param type     事件类型
     * @param content  事件内容
     * @param quantity 数量（可选）
     * @param user     用户（可选）
     */
    public synchronized void addLog(String type, String content, int quantity, String user) {
        // 包装成字符串调用另一个addLog方法
        addLog(type, content + " (" + quantity + ")", user);
    }

    /**
     * 添加一条日志记录
     *
     * @param type    事件类型
     * @param content 事件内容
     * @param user    用户（可选）
     */
    public synchronized void addLog(String type, String content, String user) {
        long timestamp = System.currentTimeMillis();

        // 创建日志条目字符串
        String userPart = user != null ? " | " + user : "";
        String logEntryString = timestamp + " | " + type + " | " + content + userPart;

        // 格式化日志条目为用户友好的描述
        String formattedDescription = formatLogEntry(logEntryString);

        // 创建 LogEntry 对象
        LogEntry logEntry = new LogEntry(timestamp, type, formattedDescription, user);

        JSONArray logsArray;
        String rawLogs = prefs.getString(KEY_LOGS, "[]");

        try {
            logsArray = new JSONArray(rawLogs);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse logs", e);
            logsArray = new JSONArray();
        }

        // 将 LogEntry 转换为 JSON 对象并添加到数组
        try {
            JSONObject jsonLog = new JSONObject();
            jsonLog.put("timestamp", logEntry.timestamp());
            jsonLog.put("actionType", logEntry.actionType());
            jsonLog.put("description", logEntry.description());
            jsonLog.put("user", logEntry.user());

            logsArray.put(jsonLog);
            prefs.edit().putString(KEY_LOGS, logsArray.toString()).apply();
            logsCache.add(0, logEntry);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create JSON log entry", e);
        }
    }

    /**
     * 从存储中获取所有日志记录（用于初始化缓存）
     */
    private synchronized List<LogEntry> getAllLogsFromStorage() {
        List<LogEntry> logs = new ArrayList<>();
        String rawLogs = prefs.getString(KEY_LOGS, "[]");

        try {
            JSONArray logsArray = new JSONArray(rawLogs);
            for (int i = 0; i < logsArray.length(); i++) {
                Object logObj = logsArray.get(i);
                if (logObj instanceof String logString) {
                    // 旧格式：字符串形式的日志条目
                    try {
                        String[] parts = logString.split(" \\| ", 3);
                        if (parts.length < 3)
                            continue;

                        long timestamp = Long.parseLong(parts[0]);
                        String type = parts[1];
                        String content = parts[2];

                        // 提取用户
                        String user = "";
                        if (content.contains(" | ")) {
                            int userIndex = content.lastIndexOf(" | ");
                            user = content.substring(userIndex + 3);
                            content = content.substring(0, userIndex);
                        }

                        // 格式化日志
                        String formattedDescription = formatLogEntry(logString);

                        // 创建 LogEntry
                        LogEntry logEntry = new LogEntry(timestamp, type, formattedDescription, user);
                        logs.add(logEntry);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse old format log: " + logString, e);
                    }
                } else if (logObj instanceof JSONObject jsonLog) {
                    // 新格式：JSON 对象形式的日志条目
                    try {
                        long timestamp = jsonLog.getLong("timestamp");
                        String actionType = jsonLog.getString("actionType");
                        String description = jsonLog.getString("description");
                        String user = jsonLog.optString("user", "");

                        LogEntry logEntry = new LogEntry(timestamp, actionType, description, user);
                        logs.add(logEntry);
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to parse JSON log entry", e);
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse logs array", e);
        }

        // 按时间倒序排列
        Collections.reverse(logs);
        return logs;
    }

    /**
     * 获取所有日志记录（按时间倒序）
     */
    public synchronized List<LogEntry> getAllLogs() {
        return new ArrayList<>(logsCache);
    }

    /**
     * 获取所有日志记录（按时间倒序）- 供外部调用
     */
    public synchronized List<LogEntry> getLogs() {
        return getAllLogs();
    }

    /**
     * 清空所有日志
     */
    public synchronized void clearLogs() {
        prefs.edit().putString(KEY_LOGS, "[]").apply();
        logsCache.clear();
    }

    /**
     * 解析日志条目为更友好的显示格式
     */
    public String formatLogEntry(String logEntry) {
        try {
            String[] parts = logEntry.split(" \\| ", 3);
            if (parts.length < 3)
                return logEntry;

            long timestamp = Long.parseLong(parts[0]);
            String type = parts[1];
            String content = parts[2];

            // 解析用户部分
            String user = "";
            if (content.contains(" | ")) {
                int userIndex = content.lastIndexOf(" | ");
                user = content.substring(userIndex + 3);
                content = content.substring(0, userIndex);
            }

            // 根据类型格式化内容
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

            // 格式化时间
            String timeAgo = EventLogManager.formatTimeAgo(timestamp);

            return formattedContent + " | " + timeAgo;

        } catch (Exception e) {
            Log.e(TAG, "Failed to format log entry", e);
            return logEntry;
        }
    }

    // 日志条目类
    public record LogEntry(long timestamp, String actionType, String description, String user) {

        public String getItemName() {
            // 从描述中提取物品名称（简单实现）
            return description;
        }
    }
}