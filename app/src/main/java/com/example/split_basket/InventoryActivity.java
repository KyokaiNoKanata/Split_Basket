package com.example.split_basket;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.button.MaterialButton;

public class InventoryActivity extends AppCompatActivity {

    private MaterialButton btnHome, btnInventory, btnList, btnBill;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        View scroll = findViewById(R.id.scrollContent);
        scroll.setAlpha(0f);
        scroll.animate().alpha(1f).setDuration(300).start();

        // Bottom navigation
        btnHome = findViewById(R.id.btnHome);
        btnInventory = findViewById(R.id.btnInventory);
        btnList = findViewById(R.id.btnList);
        btnBill = findViewById(R.id.btnBill);

        updateButtonStates(btnInventory);

        btnHome.setOnClickListener(v -> {
            updateButtonStates(btnHome);
            navigateTo(HomeActivity.class);
        });
        btnInventory.setOnClickListener(v -> {
            updateButtonStates(btnInventory);
            Toast.makeText(this, "Already on Inventory", Toast.LENGTH_SHORT).show();
        });
        btnList.setOnClickListener(v -> {
            updateButtonStates(btnList);
            navigateTo(ListActivity.class);
        });
        btnBill.setOnClickListener(v -> {
            updateButtonStates(btnBill);
            navigateTo(BillActivity.class);
        });

        // Top action buttons
        findViewById(R.id.btnManualAdd).setOnClickListener(v -> Toast.makeText(this, "Manually add", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnImportFromList).setOnClickListener(v -> Toast.makeText(this, "Import from Shopping List", Toast.LENGTH_SHORT).show());

        // Category chips single selection
        ChipGroup chipGroup = findViewById(R.id.chipGroupCategories);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            String selected = "All";
            if (!checkedIds.isEmpty()) {
                int id = checkedIds.get(0);
                Chip chip = group.findViewById(id);
                if (chip != null) selected = chip.getText().toString();
            }
            Toast.makeText(this, "Category: " + selected, Toast.LENGTH_SHORT).show();
        });
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
            Intent intent = new Intent(InventoryActivity.this, targetActivity);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish(); // 结束当前Activity，避免后台栈堆积
        }).start();
    }
}