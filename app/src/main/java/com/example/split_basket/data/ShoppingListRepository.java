package com.example.split_basket.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.split_basket.EventLogManager;
import com.example.split_basket.R;
import com.example.split_basket.ShoppingItem;
import com.example.split_basket.callback.OperationCallback;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ShoppingListRepository {

    private static volatile ShoppingListRepository INSTANCE;
    private final ShoppingListDao shoppingListDao;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final Context appContext;
    private final EventLogManager eventLogManager;
    private volatile boolean seeded = false;

    private ShoppingListRepository(@NonNull Context context) {
        appContext = context.getApplicationContext();
        SplitBasketDatabase database = SplitBasketDatabase.getInstance(appContext);
        shoppingListDao = database.shoppingListDao();
        eventLogManager = EventLogManager.getInstance(appContext);
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    public static ShoppingListRepository getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (ShoppingListRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ShoppingListRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    public void ensureSeedData() {
        if (seeded)
            return;
        executorService.execute(() -> {
            if (shoppingListDao.countItems() == 0) {
                shoppingListDao.insert(new ShoppingItem(appContext.getString(R.string.bread), "Alice", 2));
                shoppingListDao.insert(new ShoppingItem(appContext.getString(R.string.tissue), "David", 1));
                shoppingListDao.insert(new ShoppingItem(appContext.getString(R.string.eggs), "Lily", 3));
            }
            seeded = true;
        });
    }

    public LiveData<List<ShoppingItem>> observeItems() {
        return shoppingListDao.observeItems();
    }

    public void addItem(@NonNull ShoppingItem item) {
        addItem(item, null);
    }

    public void addItem(@NonNull ShoppingItem item, @Nullable OperationCallback callback) {
        executorService.execute(() -> {
            String name = item.getName() == null ? "" : item.getName().trim();
            String addedBy = item.getAddedBy() == null ? "" : item.getAddedBy().trim();
            boolean existsSameUser = shoppingListDao.countItemsByNameAndAdder(name, addedBy) > 0;
            if (existsSameUser) {
                notifyCallback(callback, false,
                        appContext.getString(R.string.error_item_duplicate_with_user, name, addedBy));
                return;
            }
            shoppingListDao.insert(item);
            // 添加日志记录
            eventLogManager.addLog(EventLogManager.EVENT_TYPE_SHOPPING_LIST_ADD, item.getName(), item.getQuantity(),
                    "");
            notifyCallback(callback, true,
                    appContext.getString(R.string.item_added_success, name));
        });
    }

    public void updateItem(@NonNull ShoppingItem item) {
        executorService.execute(() -> {
            shoppingListDao.update(item);
            // 添加日志记录
            eventLogManager.addLog(EventLogManager.EVENT_TYPE_SHOPPING_LIST_UPDATE, item.getName(), item.getQuantity(),
                    "");
        });
    }

    public void markItemsPurchasedByIds(@NonNull List<Long> ids) {
        if (ids.isEmpty())
            return;
        executorService.execute(() -> {
            // 获取被标记为购买的商品信息
            List<ShoppingItem> items = shoppingListDao.getItemsByIds(ids);
            shoppingListDao.markPurchasedByIds(ids);
            // 为每个商品添加日志记录
            for (ShoppingItem item : items) {
                eventLogManager.addLog(EventLogManager.EVENT_TYPE_SHOPPING_LIST_PURCHASE, item.getName(),
                        item.getQuantity(), "");
            }
        });
    }

    public void deleteItem(@NonNull ShoppingItem item) {
        executorService.execute(() -> {
            shoppingListDao.delete(item);
            // 添加日志记录
            eventLogManager.addLog(EventLogManager.EVENT_TYPE_SHOPPING_LIST_REMOVE, item.getName(), item.getQuantity(),
                    "");
        });
    }

    public List<ShoppingItem> getPurchasedItems() {
        List<ShoppingItem> result = null;
        try {
            result = executorService.submit(shoppingListDao::getPurchasedItems).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result != null ? result : java.util.Collections.emptyList();
    }

    public void clearAll() {
        executorService.execute(shoppingListDao::clearAll);
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

    private void notifyCallback(@Nullable OperationCallback callback,
                                boolean success,
                                @NonNull String message) {
        if (callback != null) {
            callback.onComplete(success, message);
        }
    }
}
