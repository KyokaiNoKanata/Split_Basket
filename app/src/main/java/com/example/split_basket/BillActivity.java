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

public class BillActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_BILL_DETAIL = 1001;
    private MaterialButton btnHome, btnInventory, btnList, btnBill;
    private BillRepository billStorage;
    private List<BillItem> billItems = new ArrayList<>();

    // UI元素引用
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

        // 初始化账单存储
        billStorage = BillRepository.getInstance(this);
        billStorage.ensureSeedData();

        // 使用LiveData观察账单数据变化
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

        // 初始化UI元素
        inputBillName = findViewById(R.id.inputBillName);
        inputTotalSpent = findViewById(R.id.inputTotalSpent);
        radioSplitMethod = findViewById(R.id.radioSplitMethod);
        radioEqual = findViewById(R.id.radioEqual);
        radioCustom = findViewById(R.id.radioCustom);
        layoutEqualSplit = findViewById(R.id.layoutEqualSplit);
        layoutCustomSplit = findViewById(R.id.layoutCustomSplit);
        tvPerPersonAmount = findViewById(R.id.tvPerPersonAmount);
        tvCalculatedTotal = findViewById(R.id.tvCalculatedTotal);

        // 平均付款模式的复选框
        checkBox1 = findViewById(R.id.checkBox1);
        checkBox2 = findViewById(R.id.checkBox2);
        checkBox3 = findViewById(R.id.checkBox3);
        checkBox4 = findViewById(R.id.checkBox4);

        // 自定义付款模式的复选框和输入框
        customCheckBox1 = findViewById(R.id.customCheckBox1);
        customCheckBox2 = findViewById(R.id.customCheckBox2);
        customCheckBox3 = findViewById(R.id.customCheckBox3);
        customCheckBox4 = findViewById(R.id.customCheckBox4);
        editUser1Amount = findViewById(R.id.editUser1Amount);
        editUser2Amount = findViewById(R.id.editUser2Amount);
        editUser3Amount = findViewById(R.id.editUser3Amount);
        editUser4Amount = findViewById(R.id.editUser4Amount);

        // 设置付款方式切换逻辑
        radioSplitMethod.setOnCheckedChangeListener((group, checkedId) -> {
            String mode;
            if (checkedId == R.id.radioEqual) {
                mode = "Equal";
                layoutEqualSplit.setVisibility(View.VISIBLE);
                layoutCustomSplit.setVisibility(View.GONE);
                inputTotalSpent.setEnabled(true); // 平均模式下启用总金额输入
            } else if (checkedId == R.id.radioCustom) {
                mode = "Custom";
                layoutEqualSplit.setVisibility(View.GONE);
                layoutCustomSplit.setVisibility(View.VISIBLE);
                inputTotalSpent.setEnabled(false); // 自定义模式下禁用总金额输入，由系统自动计算
            } else {
                mode = "Equal"; // 默认
                layoutEqualSplit.setVisibility(View.VISIBLE);
                layoutCustomSplit.setVisibility(View.GONE);
            }
            group.setTag(mode); // store current mode for later use
        });

        // 设置监听器
        setupEqualSplitListeners();
        setupCustomSplitListeners();

        // 默认选中平均付款
        radioEqual.setChecked(true);

        findViewById(R.id.btnCreateBill).setOnClickListener(v -> {
            createNewBill();
        });
    }

    // 从存储加载账单
    private void loadBillsFromStorage() {
        new Thread(() -> {
            try {
                List<BillItem> loadedBills = billStorage.getAllBills();

                runOnUiThread(() -> {
                    billItems = loadedBills;
                    // 隐藏XML中定义的静态账单卡片
                    hideStaticBillCards();

                    // 动态加载账单
                    displayBills();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(
                        () -> Toast.makeText(BillActivity.this, "Failed to load bills", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // 隐藏XML中定义的静态账单卡片
    // 设置平均付款模式下的监听器
    private void setupEqualSplitListeners() {
        // 监听总金额输入变化
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

        // 监听成员选择变化
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

    // 设置自定义付款模式下的监听器
    private void setupCustomSplitListeners() {
        // 为每个复选框设置监听器，控制对应金额输入框的可用性
        setupCustomMemberListener(customCheckBox1, editUser1Amount);
        setupCustomMemberListener(customCheckBox2, editUser2Amount);
        setupCustomMemberListener(customCheckBox3, editUser3Amount);
        setupCustomMemberListener(customCheckBox4, editUser4Amount);

        // 为每个金额输入框设置变化监听，用于计算总金额
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

    // 设置自定义模式下成员选择的监听器
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

    // 计算平均付款模式下每人应付金额
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

        // 计算选中的人数
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

    // 计算自定义付款模式下的总金额
    private void calculateCustomTotalAmount() {
        double total = 0;

        total += getAmountFromEditText(editUser1Amount);
        total += getAmountFromEditText(editUser2Amount);
        total += getAmountFromEditText(editUser3Amount);
        total += getAmountFromEditText(editUser4Amount);

        tvCalculatedTotal.setText(String.format("Total calculated: ¥%.2f", total));

        // 更新总金额输入框（虽然在自定义模式下它是禁用的）
        inputTotalSpent.setText(String.format("%.2f", total));
    }

    // 从EditText获取金额
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

    // 创建新账单
    private void createNewBill() {
        String name = inputBillName.getText().toString().trim();
        Object modeObj = radioSplitMethod.getTag();
        String mode = modeObj instanceof String ? (String) modeObj : "Equal"; // default to Equal

        // 验证输入
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter bill name", Toast.LENGTH_SHORT).show();
            return;
        }

        // 格式化当前日期为英文格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String creationDate = sdf.format(new Date());

        try {
            String total;

            if (radioEqual.isChecked()) {
                // 平均付款模式
                total = inputTotalSpent.getText().toString().trim();
                if (total.isEmpty()) {
                    Toast.makeText(this, "Please enter total amount", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 检查是否有选中的成员
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

                // 创建新账单对象
                String billId = "new_bill_" + System.currentTimeMillis();
                BillItem newBill = new BillItem(billId, name, total, "Unpaid", mode, creationDate);

                // 添加选中的参与者
                String[] participantNames = {"User1", "User2", "User3", "User4"};

                for (int i = 0; i < checkBoxes.length; i++) {
                    if (checkBoxes[i] != null && checkBoxes[i].isChecked()) {
                        newBill.addParticipant(participantNames[i]);
                    }
                }

                // 保存到存储
                billStorage.addBill(newBill);
            } else {
                // 自定义付款模式
                // 计算总金额
                double totalAmount = 0;

                // 创建新账单对象
                String billId = "new_bill_" + System.currentTimeMillis();
                BillItem newBill = new BillItem(billId, name, String.format("%.2f", totalAmount), "Unpaid", mode,
                        creationDate);

                // 添加选中的参与者和金额
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

                // 更新账单总金额
                newBill.setAmount(String.format("%.2f", totalAmount));

                // 保存到存储
                billStorage.addBill(newBill);
            }

            // 重置表单
            resetForm();

            Toast.makeText(this, "Bill created successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating bill", Toast.LENGTH_SHORT).show();
        }
    }

    // 重置表单
    private void resetForm() {
        inputBillName.setText("");
        inputTotalSpent.setText("");
        radioSplitMethod.clearCheck();
        radioEqual.setChecked(true); // 默认选中平均付款

        // 重置平均付款模式的复选框
        checkBox1.setChecked(false);
        checkBox2.setChecked(false);
        checkBox3.setChecked(false);
        checkBox4.setChecked(false);

        // 重置自定义付款模式的复选框和输入框
        customCheckBox1.setChecked(false);
        customCheckBox2.setChecked(false);
        customCheckBox3.setChecked(false);
        customCheckBox4.setChecked(false);
        editUser1Amount.setText("");
        editUser2Amount.setText("");
        editUser3Amount.setText("");
        editUser4Amount.setText("");

        // 重置显示文本
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

    // 显示所有账单
    private void displayBills() {
        try {
            // 获取账单列表容器
            View scrollContent = findViewById(R.id.scrollContent);
            if (scrollContent instanceof ScrollView) {
                View child = ((ScrollView) scrollContent).getChildAt(0);
                if (child instanceof LinearLayout scrollLayout) {

                    // 先移除动态添加的账单（保留标题和创建区域）
                    removeDynamicBills(scrollLayout);

                    // 找到最近账单标题
                    int recentBillsTitleIndex = -1;
                    for (int i = 0; i < scrollLayout.getChildCount(); i++) {
                        View view = scrollLayout.getChildAt(i);
                        if (view instanceof TextView && "Recent bills".equals(((TextView) view).getText().toString())) {
                            recentBillsTitleIndex = i;
                            break;
                        }
                    }

                    if (recentBillsTitleIndex != -1) {
                        // 先添加未支付账单
                        List<BillItem> unpaidBills = billStorage.getUnpaidBills();
                        for (BillItem bill : unpaidBills) {
                            MaterialCardView card = createBillCard(bill);
                            scrollLayout.addView(card, recentBillsTitleIndex + 1);
                            recentBillsTitleIndex++;
                        }

                        // 再添加已支付账单
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

    // 移除动态添加的账单卡片
    private void removeDynamicBills(LinearLayout layout) {
        List<View> viewsToRemove = new ArrayList<>();

        for (int i = 0; i < layout.getChildCount(); i++) {
            View view = layout.getChildAt(i);
            // 标记要移除的动态添加的账单卡片
            if (view instanceof MaterialCardView &&
                    view.getTag() != null && "dynamic_bill_card".equals(view.getTag().toString())) {
                viewsToRemove.add(view);
            }
        }

        // 移除所有标记的视图
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

    // 创建账单卡片
    private MaterialCardView createBillCard(BillItem bill) {
        MaterialCardView card = new MaterialCardView(this);
        card.setTag("dynamic_bill_card"); // 标记为动态创建的卡片

        // 设置卡片属性，与XML中定义的卡片保持一致
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 8); // 底部边距
        card.setLayoutParams(cardParams);
        card.setUseCompatPadding(true);
        card.setCardElevation(1);

        // 创建内部布局
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        mainLayout.setOrientation(LinearLayout.HORIZONTAL);
        mainLayout.setPadding(12, 12, 12, 12);

        // 左侧布局（账单名称和金额）
        LinearLayout leftLayout = new LinearLayout(this);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f);
        leftLayout.setLayoutParams(leftParams);
        leftLayout.setOrientation(LinearLayout.VERTICAL);

        // 账单名称
        TextView nameTv = new TextView(this);
        nameTv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        nameTv.setText(bill.getName());
        nameTv.setTypeface(null, android.graphics.Typeface.BOLD);
        nameTv.setTextSize(16);

        // 账单金额
        TextView amountTv = new TextView(this);
        amountTv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        amountTv.setText(bill.getAmount());
        amountTv.setTextSize(15);
        amountTv.setTypeface(null, android.graphics.Typeface.BOLD);

        leftLayout.addView(nameTv);
        leftLayout.addView(amountTv);

        // 状态文本
        TextView statusTv = new TextView(this);
        statusTv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        // 根据状态显示对应的英文文本并设置颜色
        String status = bill.getStatus();
        if ("已支付".equals(status) || "Paid".equals(status)) {
            statusTv.setText("Paid");
            statusTv.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else if ("未支付".equals(status) || "Unpaid".equals(status)) {
            statusTv.setText("Unpaid");
            statusTv.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else {
            statusTv.setText(status);
        }
        statusTv.setTypeface(null, android.graphics.Typeface.BOLD);

        mainLayout.addView(leftLayout);
        mainLayout.addView(statusTv);

        card.addView(mainLayout);

        // 设置点击事件，所有账单都可以点击查看详情
        card.setClickable(true);
        card.setOnClickListener(v -> {
            // 确保传递英文状态值
            String currentStatus = bill.getStatus();
            String englishStatus = currentStatus;
            if ("已支付".equals(currentStatus)) {
                englishStatus = "Paid";
            } else if ("未支付".equals(currentStatus)) {
                englishStatus = "Unpaid";
            }

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
                // 更新UI中的支付状态
                updateBillStatus(billId);
            } else if (resultCode == RESULT_OK) {
                // 账单被删除，重新加载和显示账单列表
                loadBillsFromStorage();
                displayBills();
            }
        }
    }

    private void updateBillStatus(String billId) {
        try {
            // 从存储中找到并更新账单
            BillItem bill = billStorage.getBillById(billId);
            if (bill != null) {
                bill.setStatus("Paid");
                billStorage.updateBill(bill);

                // 更新UI
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
        // 每次返回页面时重新加载账单数据，确保数据一致性
        loadBillsFromStorage();
    }

    // 辅助方法：设置Margin
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
            finish(); // 结束当前Activity，避免后台栈堆积
        }).start();
    }
}
