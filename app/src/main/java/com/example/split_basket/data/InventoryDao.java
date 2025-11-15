package com.example.split_basket.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.split_basket.InventoryItem;

import java.util.List;

@Dao
public interface InventoryDao {
    @Query("SELECT * FROM inventory_items")
    List<InventoryItem> getAllItems();

    @Query("SELECT * FROM inventory_items")
    LiveData<List<InventoryItem>> observeItems();

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    InventoryItem getItemById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(InventoryItem item);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void update(InventoryItem item);

    @Delete
    void delete(InventoryItem item);

    @Query("DELETE FROM inventory_items WHERE id = :itemId")
    void deleteById(String itemId);

    @Query("DELETE FROM inventory_items")
    void clearAll();

    @Query("SELECT COUNT(*) FROM inventory_items")
    int countItems();
}
