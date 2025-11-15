package com.example.split_basket;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class BillStorage {
    private static final String PREF_NAME = "bill_storage";
    private static final String KEY_BILLS = "bills_list";
    private static final String KEY_FIRST_LAUNCH = "first_launch";

    private final SharedPreferences preferences;
    private final Gson gson;
    private final EventLogManager eventLogManager;

    public BillStorage(Context context) {
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
        eventLogManager = EventLogManager.getInstance(context);

        // Initialize default bill data on first launch
        if (preferences.getBoolean(KEY_FIRST_LAUNCH, true)) {
            initializeDefaultBills();
            preferences.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
        } else {
            // If not first launch but no bill data, also initialize default bill
            List<BillItem> bills = getAllBills();
            if (bills.isEmpty()) {
                initializeDefaultBills();
            }
        }
    }

    // Initialize default bill data
    private void initializeDefaultBills() {
        List<BillItem> defaultBills = new ArrayList<>();

        // Add default unpaid bill
        BillItem unpaidBill = new BillItem("unpaid_bill_1", "Weekend Party", "$ 389.50", "Unpaid", "Equal Split",
                "2024-01-01");
        unpaidBill.addParticipant("User1");
        unpaidBill.addParticipant("User2");
        unpaidBill.addParticipant("User3");
        unpaidBill.addParticipant("User4");
        defaultBills.add(unpaidBill);

        // Add default paid bill
        BillItem paidBill1 = new BillItem("paid_bill_1", "Daily Shopping", "$ 128.30", "Paid", "By Quantity",
                "2024-01-02");
        paidBill1.addParticipant("User1");
        paidBill1.addParticipant("User2");
        paidBill1.addParticipant("User3");
        paidBill1.addParticipant("User4");
        defaultBills.add(paidBill1);

        BillItem paidBill2 = new BillItem("paid_bill_2", "Dinner", "$ 456.80", "Paid", "Custom", "2024-01-03");
        paidBill2.addParticipant("User1");
        paidBill2.addParticipant("User2");
        paidBill2.addParticipant("User3");
        defaultBills.add(paidBill2);

        saveBills(defaultBills);
    }

    // Save bill list
    public void saveBills(List<BillItem> bills) {
        String json = gson.toJson(bills);
        preferences.edit().putString(KEY_BILLS, json).apply();
    }

    // Get all bills
    public List<BillItem> getAllBills() {
        String json = preferences.getString(KEY_BILLS, "[]");
        Type type = new TypeToken<List<BillItem>>() {
        }.getType();
        List<BillItem> bills = gson.fromJson(json, type);
        return bills != null ? bills : new ArrayList<>();
    }

    // Add a new bill
    public synchronized void addBill(BillItem bill) {
        List<BillItem> bills = getAllBills();
        bills.add(0, bill);
        saveBills(bills);
        // Add log record
        eventLogManager.addLog(EventLogManager.EVENT_TYPE_BILL_ADD, bill.getName() + " - " + bill.getAmount(), "");
    }

    // Update bill
    public synchronized void updateBill(BillItem updatedBill) {
        List<BillItem> bills = getAllBills();
        for (int i = 0; i < bills.size(); i++) {
            if (bills.get(i).getId().equals(updatedBill.getId())) {
                bills.set(i, updatedBill);
                saveBills(bills);
                // Add log record
                eventLogManager.addLog(EventLogManager.EVENT_TYPE_BILL_UPDATE,
                        updatedBill.getName() + " - " + updatedBill.getAmount(), "");
                return;
            }
        }
    }

    // Find bill by ID
    public BillItem getBillById(String billId) {
        List<BillItem> bills = getAllBills();
        for (BillItem bill : bills) {
            if (bill.getId().equals(billId)) {
                return bill;
            }
        }
        return null;
    }

    // Get unpaid bills
    public List<BillItem> getUnpaidBills() {
        List<BillItem> result = new ArrayList<>();
        List<BillItem> bills = getAllBills();
        for (BillItem bill : bills) {
            if ("Unpaid".equals(bill.getStatus())) {
                result.add(bill);
            }
        }
        return result;
    }

    // Get paid bills
    public List<BillItem> getPaidBills() {
        List<BillItem> result = new ArrayList<>();
        List<BillItem> bills = getAllBills();
        for (BillItem bill : bills) {
            if ("Paid".equals(bill.getStatus())) {
                result.add(bill);
            }
        }
        return result;
    }

    // Clear all bills
    public void clearAllBills() {
        preferences.edit().remove(KEY_BILLS).apply();
    }

    // Delete bill by ID
    public synchronized void deleteBill(String billId) {
        List<BillItem> bills = getAllBills();
        // First record the information of the bill to be deleted
        BillItem deletedBill = null;
        for (BillItem bill : bills) {
            if (bill.getId().equals(billId)) {
                deletedBill = bill;
                break;
            }
        }
        // Perform deletion
        for (int i = 0; i < bills.size(); i++) {
            if (bills.get(i).getId().equals(billId)) {
                bills.remove(i);
                saveBills(bills);
                // Record deletion log
                if (deletedBill != null) {
                    eventLogManager.addLog(EventLogManager.EVENT_TYPE_BILL_REMOVE,
                            deletedBill.getName() + " - " + deletedBill.getAmount(), "");
                }
                return;
            }
        }
    }
}