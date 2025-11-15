package com.example.split_basket;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class ListActivity extends AppCompatActivity {

    private MaterialButton btnHome, btnInventory, btnList, btnBill;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        View scroll = findViewById(R.id.scrollContent);
        scroll.setAlpha(0f);
        scroll.animate().alpha(1f).setDuration(300).start();

        // Bottom navigation
        btnHome = findViewById(R.id.btnHome);
        btnInventory = findViewById(R.id.btnInventory);
        btnList = findViewById(R.id.btnList);
        btnBill = findViewById(R.id.btnBill);

        updateButtonStates(btnList);

        btnHome.setOnClickListener(v -> {
            updateButtonStates(btnHome);
            navigateTo(HomeActivity.class);
        });
        btnInventory.setOnClickListener(v -> {
            updateButtonStates(btnInventory);
            navigateTo(InventoryActivity.class);
        });
        btnList.setOnClickListener(v -> {
            updateButtonStates(btnList);
            Toast.makeText(this, "Already on List", Toast.LENGTH_SHORT).show();
        });
        btnBill.setOnClickListener(v -> {
            updateButtonStates(btnBill);
            navigateTo(BillActivity.class);
        });

        // Action buttons
        findViewById(R.id.btnAddItems).setOnClickListener(v -> Toast.makeText(this, "Add Items", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnSettlement).setOnClickListener(v -> Toast.makeText(this, "Settlement", Toast.LENGTH_SHORT).show());
    }

    private void updateButtonStates(MaterialButton selectedButton) {
        btnHome.setSelected(false);
        btnInventory.setSelected(false);
        btnList.setSelected(false);
        btnBill.setSelected(false);
        selectedButton.setSelected(true);
    }

    private void navigateTo(Class<?> targetActivity) {
        View scroll = findViewById(R.id.scrollContent);
        scroll.animate().alpha(0f).setDuration(150).withEndAction(() -> {
            Intent intent = new Intent(ListActivity.this, targetActivity);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish(); // 结束当前Activity，避免后台栈堆积
        }).start();
    }
}
