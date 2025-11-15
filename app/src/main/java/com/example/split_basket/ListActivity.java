package com.example.split_basket;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListActivity extends AppCompatActivity implements ShoppingListAdapter.ItemInteractionListener {

    private final List<ShoppingItem> currentItems = new ArrayList<>();
    private MaterialButton btnHome, btnInventory, btnList, btnBill;
    private TextView textSummary;
    private TextView textEmptyState;
    private ShoppingListAdapter adapter;
    private ShoppingListViewModel viewModel;
    private ShoppingItem recentlyDeletedItem;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        View scroll = findViewById(R.id.scrollContent);
        scroll.setAlpha(0f);
        scroll.animate().alpha(1f).setDuration(300).start();

        setupBottomNavigation();
        setupShoppingList();
        setupRecommendations();
        setupActions();
    }

    private void setupBottomNavigation() {
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
        btnList.setOnClickListener(v -> updateButtonStates(btnList));
        btnBill.setOnClickListener(v -> {
            updateButtonStates(btnBill);
            navigateTo(BillActivity.class);
        });
    }

    private void setupShoppingList() {
        textSummary = findViewById(R.id.textSummary);
        textEmptyState = findViewById(R.id.textEmptyState);
        RecyclerView recyclerView = findViewById(R.id.recyclerShoppingList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ShoppingListAdapter(this, this);
        recyclerView.setAdapter(adapter);
        attachSwipeHelper(recyclerView);

        viewModel = new ViewModelProvider(this).get(ShoppingListViewModel.class);
        viewModel.getItems().observe(this, items -> {
            currentItems.clear();
            if (items != null) {
                currentItems.addAll(items);
            }
            adapter.submitList(new ArrayList<>(currentItems));
            refreshSummary();
            refreshRecommendations();
        });
    }

    private void setupRecommendations() {
        // Initial population based on current items (observer will also refresh)
        refreshRecommendations();
    }

    private void setupActions() {
        findViewById(R.id.btnAddItems).setOnClickListener(v -> showAddItemDialog());
        findViewById(R.id.btnSettlement).setOnClickListener(v -> showSettlementDialog());
    }

    private void addQuickRecommendation(@NonNull String itemName) {
        if (viewModel == null)
            return;
        viewModel.addItem(itemName, 1, getString(R.string.default_added_by), (success, message) -> {
            String toastMessage = success
                    ? getString(R.string.chip_added_feedback, itemName, 1)
                    : message;
            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
        });
    }

    private void showAddItemDialog() {
        showAddItemDialog(null);
    }

    private void showAddItemDialog(@Nullable String prefillName) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_item, null, false);
        TextInputEditText inputName = dialogView.findViewById(R.id.inputItemName);
        TextInputEditText inputQuantity = dialogView.findViewById(R.id.inputQuantity);
        TextInputEditText inputAddedBy = dialogView.findViewById(R.id.inputAddedBy);

        if (!TextUtils.isEmpty(prefillName) && inputName != null) {
            inputName.setText(prefillName);
            inputName.setSelection(prefillName.length());
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.add_item_title)
                .setView(dialogView)
                .setPositiveButton(R.string.action_add, (dialog, which) -> {
                    String name = inputName.getText() != null ? inputName.getText().toString().trim() : "";
                    String quantityText = inputQuantity.getText() != null ? inputQuantity.getText().toString().trim()
                            : "";
                    String addedBy = inputAddedBy.getText() != null ? inputAddedBy.getText().toString().trim() : "";

                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(this, R.string.error_item_name_required, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int quantity;
                    try {
                        quantity = Integer.parseInt(TextUtils.isEmpty(quantityText) ? "1" : quantityText);
                    } catch (NumberFormatException e) {
                        quantity = 1;
                    }

                    if (quantity <= 0) {
                        quantity = 1;
                    }

                    if (TextUtils.isEmpty(addedBy)) {
                        addedBy = getString(R.string.default_added_by);
                    }

                    if (viewModel != null) {
                        viewModel.addItem(name, quantity, addedBy, this::handleAddResult);
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void handleAddResult(boolean success, @NonNull String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void refreshRecommendations() {
        ChipGroup group = findViewById(R.id.chipGroupRecommend);
        if (group == null)
            return;
        group.removeAllViews();

        // Count frequency of names (case-insensitive)
        java.util.Map<String, Integer> freq = new java.util.HashMap<>();
        java.util.Map<String, String> canonical = new java.util.HashMap<>();
        for (ShoppingItem item : currentItems) {
            String name = item.getName() == null ? "" : item.getName().trim();
            if (name.isEmpty())
                continue;
            String key = name.toLowerCase();
            Integer currentCount = freq.get(key);
            int newCount = (currentCount != null ? currentCount : 0) + 1;
            freq.put(key, newCount);
            // preserve first-seen original casing for display
            if (!canonical.containsKey(key))
                canonical.put(key, name);
        }

        // Build sorted list by count desc
        List<java.util.Map.Entry<String, Integer>> entries = new java.util.ArrayList<>(freq.entrySet());
        Collections.sort(entries, (a, b) -> Integer.compare(b.getValue(), a.getValue()));

        int maxWidth = group.getWidth();
        if (maxWidth == 0) {
            // Approximate using screen width minus typical paddings (16dp both sides)
            int screen = getResources().getDisplayMetrics().widthPixels;
            int margin = (int) (16 * getResources().getDisplayMetrics().density) * 2;
            maxWidth = Math.max(0, screen - margin);
        }

        int spacing = 0;
        try {
            spacing = (Integer) ChipGroup.class.getMethod("getChipSpacingHorizontal").invoke(group);
        } catch (Exception ignored) {
        }

        int lineWidth = 0;
        int added = 0;
        java.util.List<String> candidates = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, Integer> e : entries) {
            // 替换getOrDefault以支持API 23
            candidates.add(canonical.containsKey(e.getKey()) ? canonical.get(e.getKey()) : e.getKey());
        }
        if (candidates.isEmpty()) {
            candidates.add(getString(R.string.bread));
            candidates.add(getString(R.string.tissue));
            candidates.add(getString(R.string.eggs));
        }

        for (String display : candidates) {
            Chip temp = new Chip(this);
            temp.setText(display);
            temp.setCheckable(false);
            int specW = View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST);
            int specH = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            temp.measure(specW, specH);
            int w = temp.getMeasuredWidth();
            int nextWidth = added == 0 ? w : lineWidth + spacing + w;
            if (nextWidth > maxWidth) {
                break; // do not exceed one line
            }
            addRecommendationChip(group, display);
            lineWidth = nextWidth;
            added++;
        }
    }

    private void addRecommendationChip(@NonNull ChipGroup group, @NonNull String name) {
        Chip chip = new Chip(this);
        chip.setText(name);
        chip.setCheckable(false);
        chip.setClickable(true);
        chip.setOnClickListener(v -> showAddItemDialog(name));
        group.addView(chip);
    }

    private void showSettlementDialog() {
        List<ShoppingItem> remaining = new ArrayList<>();
        for (ShoppingItem item : currentItems) {
            if (!item.isPurchased()) {
                remaining.add(item);
            }
        }

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settlement_title);

        if (remaining.isEmpty()) {
            dialogBuilder
                    .setMessage(getString(R.string.no_remaining_items))
                    .setPositiveButton(android.R.string.ok, null);
        } else {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < remaining.size(); i++) {
                ShoppingItem item = remaining.get(i);
                builder.append(item.getName()).append(" x").append(item.getQuantity());
                if (i < remaining.size() - 1) {
                    builder.append('\n');
                }
            }
            String message = getString(R.string.settlement_message, remaining.size(), builder.toString());
            dialogBuilder
                    .setMessage(message)
                    .setPositiveButton(R.string.action_mark_all_purchased, (dialog, which) -> {
                        if (viewModel != null) {
                            // Persist to DB using bulk update by IDs; UI will refresh via LiveData
                            List<Long> ids = new ArrayList<>(remaining.size());
                            for (ShoppingItem it : remaining)
                                ids.add(it.getId());
                            viewModel.markItemsPurchasedByIds(ids);
                            Toast.makeText(this, R.string.action_mark_all_purchased, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null);
        }

        dialogBuilder.show();
    }

    private void refreshSummary() {
        int total = currentItems.size();
        int purchased = 0;
        for (ShoppingItem item : currentItems) {
            if (item.isPurchased())
                purchased++;
        }
        textSummary.setText(getString(R.string.total_purchased_template, total, purchased));
        textEmptyState.setVisibility(total == 0 ? View.VISIBLE : View.GONE);
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
            Intent intent = new Intent(ListActivity.this, targetActivity);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        }).start();
    }

    @Override
    public void onItemChecked(@NonNull ShoppingItem item, boolean isChecked, int position) {
        if (viewModel != null) {
            viewModel.setPurchased(item, isChecked);
        }
    }

    @Override
    public void onItemLongPressed(@NonNull ShoppingItem item, int position) {
        showEditItemDialog(item);
    }

    private void attachSwipeHelper(@NonNull RecyclerView recyclerView) {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION)
                    return;
                ShoppingItem item = adapter.getItemAt(position);
                if (item != null) {
                    deleteItemWithUndo(item);
                }
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }

    private void deleteItemWithUndo(@NonNull ShoppingItem item) {
        if (viewModel == null)
            return;
        recentlyDeletedItem = snapshotItem(item);
        viewModel.deleteItem(item);
        Snackbar snackbar = Snackbar.make(findViewById(R.id.scrollContent),
                getString(R.string.item_deleted, item.getName()), Snackbar.LENGTH_LONG);
        snackbar.setAnchorView(R.id.bottomBar);
        snackbar.setAction(R.string.action_undo, v -> {
            if (viewModel != null && recentlyDeletedItem != null) {
                viewModel.restoreItem(recentlyDeletedItem);
                recentlyDeletedItem = null;
            }
        });
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                    recentlyDeletedItem = null;
                }
            }
        });
        snackbar.show();
    }

    private void showEditItemDialog(@NonNull ShoppingItem item) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_item, null, false);
        TextInputEditText inputName = dialogView.findViewById(R.id.inputItemName);
        TextInputEditText inputQuantity = dialogView.findViewById(R.id.inputQuantity);
        TextInputEditText inputAddedBy = dialogView.findViewById(R.id.inputAddedBy);

        if (inputName != null)
            inputName.setText(item.getName());
        if (inputQuantity != null)
            inputQuantity.setText(String.valueOf(item.getQuantity()));
        if (inputAddedBy != null)
            inputAddedBy.setText(item.getAddedBy());

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.edit_item_title)
                .setView(dialogView)
                .setPositiveButton(R.string.action_save, (dialog, which) -> {
                    String name = inputName != null && inputName.getText() != null
                            ? inputName.getText().toString().trim()
                            : "";
                    String quantityText = inputQuantity != null && inputQuantity.getText() != null
                            ? inputQuantity.getText().toString().trim()
                            : "";
                    String addedBy = inputAddedBy != null && inputAddedBy.getText() != null
                            ? inputAddedBy.getText().toString().trim()
                            : "";

                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(this, R.string.error_item_name_required, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int quantity;
                    try {
                        quantity = Integer.parseInt(TextUtils.isEmpty(quantityText) ? "1" : quantityText);
                    } catch (NumberFormatException e) {
                        quantity = 1;
                    }

                    if (quantity <= 0) {
                        quantity = 1;
                    }

                    if (TextUtils.isEmpty(addedBy)) {
                        addedBy = getString(R.string.default_added_by);
                    }

                    if (viewModel != null) {
                        viewModel.updateItemDetails(item, name, quantity, addedBy);
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private ShoppingItem snapshotItem(@NonNull ShoppingItem item) {
        ShoppingItem copy = new ShoppingItem();
        copy.setId(item.getId());
        copy.setName(item.getName());
        copy.setAddedBy(item.getAddedBy());
        copy.setQuantity(item.getQuantity());
        copy.setPurchased(item.isPurchased());
        copy.setCreatedAt(item.getCreatedAt());
        copy.setInventoryItemId(item.getInventoryItemId());
        return copy;
    }
}
