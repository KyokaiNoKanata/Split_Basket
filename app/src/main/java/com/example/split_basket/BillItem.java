package com.example.split_basket;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class BillItem implements Parcelable {
    private String id;
    private String name;
    private String amount;
    private String status;
    private String method;
    private List<String> participants;
    private List<Double> customAmounts; // 用于存储自定义金额
    private String creationDate; // 账单创建日期

    public BillItem(String id, String name, String amount, String status, String method, String creationDate) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.status = status;
        this.method = method;
        this.participants = new ArrayList<>();
        this.customAmounts = new ArrayList<>();
        this.creationDate = creationDate;
    }

    protected BillItem(Parcel in) {
        id = in.readString();
        name = in.readString();
        amount = in.readString();
        status = in.readString();
        method = in.readString();
        creationDate = in.readString();
        participants = in.createStringArrayList();
        // 读取自定义金额
        int size = in.readInt();
        customAmounts = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            customAmounts.add(in.readDouble());
        }
    }

    public static final Creator<BillItem> CREATOR = new Creator<BillItem>() {
        @Override
        public BillItem createFromParcel(Parcel in) {
            return new BillItem(in);
        }

        @Override
        public BillItem[] newArray(int size) {
            return new BillItem[size];
        }
    };

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void addParticipant(String participant) {
        this.participants.add(participant);
    }

    public List<Double> getCustomAmounts() {
        return customAmounts;
    }

    public void setCustomAmount(int index, double amount) {
        if (index >= 0 && index < customAmounts.size()) {
            customAmounts.set(index, amount);
        }
    }

    public void addCustomAmount(double amount) {
        this.customAmounts.add(amount);
    }

    // 计算平均金额
    public double getAverageAmount() {
        if (participants.isEmpty()) return 0;
        
        try {
            // 移除货币符号和空格，转换为double
            String cleanAmount = amount.replace("¥", "").replace("$", "").trim();
            double total = Double.parseDouble(cleanAmount);
            return total / participants.size();
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    public String getCreationDate() {
        return creationDate;
    }
    
    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeString(name);
        parcel.writeString(amount);
        parcel.writeString(status);
        parcel.writeString(method);
        parcel.writeString(creationDate);
        parcel.writeStringList(participants);
        // 写入自定义金额
        parcel.writeInt(customAmounts.size());
        for (Double amount : customAmounts) {
            parcel.writeDouble(amount);
        }
    }
}