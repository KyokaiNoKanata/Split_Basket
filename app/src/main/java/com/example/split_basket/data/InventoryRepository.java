package com.example.split_basket.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.split_basket.EventLogManager;
import com.example.split_basket.InventoryItem;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

public class InventoryRepository {

    private static volatile InventoryRepository INSTANCE;
    private final InventoryDao inventoryDao;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final Context appContext;
    private final EventLogManager eventLogManager;
    private volatile boolean seeded = false;

    private InventoryRepository(@NonNull Context context) {
        appContext = context.getApplicationContext();
        SplitBasketDatabase database = SplitBasketDatabase.getInstance(appContext);
        inventoryDao = database.inventoryDao();
        eventLogManager = EventLogManager.getInstance(appContext);
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    public static InventoryRepository getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (InventoryRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new InventoryRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    public void ensureSeedData() {
        if (seeded)
            return;
        executorService.execute(() -> {
            if (inventoryDao.countItems() == 0) {
                // No seed data for inventory
            }
            seeded = true;
        });
    }

    public LiveData<List<InventoryItem>> observeItems() {
        return inventoryDao.observeItems();
    }

    public List<InventoryItem> getItems() {
        List<InventoryItem> result = null;
        try {
            result = executorService.submit(inventoryDao::getAllItems).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result != null ? result : java.util.Collections.emptyList();
    }

    public Future<Void> addItem(@NonNull InventoryItem item) {
        return executorService.submit(() -> {
            inventoryDao.insert(item);
            // 添加日志记录
            eventLogManager.addLog(
                    EventLogManager.EVENT_TYPE_INVENTORY_ADD,
                    item.name + " x" + item.quantity + " | " + item.category,
                    "xxx" // 默认用户
            );
            return null;
        });
    }

    public void updateItem(@NonNull InventoryItem updated) {
        executorService.execute(() -> {
            inventoryDao.update(updated);
            // 添加日志记录
            eventLogManager.addLog(
                    EventLogManager.EVENT_TYPE_INVENTORY_UPDATE,
                    updated.name + " x" + updated.quantity + " | " + updated.category,
                    "xxx" // 默认用户
            );
        });
    }

    public void removeItem(String id) {
        executorService.execute(() -> {
            // Get the item first to log details
            InventoryItem item = inventoryDao.getItemById(id);
            inventoryDao.deleteById(id);

            // 添加日志记录
            if (item != null) {
                eventLogManager.addLog(
                        EventLogManager.EVENT_TYPE_INVENTORY_REMOVE,
                        item.name + " x" + item.quantity + " | " + item.category,
                        "xxx" // 默认用户
                );
            }
        });
    }

    public void clearAll() {
        executorService.execute(() -> {
            inventoryDao.clearAll();
        });
    }

    // Get logs - this method returns all logs for inventory items
    public List<String> getLogs() {
        // Get logs from EventLogManager and convert to the expected format
        return eventLogManager.getAllLogs().stream()
                .filter(logEntry -> logEntry.getActionType().startsWith("INVENTORY"))
                .map(logEntry -> {
                    // Convert the event log to the old format: "time | type | message | category"
                    String description = logEntry.getDescription();
                    String category = "";
                    String formattedMessage = description;

                    // Extract category from message (old format: "name xqty | category")
                    if (description.contains(" | ")) {
                        String[] parts = description.split(" \\| ", 2);
                        formattedMessage = parts[0];
                        category = parts[1];
                    }

                    // Map event type to old format (IN/OUT/UPDATE)
                    String oldType = "";
                    switch (logEntry.getActionType()) {
                        case EventLogManager.EVENT_TYPE_INVENTORY_ADD:
                            oldType = "IN";
                            break;
                        case EventLogManager.EVENT_TYPE_INVENTORY_REMOVE:
                            oldType = "OUT";
                            break;
                        case EventLogManager.EVENT_TYPE_INVENTORY_UPDATE:
                            oldType = "UPDATE";
                            break;
                        default:
                            oldType = logEntry.getActionType();
                    }

                    // Return in the expected format
                    return String.format("%d | %s | %s | %s", logEntry.getTimestamp(), oldType, formattedMessage,
                            category);
                })
                .collect(Collectors.toList());
    }

    private void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(3, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
