package com.example.split_basket;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.split_basket.data.InventoryRepository;

import java.util.List;
import java.util.concurrent.Future;

public class InventoryViewModel extends AndroidViewModel {
    private final InventoryRepository repository;
    private final LiveData<List<InventoryItem>> inventoryItems;

    public InventoryViewModel(Application application) {
        super(application);
        repository = InventoryRepository.getInstance(application);
        inventoryItems = repository.observeItems();
    }

    /**
     * Returns LiveData of all inventory items
     */
    public LiveData<List<InventoryItem>> getInventoryItems() {
        return inventoryItems;
    }

    /**
     * Adds a new inventory item
     */
    public Future<Void> addItem(InventoryItem item) {
        return repository.addItem(item);
    }

    /**
     * Updates an existing inventory item
     */
    public void updateItem(InventoryItem item) {
        repository.updateItem(item);
    }

    /**
     * Removes an inventory item by ID
     */
    public void removeItem(String id) {
        repository.removeItem(id);
    }

    /**
     * Clears all inventory items
     */
    public void clearAll() {
        repository.clearAll();
    }

    /**
     * Returns all inventory logs
     */
    public List<String> getLogs() {
        return repository.getLogs();
    }

    /**
     * Factory for creating InventoryViewModel
     */
    public static class Factory implements ViewModelProvider.Factory {
        private Application application;

        public Factory(Application application) {
            this.application = application;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(InventoryViewModel.class)) {
                return (T) new InventoryViewModel(application);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}