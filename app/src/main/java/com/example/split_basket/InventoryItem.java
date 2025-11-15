package com.example.split_basket;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "inventory_items")
public class InventoryItem {
    @PrimaryKey
    @NonNull
    public String id;
    public String name;
    public int quantity;
    public String category; // Vegetable/Meat/Fruit/Other
    public Long expireDateMillis; // 可空
    public long createdAtMillis;
    public String photoUri; // 新增：可空的照片地址

    public InventoryItem(String id, String name, int quantity, String category, Long expireDateMillis,
                         long createdAtMillis) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.category = category;
        this.expireDateMillis = expireDateMillis;
        this.createdAtMillis = createdAtMillis;
    }

}