package com.example.split_basket.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.split_basket.BillItem;

import java.util.List;

@Dao
public interface BillDao {
    @Query("SELECT * FROM bills")
    List<BillItem> getAllBills();

    @Query("SELECT * FROM bills")
    LiveData<List<BillItem>> observeBills();

    @Query("SELECT * FROM bills WHERE status = 'Unpaid' OR status = '未支付'")
    List<BillItem> getUnpaidBills();

    @Query("SELECT * FROM bills WHERE status = 'Paid' OR status = '已支付'")
    List<BillItem> getPaidBills();

    @Query("SELECT * FROM bills WHERE id = :billId")
    BillItem getBillById(String billId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BillItem bill);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<BillItem> bills);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void update(BillItem bill);

    @Delete
    void delete(BillItem bill);

    @Query("DELETE FROM bills WHERE id = :billId")
    void deleteById(String billId);

    @Query("DELETE FROM bills")
    void clearAll();

    @Query("SELECT COUNT(*) FROM bills")
    int countBills();
}
