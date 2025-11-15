package com.example.split_basket;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import com.example.split_basket.data.BillRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class BillActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_BILL_DETAIL = 1001;
    private MaterialButton btnHome, btnInventory, btnList, btnBill;
    private BillRepository billStorage;
    private List<BillItem> billItems = new ArrayList<>();

    // UI element references
    private EditText inputBillName, inputTotalSpent;
    private RadioGroup radioSplitMethod;
    private RadioButton radioEqual, radioCustom;
    private LinearLayout layoutEqualSplit, layoutCustomSplit;
    private TextView tvPerPersonAmount, tvCalculatedTotal;
    private CheckBox checkBox1, checkBox2, checkBox3, checkBox4;
    private CheckBox customCheckBox1, customCheckBox2, customCheckBox3, customCheckBox4;
    private EditText editUser1Amount, editUser2Amount, editUser3Amount, editUser4Amount;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill);

        View scroll = findViewById(R.id.scrollContent);
        scroll.setAlpha(0f);
        scroll.animate().alpha(1f).setDuration(300).start();

        // Initialize bill storage
        billStorage = BillRepository.getInstance(this);
        billStorage.ensureSeedData();

        // Use LiveData to observe bill data changes
        billStorage.observeBills().observe(this, new Observer<List<BillItem>>() {
            @Override
            public void onChanged(List<BillItem> bills) {
                billItems = bills;
                hideStaticBillCards();
                displayBills();
            }
        });

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
            Toast.makeText(this, "Already on Bill page", Toast.LENGTH_SHORT).show();
        });

        // Initialize UI elements
        inputBillName = findViewById(R.id.inputBillName);
        inputTotalSpent = findViewById(R.id.inputTotalSpent);
        radioSplitMethod = findViewById(R.id.radioSplitMethod);
        radioEqual = findViewById(R.id.radioEqual);
        radioCustom = findViewById(R.id.radioCustom);
        layoutEqualSplit = findViewById(R.id.layoutEqualSplit);
        layoutCustomSplit = findViewById(R.id.layoutCustomSplit);
        tvPerPersonAmount = findViewById(R.id.tvPerPersonAmount);
        tvCalculatedTotal = findViewById(R.id.tvCalculatedTotal);

        // Checkboxes for equal payment mode
        checkBox1 = findViewById(R.id.checkBox1);
        checkBox2 = findViewById(R.id.checkBox2);
        checkBox3 = findViewById(R.id.checkBox3);
        checkBox4 = findViewById(R.id.checkBox4);

        // Checkboxes and input fields for custom payment mode
        customCheckBox1 = findViewById(R.id.customCheckBox1);
        customCheckBox2 = findViewById(R.id.customCheckBox2);
        customCheckBox3 = findViewById(R.id.customCheckBox3);
        customCheckBox4 = findViewById(R.id.customCheckBox4);
        editUser1Amount = findViewById(R.id.editUser1Amount);
        editUser2Amount = findViewById(R.id.editUser2Amount);
        editUser3Amount = findViewById(R.id.editUser3Amount);
        editUser4Amount = findViewById(R.id.editUser4Amount);

        // Handle intent data from HomeActivity
        Intent intent = getIntent();
        if (intent != null) {
            // Extract data from intent
            String billName = intent.getStringExtra(HomeActivity.EXTRA_BILL_NAME);
            String totalAmount = intent.getStringExtra(HomeActivity.EXTRA_BILL_TOTAL);
            String billDate = intent.getStringExtra(HomeActivity.EXTRA_BILL_DATE);
            int selectedModeId = intent.getIntExtra(HomeActivity.EXTRA_BILL_MODE, R.id.rbEqual);

            // Fill UI fields
            if (billName != null) {
                inputBillName.setText(billName);
            }
            if (totalAmount != null) {
                inputTotalSpent.setText(totalAmount);
            }
            if (billDate != null) {
                // There's currently no date input field in the UI; it will be used when
                // creating the bill
            }
            // Set payment method
            if (selectedModeId == R.id.rbEqual) {
                radioEqual.setChecked(true);
            } else if (selectedModeId == R.id.rbByQuantity || selectedModeId == R.id.rbByItem) {
                // The "split by quantity" and "split by item" modes don't exist in the current
                // design; default to custom split
                radioCustom.setChecked(true);
            } else {
                radioEqual.setChecked(true);
            }
        }

        // Set payment method switching logic
        radioSplitMethod.setOnCheckedChangeListener((group, checkedId) -> {
            String mode;
            if (checkedId == R.id.radioEqual) {
                mode = "Equal";
                layoutEqualSplit.setVisibility(View.VISIBLE);
                layoutCustomSplit.setVisibility(View.GONE);
                inputTotalSpent.setEnabled(true); // Enable total amount input in equal mode
            } else if (checkedId == R.id.radioCustom) {
                mode = "Custom";
                layoutEqualSplit.setVisibility(View.GONE);
                layoutCustomSplit.setVisibility(View.VISIBLE);
                inputTotalSpent.setEnabled(false); // Disable total amount input in custom mode, calculated
                // automatically by the system
            } else {
                mode = "Equal"; // Default
                layoutEqualSplit.setVisibility(View.VISIBLE);
                layoutCustomSplit.setVisibility(View.GONE);
            }
            group.setTag(mode); // store current mode for later use
        });

        // Set listeners
        setupEqualSplitListeners();
        setupCustomSplitListeners();

        // Default to equal payment
        radioEqual.setChecked(true);

        findViewById(R.id.btnCreateBill).setOnClickListener(v -> {
            createNewBill();
        });
    }

    // Load bills from storage
    private void loadBillsFromStorage() {
        new Thread(() -> {
            try {
                List<BillItem> loadedBills = billStorage.getAllBills();

                runOnUiThread(() -> {
                    billItems = loadedBills;
                    // Hide static bill cards defined in XML
                    hideStaticBillCards();

                    // Dynamically load bills
                    displayBills();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(
                        () -> Toast.makeText(BillActivity.this, "Failed to load bills", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // Hide static bill cards defined in XML
    // Set listeners for equal split payment mode
    private void setupEqualSplitListeners() {
        // Listen for changes in total amount input
        inputTotalSpent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                calculatePerPersonAmount();
            }
        });

        // Listen for changes in member selection
        CompoundButton.OnCheckedChangeListener checkListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                calculatePerPersonAmount();
            }
        };

        checkBox1.setOnCheckedChangeListener(checkListener);
        checkBox2.setOnCheckedChangeListener(checkListener);
        checkBox3.setOnCheckedChangeListener(checkListener);
        checkBox4.setOnCheckedChangeListener(checkListener);
    }

    // Set listeners for custom split payment mode
    private void setupCustomSplitListeners() {
        // Set listener for each checkbox to control the availability of the
        // corresponding amount input field
        setupCustomMemberListener(customCheckBox1, editUser1Amount);
        setupCustomMemberListener(customCheckBox2, editUser2Amount);
        setupCustomMemberListener(customCheckBox3, editUser3Amount);
        setupCustomMemberListener(customCheckBox4, editUser4Amount);

        // Set change listeners for each amount input field to calculate total amount
        TextWatcher amountTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                calculateCustomTotalAmount();
            }
        };

        editUser1Amount.addTextChangedListener(amountTextWatcher);
        editUser2Amount.addTextChangedListener(amountTextWatcher);
        editUser3Amount.addTextChangedListener(amountTextWatcher);
        editUser4Amount.addTextChangedListener(amountTextWatcher);
    }

    // Set listeners for member selection in custom mode
    private void setupCustomMemberListener(CheckBox checkBox, EditText editText) {
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                editText.setEnabled(isChecked);
                if (!isChecked) {
                    editText.setText("");
                }
                calculateCustomTotalAmount();
            }
        });
    }

    // Calculate amount per person in equal split payment mode
    private void calculatePerPersonAmount() {
        String totalSpentStr = inputTotalSpent.getText().toString().trim();
        if (totalSpentStr.isEmpty()) {
            tvPerPersonAmount.setText("Per person: ¥0.00");
            return;
        }

        double totalSpent;
        try {
            totalSpent = Double.parseDouble(totalSpentStr);
        } catch (NumberFormatException e) {
            tvPerPersonAmount.setText("Per person: ¥0.00");
            return;
        }

        // Calculate the number of selected members
        int checkedCount = 0;
        if (checkBox1.isChecked())
            checkedCount++;
        if (checkBox2.isChecked())
            checkedCount++;
        if (checkBox3.isChecked())
            checkedCount++;
        if (checkBox4.isChecked())
            checkedCount++;

        if (checkedCount > 0) {
            double perPerson = totalSpent / checkedCount;
            tvPerPersonAmount.setText(String.format("Per person: ¥%.2f", perPerson));
        } else {
            tvPerPersonAmount.setText("Per person: ¥0.00");
        }
    }

    // Calculate total amount in custom split payment mode
    private void calculateCustomTotalAmount() {
        double total = 0;

        total += getAmountFromEditText(editUser1Amount);
        total += getAmountFromEditText(editUser2Amount);
        total += getAmountFromEditText(editUser3Amount);
        total += getAmountFromEditText(editUser4Amount);

        tvCalculatedTotal.setText(String.format("Total calculated: ¥%.2f", total));

        // Update total amount input field (even though it's disabled in custom mode)
        inputTotalSpent.setText(String.format("%.2f", total));
    }

    // Get amount from EditText
    private double getAmountFromEditText(EditText editText) {
        String text = editText.getText().toString().trim();
        if (text.isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // Create a new bill
    private void createNewBill() {
        String name = inputBillName.getText().toString().trim();
        Object modeObj = radioSplitMethod.getTag();
        String mode = modeObj instanceof String ? (String) modeObj : "Equal"; // default to Equal

        // Validate input
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter bill name", Toast.LENGTH_SHORT).show();
            return;
        }

        // Format current date to English format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String creationDate = sdf.format(new Date());

        try {
            String total;

            if (radioEqual.isChecked()) {
                // Equal split payment mode
                total = inputTotalSpent.getText().toString().trim();
                if (total.isEmpty()) {
                    Toast.makeText(this, "Please enter total amount", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check if there are selected members
                CheckBox[] checkBoxes = new CheckBox[]{
                        checkBox1, checkBox2, checkBox3, checkBox4
                };

                boolean hasMembers = false;
                for (CheckBox cb : checkBoxes) {
                    if (cb != null && cb.isChecked()) {
                        hasMembers = true;
                        break;
                    }
                }

                if (!hasMembers) {
                    Toast.makeText(this, "Please select at least one member", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Create new bill object
                String billId = UUID.randomUUID().toString();
                BillItem newBill = new BillItem(billId, name, total, "Unpaid", mode, creationDate);

                // Add selected participants
                String[] participantNames = {"User1", "User2", "User3", "User4"};

                for (int i = 0; i < checkBoxes.length; i++) {
                    if (checkBoxes[i] != null && checkBoxes[i].isChecked()) {
                        newBill.addParticipant(participantNames[i]);
                    }
                }

                // Save to storage
                billStorage.addBill(newBill);
            } else {
                // Custom split payment mode
                // Calculate total amount
                double totalAmount = 0;

                // Create new bill object
                String billId = UUID.randomUUID().toString();
                BillItem newBill = new BillItem(billId, name, String.format("%.2f", totalAmount), "Unpaid", mode,
                        creationDate);

                // Add selected participants and amounts
                if (customCheckBox1.isChecked()) {
                    double amount = getAmountFromEditText(editUser1Amount);
                    if (amount <= 0) {
                        Toast.makeText(this, "Please enter valid amount for User1", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    newBill.addParticipant("User1");
                    newBill.addCustomAmount(amount);
                    totalAmount += amount;
                }
                if (customCheckBox2.isChecked()) {
                    double amount = getAmountFromEditText(editUser2Amount);
                    if (amount <= 0) {
                        Toast.makeText(this, "Please enter valid amount for User2", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    newBill.addParticipant("User2");
                    newBill.addCustomAmount(amount);
                    totalAmount += amount;
                }
                if (customCheckBox3.isChecked()) {
                    double amount = getAmountFromEditText(editUser3Amount);
                    if (amount <= 0) {
                        Toast.makeText(this, "Please enter valid amount for User3", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    newBill.addParticipant("User3");
                    newBill.addCustomAmount(amount);
                    totalAmount += amount;
                }
                if (customCheckBox4.isChecked()) {
                    double amount = getAmountFromEditText(editUser4Amount);
                    if (amount <= 0) {
                        Toast.makeText(this, "Please enter valid amount for User4", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    newBill.addParticipant("User4");
                    newBill.addCustomAmount(amount);
                    totalAmount += amount;
                }

                if (totalAmount <= 0) {
                    Toast.makeText(this, "Please select at least one member with amount", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Update bill total amount
                newBill.setAmount(String.format("%.2f", totalAmount));

                // Save to storage
                billStorage.addBill(newBill);
            }

            // Reset form
            resetForm();

            Toast.makeText(this, "Bill created successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating bill", Toast.LENGTH_SHORT).show();
        }
    }

    // Reset form
    private void resetForm() {
        inputBillName.setText("");
        inputTotalSpent.setText("");
        radioSplitMethod.clearCheck();
        radioEqual.setChecked(true); // Default to equal split payment

        // Reset checkboxes for equal split payment mode
        checkBox1.setChecked(false);
        checkBox2.setChecked(false);
        checkBox3.setChecked(false);
        checkBox4.setChecked(false);

        // Reset checkboxes and input fields for custom split payment mode
        customCheckBox1.setChecked(false);
        customCheckBox2.setChecked(false);
        customCheckBox3.setChecked(false);
        customCheckBox4.setChecked(false);
        editUser1Amount.setText("");
        editUser2Amount.setText("");
        editUser3Amount.setText("");
        editUser4Amount.setText("");

        // Reset display text
        tvPerPersonAmount.setText("Per person: ¥0.00");
        tvCalculatedTotal.setText("Total calculated: ¥0.00");
    }

    private void hideStaticBillCards() {
        View cardUnpaidBill = findViewById(R.id.cardUnpaidBill);
        View cardPaidBill1 = findViewById(R.id.cardPaidBill1);
        View cardPaidBill2 = findViewById(R.id.cardPaidBill2);

        if (cardUnpaidBill != null)
            cardUnpaidBill.setVisibility(View.GONE);
        if (cardPaidBill1 != null)
            cardPaidBill1.setVisibility(View.GONE);
        if (cardPaidBill2 != null)
            cardPaidBill2.setVisibility(View.GONE);
    }

    // Display all bills
    private void displayBills() {
        try {
            // Get bill list container
            View scrollContent = findViewById(R.id.scrollContent);
            if (scrollContent instanceof ScrollView) {
                View child = ((ScrollView) scrollContent).getChildAt(0);
                if (child instanceof LinearLayout scrollLayout) {

                    // First remove dynamically added bills (keep title and creation area)
                    removeDynamicBills(scrollLayout);

                    // Find recent bills title
                    int recentBillsTitleIndex = -1;
                    for (int i = 0; i < scrollLayout.getChildCount(); i++) {
                        View view = scrollLayout.getChildAt(i);
                        if (view instanceof TextView && "Recent bills".equals(((TextView) view).getText().toString())) {
                            recentBillsTitleIndex = i;
                            break;
                        }
                    }

                    if (recentBillsTitleIndex != -1) {
                        // First add unpaid bills
                        List<BillItem> unpaidBills = billStorage.getUnpaidBills();
                        for (BillItem bill : unpaidBills) {
                            MaterialCardView card = createBillCard(bill);
                            scrollLayout.addView(card, recentBillsTitleIndex + 1);
                            recentBillsTitleIndex++;
                        }

                        // Then add paid bills
                        List<BillItem> paidBills = billStorage.getPaidBills();
                        for (BillItem bill : paidBills) {
                            MaterialCardView card = createBillCard(bill);
                            scrollLayout.addView(card, recentBillsTitleIndex + 1);
                            recentBillsTitleIndex++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to display bills", Toast.LENGTH_SHORT).show();
        }
    }

    // Remove dynamically added bill cards
    private void removeDynamicBills(LinearLayout layout) {
        List<View> viewsToRemove = new ArrayList<>();

        for (int i = 0; i < layout.getChildCount(); i++) {
            View view = layout.getChildAt(i);
            // Mark dynamically added bill cards to remove
            if (view instanceof MaterialCardView &&
                    view.getTag() != null && "dynamic_bill_card".equals(view.getTag().toString())) {
                viewsToRemove.add(view);
            }
        }

        // Remove all marked views
        for (View view : viewsToRemove) {
            layout.removeView(view);
        }
    }

    private void openBillDetail(String name, String amount, String status, String method, String billId) {
        Intent intent = new Intent(this, BillDetailActivity.class);
        intent.putExtra(BillDetailActivity.EXTRA_BILL_NAME, name);
        intent.putExtra(BillDetailActivity.EXTRA_BILL_AMOUNT, amount);
        intent.putExtra(BillDetailActivity.EXTRA_BILL_STATUS, status);
        intent.putExtra(BillDetailActivity.EXTRA_BILL_METHOD, method);
        intent.putExtra(BillDetailActivity.EXTRA_BILL_ID, billId);
        startActivityForResult(intent, REQUEST_CODE_BILL_DETAIL);
    }

    // Create bill card
    private MaterialCardView createBillCard(BillItem bill) {
        MaterialCardView card = new MaterialCardView(this);
        card.setTag("dynamic_bill_card"); // Mark as dynamically created card

        // Set card properties to match those defined in XML
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 8); // Bottom margin
        card.setLayoutParams(cardParams);
        card.setUseCompatPadding(true);
        card.setCardElevation(1);

        // Create inner layout
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        mainLayout.setOrientation(LinearLayout.HORIZONTAL);
        mainLayout.setPadding(12, 12, 12, 12);

        // Left layout (bill name and amount)
        LinearLayout leftLayout = new LinearLayout(this);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f);
        leftLayout.setLayoutParams(leftParams);
        leftLayout.setOrientation(LinearLayout.VERTICAL);

        // Bill name
        TextView nameTv = new TextView(this);
        nameTv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        nameTv.setText(bill.getName());
        nameTv.setTypeface(null, android.graphics.Typeface.BOLD);
        nameTv.setTextSize(16);

        // Bill amount
        TextView amountTv = new TextView(this);
        amountTv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        amountTv.setText(bill.getAmount());
        amountTv.setTextSize(15);
        amountTv.setTypeface(null, android.graphics.Typeface.BOLD);

        leftLayout.addView(nameTv);
        leftLayout.addView(amountTv);

        // Status text
        TextView statusTv = new TextView(this);
        statusTv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        // Display corresponding English text based on status and set color
        String status = bill.getStatus();
        if ("Paid".equals(status)) {
            statusTv.setText("Paid");
            statusTv.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else if ("Unpaid".equals(status)) {
            statusTv.setText("Unpaid");
            statusTv.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else {
            statusTv.setText(status);
        }
        statusTv.setTypeface(null, android.graphics.Typeface.BOLD);

        mainLayout.addView(leftLayout);
        mainLayout.addView(statusTv);

        card.addView(mainLayout);

        // Set click event for all bills to view details
        card.setClickable(true);
        card.setOnClickListener(v -> {
            // Ensure to pass English status value
            String currentStatus = bill.getStatus();
            String englishStatus = currentStatus;

            openBillDetail(
                    bill.getName(),
                    bill.getAmount(),
                    englishStatus,
                    bill.getMethod(),
                    bill.getId());
        });

        return card;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_BILL_DETAIL && data != null) {
            String billId = data.getStringExtra(BillDetailActivity.EXTRA_BILL_ID);

            if (resultCode == BillDetailActivity.RESULT_BILL_PAID) {
                // Update payment status in UI
                updateBillStatus(billId);
            } else if (resultCode == RESULT_OK) {
                // Bill deleted, reload and display bill list
                loadBillsFromStorage();
                displayBills();
            }
        }
    }

    private void updateBillStatus(String billId) {
        try {
            // Find and update bill from storage
            BillItem bill = billStorage.getBillById(billId);
            if (bill != null) {
                bill.setStatus("Paid");
                billStorage.updateBill(bill);

                // Update UI
                displayBills();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error updating bill status", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload bill data every time returning to page to ensure data consistency
        loadBillsFromStorage();
    }

    // Helper method: Set margin
    private void setMargin(View view, int left, int top, int right, int bottom) {
        if (view != null) {
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(left, top, right, bottom);
            view.setLayoutParams(params);
        }
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
            Intent intent = new Intent(BillActivity.this, targetActivity);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish(); // End current Activity to avoid background stack accumulation
        }).start();
    }
}
