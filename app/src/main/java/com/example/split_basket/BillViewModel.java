package com.example.split_basket;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.split_basket.data.BillRepository;

import java.util.List;

public class BillViewModel extends AndroidViewModel {
    private final BillRepository repository;
    private final LiveData<List<BillItem>> allBills;

    public BillViewModel(Application application) {
        super(application);
        repository = BillRepository.getInstance(application);
        repository.ensureSeedData(); // Initialize default bill data if needed
        allBills = repository.observeBills();
    }

    /**
     * Returns LiveData of all bills
     */
    public LiveData<List<BillItem>> getAllBills() {
        return allBills;
    }

    /**
     * Returns list of unpaid bills
     */
    public List<BillItem> getUnpaidBills() {
        return repository.getUnpaidBills();
    }

    /**
     * Returns list of paid bills
     */
    public List<BillItem> getPaidBills() {
        return repository.getPaidBills();
    }

    /**
     * Returns a bill by its ID
     */
    public BillItem getBillById(String billId) {
        return repository.getBillById(billId);
    }

    /**
     * Adds a new bill
     */
    public void addBill(BillItem bill) {
        repository.addBill(bill);
    }

    /**
     * Updates an existing bill
     */
    public void updateBill(BillItem bill) {
        repository.updateBill(bill);
    }

    /**
     * Deletes a bill by its ID
     */
    public void deleteBill(String billId) {
        repository.deleteBill(billId);
    }

    /**
     * Clears all bills
     */
    public void clearAllBills() {
        repository.clearAllBills();
    }

    /**
     * Factory for creating BillViewModel
     */
    public static class Factory implements ViewModelProvider.Factory {
        private final Application application;

        public Factory(Application application) {
            this.application = application;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(BillViewModel.class)) {
                return (T) new BillViewModel(application);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}