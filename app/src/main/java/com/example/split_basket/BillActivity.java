package com.example.split_basket;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class BillActivity extends AppCompatActivity {

    private MaterialButton btnHome, btnInventory, btnList, btnBill;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill);

        View scroll = findViewById(R.id.scrollContent);
        scroll.setAlpha(0f);
        scroll.animate().alpha(1f).setDuration(300).start();

        // Bottom navigation
        btnHome = findViewById(R.id.btnHome);
        btnInventory = findViewById(R.id.btnInventory);
        btnList = findViewById(R.id.btnList);
        btnBill = findViewById(R.id.btnBill);

        updateButtonStates(btnBill);

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
            navigateTo(ListActivity.class);
        });
        btnBill.setOnClickListener(v -> {
            updateButtonStates(btnBill);
            Toast.makeText(this, "Already on Bill", Toast.LENGTH_SHORT).show();
        });

        // Recent bills: unpaid clickable, paid disabled in layout
        View unpaid = findViewById(R.id.cardUnpaidBill);
        unpaid.setOnClickListener(v -> Toast.makeText(this, "Open unpaid bill", Toast.LENGTH_SHORT).show());

        // Create new bill area
        EditText billName = findViewById(R.id.inputBillName);
        EditText totalSpent = findViewById(R.id.inputTotalSpent);
        RadioGroup splitMethod = findViewById(R.id.radioSplitMethod);

        // Fix split method logic: reflect selection changes clearly
        splitMethod.setOnCheckedChangeListener((group, checkedId) -> {
            String mode;
            if (checkedId == R.id.radioEqual) mode = "Equal";
            else if (checkedId == R.id.radioByQuantity) mode = "By quantity";
            else if (checkedId == R.id.radioByItem) mode = "By item";
            else if (checkedId == R.id.radioCustom) mode = "Custom";
            else mode = "Unknown";
            group.setTag(mode); // store current mode for later use
            Toast.makeText(this, "Split method: " + mode, Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnCreateBill).setOnClickListener(v -> {
            String name = billName.getText().toString();
            String total = totalSpent.getText().toString();
            Object modeObj = splitMethod.getTag();
            String mode = modeObj instanceof String ? (String) modeObj : "Equal"; // default to Equal
            Toast.makeText(this, "Create bill: " + name + ", total " + total + ", method " + mode, Toast.LENGTH_SHORT).show();
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
            Intent intent = new Intent(BillActivity.this, targetActivity);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish(); // 结束当前Activity，避免后台栈堆积
        }).start();
    }
}
