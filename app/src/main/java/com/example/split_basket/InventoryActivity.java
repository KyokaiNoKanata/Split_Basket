package com.example.split_basket;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.split_basket.data.InventoryRepository;
import com.example.split_basket.data.ShoppingListRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;

public class InventoryActivity extends AppCompatActivity {

    private static final int SOON_DAYS = 3;
    private MaterialButton btnHome, btnInventory, btnList, btnBill;
    private android.widget.LinearLayout itemsContainer;
    private String selectedCategory = "All";
    private android.widget.TextView tvRemain, tvSoon, tvConsumed;
    private InventoryRepository inventoryRepository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        // Initialize inventory repository
        inventoryRepository = InventoryRepository.getInstance(this);

        View scroll = findViewById(R.id.scrollContent);
        scroll.setAlpha(0f);
        scroll.animate().alpha(1f).setDuration(300).start();

        itemsContainer = findViewById(R.id.itemsContainer);
        tvRemain = findViewById(R.id.tvRemain);
        tvSoon = findViewById(R.id.tvSoon);
        tvConsumed = findViewById(R.id.tvConsumed);

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
        findViewById(R.id.btnManualAdd).setOnClickListener(v -> {
            Intent intent = new Intent(InventoryActivity.this, InventoryAddActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnImportFromList).setOnClickListener(v -> importPurchasedItems());

        // Category chips single selection
        ChipGroup chipGroup = findViewById(R.id.chipGroupCategories);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            String selected = "All";
            if (!checkedIds.isEmpty()) {
                int id = checkedIds.get(0);
                Chip chip = group.findViewById(id);
                if (chip != null) {
                    selected = chip.getText().toString();
                }
            }
            selectedCategory = selected;
            renderItems(selectedCategory);
            updateOverview(selectedCategory);
            Toast.makeText(this, "Category: " + selected, Toast.LENGTH_SHORT).show();
        });

        // 首次进入页面渲染
        renderItems(selectedCategory);
        updateOverview(selectedCategory);
    }

    private void importPurchasedItems() {
        // 初始化 ShoppingListRepository
        ShoppingListRepository shoppingListRepository = ShoppingListRepository.getInstance(getApplication());

        // 获取所有已支付的项目
        List<ShoppingItem> purchasedItems = shoppingListRepository.getPurchasedItems();

        if (purchasedItems.isEmpty()) {
            Toast.makeText(InventoryActivity.this, "没有已支付的项目可导入", Toast.LENGTH_SHORT).show();
            return;
        }

        // 将已支付的项目转换为 InventoryItem 并添加到库存
        List<Future<Void>> futures = new ArrayList<>();
        for (ShoppingItem item : purchasedItems) {
            // 生成唯一 ID
            String id = UUID.randomUUID().toString();

            // 创建 InventoryItem 对象（默认分类为 "Other"）
            InventoryItem inventoryItem = new InventoryItem(
                    id,
                    item.getName(),
                    item.getQuantity(),
                    "Other", // 默认分类
                    null, // 没有过期日期
                    item.getCreatedAt() // 使用购物项的创建时间
            );

            // 添加到库存（异步操作，保存 Future）
            futures.add(inventoryRepository.addItem(inventoryItem));
            // 从购物清单中删除已导入的项目
            shoppingListRepository.deleteItem(item);
        }

        // 等待所有异步操作完成
        for (Future<Void> future : futures) {
            try {
                future.get(); // 阻塞直到操作完成
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 刷新界面以显示新导入的项目
        renderItems(selectedCategory);
        updateOverview(selectedCategory);

        // 显示导入成功消息
        Toast.makeText(InventoryActivity.this, "已导入 " + purchasedItems.size() + " 个项目到库存", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从新增页返回时刷新
        renderItems(selectedCategory);
        updateOverview(selectedCategory);
    }

    private void updateButtonStates(MaterialButton selectedButton) {
        btnHome.setChecked(false);
        btnInventory.setChecked(false);
        btnList.setChecked(false);
        btnBill.setChecked(false);
        selectedButton.setChecked(true);
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

    private void renderItems(String category) {
        InventoryRepository repo = InventoryRepository.getInstance(this);
        java.util.List<InventoryItem> items = repo.getItems();

        itemsContainer.removeAllViews();

        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("MM-dd-yyyy", java.util.Locale.getDefault());

        for (InventoryItem it : items) {
            if (!"All".equalsIgnoreCase(category) && !it.category.equalsIgnoreCase(category)) {
                continue;
            }
            android.widget.LinearLayout row = new android.widget.LinearLayout(this);
            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            row.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
            row.setWeightSum(3f);
            row.setPadding(0, dp(4), 0, dp(4));

            android.widget.TextView name = new android.widget.TextView(this);
            android.widget.TextView date = new android.widget.TextView(this);
            android.widget.TextView qty = new android.widget.TextView(this);

            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(0,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            name.setLayoutParams(lp);
            date.setLayoutParams(lp);
            qty.setLayoutParams(lp);

            name.setText(it.name);
            date.setText(df.format(new java.util.Date(it.createdAtMillis)));
            qty.setText(String.valueOf(it.quantity));

            itemsContainer.addView(row);
            row.addView(name);
            row.addView(date);
            row.addView(qty);

            // 点击行 -> 弹出编辑/删除
            row.setOnClickListener(v -> showItemActions(it));
        }

        if (itemsContainer.getChildCount() == 0) {
            android.widget.TextView empty = new android.widget.TextView(this);
            empty.setText("No items yet");
            empty.setPadding(0, dp(6), 0, dp(6));
            itemsContainer.addView(empty);
        }
    }

    // 弹出操作选择
    private void showItemActions(InventoryItem item) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Item actions")
                .setItems(new CharSequence[]{"Edit", "Delete"}, (dialog, which) -> {
                    if (which == 0) {
                        showEditDialog(item);
                    } else if (which == 1) {
                        // 删除：取消提醒 + 移除数据
                        ExpiryReminderScheduler.cancelReminder(this, item.id);
                        InventoryRepository repo = InventoryRepository.getInstance(this);
                        repo.removeItem(item.id);
                        android.widget.Toast.makeText(this, "Deleted: " + item.name, android.widget.Toast.LENGTH_SHORT)
                                .show();
                        renderItems(selectedCategory);
                        updateOverview(selectedCategory);
                    }
                })
                .show();
    }

    // 编辑弹窗
    private void showEditDialog(InventoryItem item) {
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = dp(16);
        container.setPadding(pad, pad, pad, pad);

        android.widget.EditText etName = new android.widget.EditText(this);
        etName.setHint("Name");
        etName.setText(item.name);

        android.widget.EditText etQty = new android.widget.EditText(this);
        etQty.setHint("Quantity");
        etQty.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etQty.setText(String.valueOf(item.quantity));

        android.widget.TextView tvCat = new android.widget.TextView(this);
        tvCat.setText("Category");

        android.widget.Spinner spCat = new android.widget.Spinner(this);
        String[] cats = new String[]{"Vegetable", "Meat", "Fruit", "Other"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, cats);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCat.setAdapter(adapter);
        int idx = java.util.Arrays.asList(cats).indexOf(item.category);
        if (idx >= 0)
            spCat.setSelection(idx);

        android.widget.EditText etDays = new android.widget.EditText(this);
        etDays.setHint("Expiry days / remind days");
        etDays.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        container.addView(etName);
        container.addView(etQty);
        container.addView(tvCat);
        container.addView(spCat);
        container.addView(etDays);

        new android.app.AlertDialog.Builder(this)
                .setTitle("Edit item")
                .setView(container)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String qtyStr = etQty.getText().toString().trim();
                    int qty = 0;
                    try {
                        qty = Integer.parseInt(qtyStr);
                    } catch (Exception ignored) {
                    }
                    if (qty <= 0)
                        qty = item.quantity; // 简单保护

                    String cat = (String) spCat.getSelectedItem();
                    String daysStr = etDays.getText().toString().trim();

                    long now = System.currentTimeMillis();
                    Long expire = item.expireDateMillis;
                    boolean providedDays = !android.text.TextUtils.isEmpty(daysStr);

                    if (providedDays) {
                        try {
                            int days = Integer.parseInt(daysStr);
                            expire = now + days * 24L * 60 * 60 * 1000;
                        } catch (NumberFormatException ignored) {
                        }
                    }

                    InventoryRepository repo = InventoryRepository.getInstance(this);
                    InventoryItem updated = new InventoryItem(item.id, name.isEmpty() ? item.name : name, qty, cat,
                            expire, item.createdAtMillis);

                    // 重新设置提醒：先取消旧提醒，再按新值安排
                    ExpiryReminderScheduler.cancelReminder(this, item.id);
                    if (expire != null && providedDays) {
                        try {
                            int daysAhead = Integer.parseInt(daysStr);
                            long trigger = expire - daysAhead * 24L * 60 * 60 * 1000;
                            if (trigger > now) {
                                ExpiryReminderScheduler.scheduleReminder(this, updated.id, updated.name, trigger);
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }

                    repo.updateItem(updated);
                    android.widget.Toast.makeText(this, "Updated: " + updated.name, android.widget.Toast.LENGTH_SHORT)
                            .show();
                    renderItems(selectedCategory);
                    updateOverview(selectedCategory);
                })
                .show();
    }

    private int dp(int value) {
        float d = getResources().getDisplayMetrics().density;
        return (int) (value * d);
    }

    private void updateOverview(String category) {
        InventoryRepository repo = InventoryRepository.getInstance(this);
        java.util.List<InventoryItem> items = repo.getItems();

        long now = System.currentTimeMillis();
        long soonWindow = SOON_DAYS * 24L * 60 * 60 * 1000;

        int remaining = 0;
        int soon = 0;

        for (InventoryItem it : items) {
            if (!"All".equalsIgnoreCase(category) && !it.category.equalsIgnoreCase(category)) {
                continue;
            }
            remaining += Math.max(0, it.quantity);
            if (it.expireDateMillis != null) {
                long diff = it.expireDateMillis - now;
                if (diff >= 0 && diff <= soonWindow) {
                    soon++;
                }
            }
        }

        int consumed = 0;
        java.util.List<String> logs = repo.getLogs();
        for (String line : logs) {
            if (!line.contains(" | OUT | "))
                continue;
            if (!"All".equalsIgnoreCase(category)) {
                String[] parts = line.split(" \\| ");
                // 期望格式: time | OUT | name xqty | category
                if (parts.length >= 4) {
                    String cat = parts[3].trim();
                    if (!category.equalsIgnoreCase(cat))
                        continue;
                }
            }
            consumed++;
        }

        tvRemain.setText(String.valueOf(remaining));
        tvSoon.setText(String.valueOf(soon));
        tvConsumed.setText(String.valueOf(consumed));
    }
}