package com.example.split_basket;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

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

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("name", name);
        o.put("quantity", quantity);
        o.put("category", category);
        if (expireDateMillis != null)
            o.put("expireDateMillis", expireDateMillis);
        else
            o.put("expireDateMillis", JSONObject.NULL);
        o.put("createdAtMillis", createdAtMillis);
        // 新增：保存照片地址（可空）
        if (photoUri != null)
            o.put("photoUri", photoUri);
        else
            o.put("photoUri", JSONObject.NULL);
        return o;
    }

    public static InventoryItem fromJson(JSONObject o) throws JSONException {
        String id = o.getString("id");
        String name = o.getString("name");
        int quantity = o.getInt("quantity");
        String category = o.getString("category");
        Long expire = o.isNull("expireDateMillis") ? null : o.getLong("expireDateMillis");
        long created = o.getLong("createdAtMillis");
        InventoryItem item = new InventoryItem(id, name, quantity, category, expire, created);
        // 新增：读取照片地址
        item.photoUri = o.isNull("photoUri") ? null : o.getString("photoUri");
        return item;
    }
}