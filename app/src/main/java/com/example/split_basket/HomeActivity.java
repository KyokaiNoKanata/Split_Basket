package com.example.split_basket;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class HomeActivity extends AppCompatActivity {

    private MaterialButton btnHome, btnInventory, btnList, btnBill;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        View scroll = findViewById(R.id.scrollContent);
        scroll.setAlpha(0f);
        scroll.animate().alpha(1f).setDuration(300).start();

        // Bottom navigation
        btnHome = findViewById(R.id.btnHome);
        btnInventory = findViewById(R.id.btnInventory);
        btnList = findViewById(R.id.btnList);
        btnBill = findViewById(R.id.btnBill);

        updateButtonStates(btnHome);

        btnHome.setOnClickListener(v -> {
            updateButtonStates(btnHome);
            Toast.makeText(this, "Already on Home", Toast.LENGTH_SHORT).show();
        });
        btnInventory.setOnClickListener(v -> {
            updateButtonStates(btnInventory);
            navigateTo(InventoryActivity.class);
        });
        btnList.setOnClickListener(v -> {
            updateButtonStates(btnList);
            navigateTo(ListActivity.class);
        });
        btnBill.setOnClickListener(v -> {
            updateButtonStates(btnBill);
            navigateTo(BillActivity.class);
        });

        // Interactive cards
        View quickAdd = findViewById(R.id.cardQuickAdd);
        View overview = findViewById(R.id.cardOverview);
        View newList = findViewById(R.id.cardNewList);
        View newBill = findViewById(R.id.cardNewBill);

        quickAdd.setOnClickListener(v -> Toast.makeText(this, "Quick Add clicked", Toast.LENGTH_SHORT).show());
        overview.setOnClickListener(v -> Toast.makeText(this, "Overview clicked", Toast.LENGTH_SHORT).show());
        newList.setOnClickListener(v -> Toast.makeText(this, "New List clicked", Toast.LENGTH_SHORT).show());
        newBill.setOnClickListener(v -> Toast.makeText(this, "New Bill clicked", Toast.LENGTH_SHORT).show());
    }

    private void updateButtonStates(MaterialButton selectedButton) {
        btnHome.setSelected(false);
        btnInventory.setSelected(false);
        btnList.setSelected(false);
        btnBill.setSelected(false);
        selectedButton.setSelected(true);
    }

    private void navigateTo(Class<?> target) {
        View scroll = findViewById(R.id.scrollContent);
        scroll.animate().alpha(0f).setDuration(150).withEndAction(() -> {
            Intent intent = new Intent(HomeActivity.this, target);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish(); // 结束当前Activity，避免后台栈堆积
        }).start();
    }
}