package com.example.split_basket.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.split_basket.ShoppingItem;

import java.util.List;

@Dao
public interface ShoppingListDao {

    @Query("SELECT * FROM shopping_items ORDER BY purchased ASC, created_at ASC")
    LiveData<List<ShoppingItem>> observeItems();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ShoppingItem item);

    @Update
    void update(ShoppingItem item);

    @Query("UPDATE shopping_items SET purchased = 1 WHERE id IN (:ids)")
    void markPurchasedByIds(List<Long> ids);

    @Query("SELECT * FROM shopping_items WHERE purchased = 1")
    List<ShoppingItem> getPurchasedItems();

    @Delete
    void delete(ShoppingItem item);

    @Query("DELETE FROM shopping_items")
    void clearAll();

    @Query("SELECT COUNT(*) FROM shopping_items")
    int countItems();

    @Query("SELECT COUNT(*) FROM shopping_items WHERE LOWER(name) = LOWER(:name)")
    int countItemsByName(String name);

    @Query("SELECT COUNT(*) FROM shopping_items WHERE LOWER(name) = LOWER(:name) AND LOWER(added_by) = LOWER(:addedBy)")
    int countItemsByNameAndAdder(String name, String addedBy);

    @Query("SELECT * FROM shopping_items WHERE id IN (:ids)")
    List<ShoppingItem> getItemsByIds(List<Long> ids);
}
