package com.example.split_basket;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.radiobutton.MaterialRadioButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity {

    // 用于 Intent 传参的 key，方便其他页面接收
    public static final String EXTRA_INVENTORY_NAME = "com.example.split_basket.EXTRA_INVENTORY_NAME";
    public static final String EXTRA_INVENTORY_DATE = "com.example.split_basket.EXTRA_INVENTORY_DATE";
    public static final String EXTRA_INVENTORY_QUANTITY = "com.example.split_basket.EXTRA_INVENTORY_QUANTITY";
    public static final String EXTRA_INVENTORY_USER = "com.example.split_basket.EXTRA_INVENTORY_USER";

    public static final String EXTRA_NEW_LIST_ITEMS = "com.example.split_basket.EXTRA_NEW_LIST_ITEMS";

    public static final String EXTRA_BILL_NAME = "com.example.split_basket.EXTRA_BILL_NAME";
    public static final String EXTRA_BILL_TOTAL = "com.example.split_basket.EXTRA_BILL_TOTAL";
    public static final String EXTRA_BILL_DATE = "com.example.split_basket.EXTRA_BILL_DATE";
    public static final String EXTRA_BILL_MODE = "com.example.split_basket.EXTRA_BILL_MODE";

    private MaterialButton btnHome, btnInventory, btnList, btnBill;

    // 新增提醒功能相关变量
    private TextView tvReminder1, tvReminder2;
    private MaterialCardView cardReminder1, cardReminder2;
    // 新增日志功能相关变量
    private RecyclerView recyclerViewStatus;
    private StatusLogAdapter statusLogAdapter;

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

        // 库存添加弹窗
        quickAdd.setOnClickListener(v -> showQuickAddDialog());

        // 简单汇总/说明弹窗（之后可以接真实数据）
        overview.setOnClickListener(v -> showOverviewDialog());

        // 共享购物清单，多选列表 + 动态添加/删除项目
        newList.setOnClickListener(v -> showNewListDialog());

        // New Bill → 快速创建账单
        newBill.setOnClickListener(v -> showNewBillDialog());

        // 初始化提醒文本框
        tvReminder1 = findViewById(R.id.tvReminder1);
        tvReminder2 = findViewById(R.id.tvReminder2);
        // 初始化提醒卡片
        cardReminder1 = findViewById(R.id.cardReminder1);
        cardReminder2 = findViewById(R.id.cardReminder2);
        // 更新提醒内容
        updateReminders();
        // 初始化日志RecyclerView
        initStatusRecyclerView();
        // 加载日志数据
        loadLogs();
    }

    // 底部导航

    private void updateButtonStates(MaterialButton selectedButton) {
        btnHome.setChecked(false);
        btnInventory.setChecked(false);
        btnList.setChecked(false);
        btnBill.setChecked(false);
        selectedButton.setChecked(true);
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

    // Quick Add Dialog

    private void showQuickAddDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_quickadd_inventory, null);

        TextInputEditText etName = dialogView.findViewById(R.id.etItemName);
        TextInputEditText etDate = dialogView.findViewById(R.id.etPurchaseDate);
        TextInputEditText etQuantity = dialogView.findViewById(R.id.etQuantity);
        TextInputEditText etUser = dialogView.findViewById(R.id.etUserName);

        MaterialButton btnConfirm = dialogView.findViewById(R.id.btnConfirmQuickAdd);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelQuickAdd);

        final androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String date = etDate.getText() != null ? etDate.getText().toString().trim() : "";
            String quantity = etQuantity.getText() != null ? etQuantity.getText().toString().trim() : "";
            String user = etUser.getText() != null ? etUser.getText().toString().trim() : "";

            if (name.isEmpty() || quantity.isEmpty()) {
                Toast.makeText(this, "Please fill in at least Name and Quantity", Toast.LENGTH_SHORT).show();
                return;
            }

            // 将数据通过 Intent 传递到 InventoryActivity
            Intent intent = new Intent(HomeActivity.this, InventoryActivity.class);
            intent.putExtra(EXTRA_INVENTORY_NAME, name);
            intent.putExtra(EXTRA_INVENTORY_DATE, date);
            intent.putExtra(EXTRA_INVENTORY_QUANTITY, quantity);
            intent.putExtra(EXTRA_INVENTORY_USER, user);
            startActivity(intent);

            dialog.dismiss();
        });

        dialog.show();
    }

    // Overview Dialog

    private void showOverviewDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_overview_inventory, null);

        MaterialButton btnClose = dialogView.findViewById(R.id.btnCloseOverview);

        final androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // New List Dialog

    private void showNewListDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_new_shoppinglist, null);

        TextInputEditText etItemName = dialogView.findViewById(R.id.etListItemName);
        TextInputEditText etItemQuantity = dialogView.findViewById(R.id.etListItemQuantity);
        MaterialButton btnAddItem = dialogView.findViewById(R.id.btnAddItem);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btnConfirmNewList);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelNewList);
        LinearLayout container = dialogView.findViewById(R.id.containerListItems);

        final androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // 动态添加新的选项（物品 + 数量），以多选框形式展示
        btnAddItem.setOnClickListener(v -> {
            String name = etItemName.getText() != null ? etItemName.getText().toString().trim() : "";
            String quantity = etItemQuantity.getText() != null ? etItemQuantity.getText().toString().trim() : "";

            if (name.isEmpty()) {
                Toast.makeText(this, "Item name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            String label = quantity.isEmpty()
                    ? name
                    : name + " (x" + quantity + ")";

            MaterialCheckBox checkBox = new MaterialCheckBox(this);
            checkBox.setText(label);
            checkBox.setChecked(true);
            checkBox.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            // 长按删除该选项
            checkBox.setOnLongClickListener(view -> {
                container.removeView(view);
                Toast.makeText(this, "Removed item: " + label, Toast.LENGTH_SHORT).show();
                return true;
            });

            container.addView(checkBox);
            etItemName.setText("");
            etItemQuantity.setText("");
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            int count = container.getChildCount();
            if (count == 0) {
                Toast.makeText(this, "Please add at least one item", Toast.LENGTH_SHORT).show();
                return;
            }

            ArrayList<String> selectedItems = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                View child = container.getChildAt(i);
                if (child instanceof MaterialCheckBox cb) {
                    if (cb.isChecked()) {
                        selectedItems.add(cb.getText().toString());
                    }
                }
            }

            if (selectedItems.isEmpty()) {
                Toast.makeText(this, "Please select at least one item", Toast.LENGTH_SHORT).show();
                return;
            }

            // 将新清单条目传递到 ListActivity
            Intent intent = new Intent(HomeActivity.this, ListActivity.class);
            intent.putStringArrayListExtra(EXTRA_NEW_LIST_ITEMS, selectedItems);
            startActivity(intent);

            dialog.dismiss();
        });

        dialog.show();
    }

    // New Bill Dialog

    private void showNewBillDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_new_bill, null);

        TextInputEditText etBillName = dialogView.findViewById(R.id.etBillName);
        TextInputEditText etTotalSpend = dialogView.findViewById(R.id.etTotalSpend);
        TextInputEditText etBillDate = dialogView.findViewById(R.id.etBillDate);

        MaterialRadioButton rbEqual = dialogView.findViewById(R.id.rbEqual);
        MaterialRadioButton rbByQuantity = dialogView.findViewById(R.id.rbByQuantity);
        MaterialRadioButton rbByItem = dialogView.findViewById(R.id.rbByItem);

        MaterialButton btnConfirm = dialogView.findViewById(R.id.btnConfirmNewBill);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelNewBill);

        final androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String billName = etBillName.getText() != null ? etBillName.getText().toString().trim() : "";
            String totalSpend = etTotalSpend.getText() != null ? etTotalSpend.getText().toString().trim() : "";
            String billDate = etBillDate.getText() != null ? etBillDate.getText().toString().trim() : "";

            if (billName.isEmpty() || totalSpend.isEmpty()) {
                Toast.makeText(this, "Please fill in Bill name and Total spend", Toast.LENGTH_SHORT).show();
                return;
            }

            String splitMode = "Equal";
            if (rbByQuantity.isChecked()) {
                splitMode = "By quantity";
            } else if (rbByItem.isChecked()) {
                splitMode = "By item";
            }

            // 将账单信息传递到 BillActivity
            Intent intent = new Intent(HomeActivity.this, BillActivity.class);
            intent.putExtra(EXTRA_BILL_NAME, billName);
            intent.putExtra(EXTRA_BILL_TOTAL, totalSpend);
            intent.putExtra(EXTRA_BILL_DATE, billDate);
            intent.putExtra(EXTRA_BILL_MODE, splitMode);
            startActivity(intent);

            dialog.dismiss();
        });

        dialog.show();
    }

    // 新增提醒功能核心逻辑
    private void updateReminders() {
        // 获取库存数据
        InventoryRepository inventoryRepo = new InventoryRepository(this);
        List<InventoryItem> items = inventoryRepo.getItems();

        // 获取账单数据
        BillStorage billStorage = new BillStorage(this);
        List<BillItem> unpaidBills = billStorage.getUnpaidBills();

        // 检查即将过期的库存商品（7天内）
        long currentTime = System.currentTimeMillis();
        long sevenDays = 7 * 24 * 60 * 60 * 1000;
        List<String> inventoryReminders = new ArrayList<>();

        for (InventoryItem item : items) {
            if (item.expireDateMillis != null && item.expireDateMillis <= currentTime + sevenDays
                    && item.expireDateMillis > currentTime) {
                String dateStr = formatDate(item.expireDateMillis);
                inventoryReminders.add(item.name + " will expire on " + dateStr + ".");
            }
        }

        // 检查未支付的账单
        List<String> billReminders = new ArrayList<>();
        if (!unpaidBills.isEmpty()) {
            for (BillItem bill : unpaidBills) {
                billReminders.add("Unpaid bill: " + bill.getName() + " (" + bill.getAmount() + ")");
            }
        }

        // 更新提醒界面
        if (!inventoryReminders.isEmpty()) {
            tvReminder1.setText(inventoryReminders.get(0));
            cardReminder1.setVisibility(View.VISIBLE);
        } else {
            cardReminder1.setVisibility(View.GONE);
        }

        if (!billReminders.isEmpty()) {
            tvReminder2.setText(billReminders.get(0));
            cardReminder2.setVisibility(View.VISIBLE);
        } else {
            cardReminder2.setVisibility(View.GONE);
        }
    }

    // 初始化日志RecyclerView
    private void initStatusRecyclerView() {
        recyclerViewStatus = findViewById(R.id.recyclerViewStatus);
        statusLogAdapter = new StatusLogAdapter(this);
        recyclerViewStatus.setAdapter(statusLogAdapter);
        // 使用默认的LinearLayoutManager（已在XML中设置）
    }

    // 加载日志数据
    private void loadLogs() {
        EventLogManager eventLogManager = EventLogManager.getInstance(this);
        List<EventLogManager.LogEntry> logs = eventLogManager.getLogs();
        statusLogAdapter.submitList(logs);
    }

    // 日期格式化工具方法
    private String formatDate(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(millis));
    }
}