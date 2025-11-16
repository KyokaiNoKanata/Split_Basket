package com.example.split_basket;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.split_basket.data.InventoryRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Future;

public class HomeActivity extends AppCompatActivity {

    // Keys for Intent parameters, used to pass data between activities
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

    // Variables related to the reminder feature
    private TextView tvReminder1, tvReminder2;
    private RecyclerView recyclerViewReminders;
    private ReminderAdapter reminderAdapter;

    // Variables related to the log feature
    private RecyclerView recyclerViewStatus;
    private StatusLogAdapter statusLogAdapter;

    private HomeViewModel homeViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize ViewModel
        homeViewModel = new ViewModelProvider(this, new HomeViewModel.Factory(getApplication()))
                .get(HomeViewModel.class);

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

        // Inventory quick add dialog
        quickAdd.setOnClickListener(v -> showQuickAddDialog());

        // Simple overview dialog (can be connected to real data later)
        overview.setOnClickListener(v -> showOverviewDialog());

        // Shared shopping list, with multi-select and dynamic add/remove items
        newList.setOnClickListener(v -> showNewListDialog());

        // New Bill -> Quickly create a bill
        newBill.setOnClickListener(v -> showNewBillDialog());

        // Initialize reminder RecyclerView
        recyclerViewReminders = findViewById(R.id.recyclerViewReminders);
        reminderAdapter = new ReminderAdapter(new ArrayList<>());
        recyclerViewReminders.setAdapter(reminderAdapter);
        // Update reminder content
        homeViewModel.updateReminders();
        // Observe reminders LiveData
        homeViewModel.reminders.observe(this, reminderAdapter::setReminders);

        // Initialize log RecyclerView
        initStatusRecyclerView();
        // Load log data
        homeViewModel.loadLogs();
        // Observe logs LiveData (handle both list update and scrolling)
        homeViewModel.logs.observe(this, logs -> {
            statusLogAdapter.submitList(logs);
            // Scroll to latest log entry if list is not empty
            if (logs != null && !logs.isEmpty()) {
                recyclerViewStatus.post(() -> recyclerViewStatus.scrollToPosition(0));
            }
        });
    }

    // Bottom navigation

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
            finish(); // Finish the current Activity to avoid background stack accumulation
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
            String quantityStr = etQuantity.getText() != null ? etQuantity.getText().toString().trim() : "";

            if (name.isEmpty() || quantityStr.isEmpty()) {
                Toast.makeText(this, "Please fill in at least Name and Quantity", Toast.LENGTH_SHORT).show();
                return;
            }

            // Parse quantity to int
            int quantity = Integer.parseInt(quantityStr);

            // Create new InventoryItem with UUID
            String id = java.util.UUID.randomUUID().toString();
            InventoryItem item = new InventoryItem(id, name, quantity, "Other",
                    System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000), // Default 30 days
                    System.currentTimeMillis(), null); // null for photoUri

            // Add to inventory using ViewModel
            Future<Void> addItemFuture = homeViewModel.addInventoryItem(item);

            try {
                // Wait for the item to be added and log to be written
                addItemFuture.get();
                // Show success message
                Toast.makeText(this, "Item added to inventory", Toast.LENGTH_SHORT).show();

                // Update reminders
                homeViewModel.updateReminders();
                // Update logs immediately after adding item
                homeViewModel.loadLogs();
            } catch (Exception e) {
                // Show error message if something goes wrong
                Toast.makeText(this, "Failed to add item", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

            dialog.dismiss();
        });

        dialog.show();
    }

    // Overview Dialog

    private void showOverviewDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_overview_inventory, null);

        TextView tvOverviewContent = dialogView.findViewById(R.id.tvOverviewContent);
        MaterialButton btnClose = dialogView.findViewById(R.id.btnCloseOverview);

        // Get inventory data
        InventoryRepository inventoryRepo = InventoryRepository.getInstance(this);
        List<InventoryItem> items = inventoryRepo.getItems();

        // Calculate summary statistics
        int totalItems = items.size();
        long currentTime = System.currentTimeMillis();
        long sevenDays = 7 * 24 * 60 * 60 * 1000;
        int expiringSoon = 0;
        Map<String, Integer> itemsByCategory = new HashMap<>();

        for (InventoryItem item : items) {
            // Count expiring items (within 7 days)
            if (item.expireDateMillis != null && item.expireDateMillis <= currentTime + sevenDays
                    && item.expireDateMillis > currentTime) {
                expiringSoon++;
            }

            // Count items by category
            String category = item.category;
            itemsByCategory.put(category, itemsByCategory.getOrDefault(category, 0) + 1);
        }

        // Build overview text
        StringBuilder overviewText = new StringBuilder();
        overviewText.append("Total items in inventory: " + totalItems + "\n\n");

        if (itemsByCategory.isEmpty()) {
            overviewText.append("No items yet.");
        } else {
            overviewText.append("Items by category:\n");
            for (Map.Entry<String, Integer> entry : itemsByCategory.entrySet()) {
                overviewText.append("- " + entry.getKey() + ": " + entry.getValue() + "\n");
            }

            overviewText.append("\nItems expiring soon (within 7 days): " + expiringSoon);
        }

        tvOverviewContent.setText(overviewText.toString());

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

        // Dynamically add new items (name + quantity) as checkboxes
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

            // Long-press to delete this option
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

            // Pass new list items to ListActivity
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

            // Pass bill information to BillActivity
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

    @Override
    protected void onResume() {
        super.onResume();
        // Update reminders when returning to the main interface
        homeViewModel.updateReminders();
        // Reload logs when returning to the main interface
        loadLogs();
    }

    // Initialize log RecyclerView
    private void initStatusRecyclerView() {
        recyclerViewStatus = findViewById(R.id.recyclerViewStatus);
        statusLogAdapter = new StatusLogAdapter(this);
        recyclerViewStatus.setAdapter(statusLogAdapter);
        // Use the default LinearLayoutManager (already set in XML)
    }

    // Load log data - now handled by ViewModel
    private void loadLogs() {
        homeViewModel.loadLogs();
    }

    // Date formatting utility method
    private String formatDate(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(millis));
    }
}
