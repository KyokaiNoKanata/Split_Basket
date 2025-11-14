package com.example.split_basket;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.split_basket.callback.OperationCallback;
import com.example.split_basket.data.ShoppingListRepository;

import java.util.List;

public class ShoppingListViewModel extends AndroidViewModel {

    private final ShoppingListRepository repository;
    private final LiveData<List<ShoppingItem>> items;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ShoppingListViewModel(@NonNull Application application) {
        super(application);
        repository = ShoppingListRepository.getInstance(application);
        repository.ensureSeedData();
        items = repository.observeItems();
    }

    public LiveData<List<ShoppingItem>> getItems() {
        return items;
    }

    public void addItem(@NonNull String name, int quantity, @NonNull String addedBy) {
        addItem(name, quantity, addedBy, null);
    }

    public void addItem(@NonNull String name, int quantity, @NonNull String addedBy,
                        @Nullable OperationCallback callback) {
        ShoppingItem item = new ShoppingItem(name, addedBy, quantity);
        repository.addItem(item, wrapCallback(callback));
    }

    public void setPurchased(@NonNull ShoppingItem item, boolean purchased) {
        item.setPurchased(purchased);
        repository.updateItem(item);
    }

    public void markItemsPurchased(@NonNull List<ShoppingItem> items) {
        for (ShoppingItem item : items) {
            item.setPurchased(true);
            repository.updateItem(item);
        }
    }

    public void markItemsPurchasedByIds(@NonNull List<Long> ids) {
        repository.markItemsPurchasedByIds(ids);
    }

    public void deleteItem(@NonNull ShoppingItem item) {
        repository.deleteItem(item);
    }

    public void updateItem(@NonNull ShoppingItem item) {
        repository.updateItem(item);
    }

    public void updateItemDetails(@NonNull ShoppingItem item, @NonNull String name, int quantity, @NonNull String addedBy) {
        item.setName(name);
        item.setQuantity(quantity);
        item.setAddedBy(addedBy);
        repository.updateItem(item);
    }

    public void restoreItem(@NonNull ShoppingItem template) {
        ShoppingItem copy = new ShoppingItem();
        copy.setId(template.getId());
        copy.setName(template.getName());
        copy.setAddedBy(template.getAddedBy());
        copy.setQuantity(template.getQuantity());
        copy.setPurchased(template.isPurchased());
        copy.setCreatedAt(template.getCreatedAt());
        copy.setInventoryItemId(template.getInventoryItemId());
        repository.addItem(copy);
    }

    @Nullable
    private OperationCallback wrapCallback(@Nullable OperationCallback callback) {
        if (callback == null) return null;
        return (success, message) -> mainHandler.post(() -> callback.onComplete(success, message));
    }
}
