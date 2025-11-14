package com.example.split_basket;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class BillStorage {
    private static final String PREF_NAME = "bill_storage";
    private static final String KEY_BILLS = "bills_list";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    
    private SharedPreferences preferences;
    private Gson gson;
    
    public BillStorage(Context context) {
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        
        // 首次启动时初始化默认账单数据
        if (preferences.getBoolean(KEY_FIRST_LAUNCH, true)) {
            initializeDefaultBills();
            preferences.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
        } else {
            // 如果不是首次启动，但没有账单数据，也初始化默认账单
            List<BillItem> bills = getAllBills();
            if (bills.isEmpty()) {
                initializeDefaultBills();
            }
        }
    }
    
    // 初始化默认账单数据
    private void initializeDefaultBills() {
        List<BillItem> defaultBills = new ArrayList<>();
        
        // 添加默认的未支付账单
        BillItem unpaidBill = new BillItem("unpaid_bill_1", "Weekend Party", "$ 389.50", "Unpaid", "Equal Split", "2024-01-01");
        unpaidBill.addParticipant("User1");
        unpaidBill.addParticipant("User2");
        unpaidBill.addParticipant("User3");
        unpaidBill.addParticipant("User4");
        defaultBills.add(unpaidBill);
        
        // 添加默认的已支付账单
        BillItem paidBill1 = new BillItem("paid_bill_1", "Daily Shopping", "$ 128.30", "Paid", "By Quantity", "2024-01-02");
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
    
    // 保存账单列表
    public void saveBills(List<BillItem> bills) {
        String json = gson.toJson(bills);
        preferences.edit().putString(KEY_BILLS, json).apply();
    }
    
    // 获取所有账单
    public List<BillItem> getAllBills() {
        String json = preferences.getString(KEY_BILLS, "[]");
        Type type = new TypeToken<List<BillItem>>(){}.getType();
        List<BillItem> bills = gson.fromJson(json, type);
        return bills != null ? bills : new ArrayList<>();
    }
    
    // 添加新账单
    public void addBill(BillItem bill) {
        List<BillItem> bills = getAllBills();
        bills.add(bill);
        saveBills(bills);
    }
    
    // 更新账单
    public void updateBill(BillItem updatedBill) {
        List<BillItem> bills = getAllBills();
        for (int i = 0; i < bills.size(); i++) {
            if (bills.get(i).getId().equals(updatedBill.getId())) {
                bills.set(i, updatedBill);
                saveBills(bills);
                return;
            }
        }
    }
    
    // 根据ID查找账单
    public BillItem getBillById(String billId) {
        List<BillItem> bills = getAllBills();
        for (BillItem bill : bills) {
            if (bill.getId().equals(billId)) {
                return bill;
            }
        }
        return null;
    }
    
    // 获取未支付的账单
    public List<BillItem> getUnpaidBills() {
        List<BillItem> result = new ArrayList<>();
        List<BillItem> bills = getAllBills();
        for (BillItem bill : bills) {
            // 兼容中英文两种状态值
            if ("Unpaid".equals(bill.getStatus()) || "未支付".equals(bill.getStatus())) {
                result.add(bill);
            }
        }
        return result;
    }
    
    // 获取已支付的账单
    public List<BillItem> getPaidBills() {
        List<BillItem> result = new ArrayList<>();
        List<BillItem> bills = getAllBills();
        for (BillItem bill : bills) {
            // 兼容中英文两种状态值
            if ("Paid".equals(bill.getStatus()) || "已支付".equals(bill.getStatus())) {
                result.add(bill);
            }
        }
        return result;
    }
    
    // 清除所有账单
    public void clearAllBills() {
        preferences.edit().remove(KEY_BILLS).apply();
    }
    
    // 根据ID删除账单
    public void deleteBill(String billId) {
        List<BillItem> bills = getAllBills();
        for (int i = 0; i < bills.size(); i++) {
            if (bills.get(i).getId().equals(billId)) {
                bills.remove(i);
                saveBills(bills);
                return;
            }
        }
    }
}