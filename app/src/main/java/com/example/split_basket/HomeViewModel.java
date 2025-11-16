package com.example.split_basket;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.split_basket.data.BillRepository;
import com.example.split_basket.data.InventoryRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Future;

public class HomeViewModel extends AndroidViewModel {
    private static final String TAG = "HomeViewModel";
    public final LiveData<Integer> totalItems;
    private final InventoryRepository inventoryRepository;
    private final BillRepository billRepository;
    private final EventLogManager eventLogManager;
    // LiveData for reminders
    private final MutableLiveData<List<String>> _reminders = new MutableLiveData<>(new ArrayList<>());
    // LiveData for logs
    private final MutableLiveData<List<EventLogManager.LogEntry>> _logs = new MutableLiveData<>(new ArrayList<>());
    // LiveData for inventory overview
    private final LiveData<List<InventoryItem>> inventoryItems;
    public LiveData<List<String>> reminders = _reminders;
    public LiveData<List<EventLogManager.LogEntry>> logs = _logs;

    public HomeViewModel(Application application) {
        super(application);
        inventoryRepository = InventoryRepository.getInstance(application);
        billRepository = BillRepository.getInstance(application);
        eventLogManager = EventLogManager.getInstance(application);

        // Ensure seed data for inventory
        inventoryRepository.ensureSeedData();

        inventoryItems = inventoryRepository.observeItems();
        totalItems = Transformations.map(inventoryItems, List::size);
    }

    /**
     * Updates the reminders for expiring inventory items and unpaid bills
     */
    public void updateReminders() {
        // Get inventory items expiring soon (within 7 days)
        List<InventoryItem> items = inventoryRepository.getItems();
        List<BillItem> unpaidBills = billRepository.getUnpaidBills();

        long currentTime = System.currentTimeMillis();
        long sevenDays = 7 * 24 * 60 * 60 * 1000;

        List<String> allReminders = new ArrayList<>();

        // Check for expiring items
        for (InventoryItem item : items) {
            if (item.expireDateMillis != null && item.expireDateMillis <= currentTime + sevenDays
                    && item.expireDateMillis > currentTime) {
                String dateStr = formatDate(item.expireDateMillis);
                allReminders.add(item.name + " will expire on " + dateStr + ".");
            }
        }

        // Check for unpaid bills
        for (BillItem bill : unpaidBills) {
            allReminders.add("Unpaid bill: " + bill.getName() + " (" + bill.getAmount() + ")");
        }

        _reminders.setValue(allReminders);
    }

    /**
     * Loads the latest status logs
     */
    public void loadLogs() {
        List<EventLogManager.LogEntry> logs = eventLogManager.getLogs();
        _logs.setValue(logs);
    }

    /**
     * Adds a new inventory item to the repository
     *
     * @param item The inventory item to add
     */
    public Future<Void> addInventoryItem(InventoryItem item) {
        return inventoryRepository.addItem(item);
    }

    /**
     * Date formatting utility method
     *
     * @param millis The timestamp in milliseconds
     * @return Formatted date string
     */
    private String formatDate(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(millis));
    }

    /**
     * Calculates expiring items count (within 7 days)
     *
     * @return Count of expiring items
     */
    public LiveData<Integer> getExpiringItemsCount() {
        return Transformations.map(inventoryItems, items -> {
            long currentTime = System.currentTimeMillis();
            long sevenDays = 7 * 24 * 60 * 60 * 1000;
            int count = 0;
            for (InventoryItem item : items) {
                if (item.expireDateMillis != null && item.expireDateMillis <= currentTime + sevenDays
                        && item.expireDateMillis > currentTime) {
                    count++;
                }
            }
            return count;
        });
    }

    /**
     * Factory for creating HomeViewModel
     */
    public static class Factory implements ViewModelProvider.Factory {
        private final Application application;

        public Factory(Application application) {
            this.application = application;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(HomeViewModel.class)) {
                return (T) new HomeViewModel(application);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}