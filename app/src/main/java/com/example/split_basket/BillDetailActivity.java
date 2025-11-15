package com.example.split_basket;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.example.split_basket.data.BillRepository;

import java.text.DecimalFormat;
import java.util.List;

public class BillDetailActivity extends AppCompatActivity {

    public static final String EXTRA_BILL_NAME = "bill_name";
    public static final String EXTRA_BILL_AMOUNT = "bill_amount";
    public static final String EXTRA_BILL_STATUS = "bill_status";
    public static final String EXTRA_BILL_METHOD = "bill_method";
    public static final String EXTRA_BILL_ID = "bill_id";
    public static final int RESULT_BILL_PAID = 1001;
    
    private String billId;
    private boolean isPaid = false;
    private BillRepository billStorage;
    private BillItem currentBill;
    private boolean isCustomAmountMode = false;
    
    // 用于格式化金额显示
    private DecimalFormat df = new DecimalFormat("0.00");

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_detail);

        try {
            // 初始化账单存储
        billStorage = BillRepository.getInstance(this);
            
            // 获取传入的账单信息
            Intent intent = getIntent();
            if (intent == null) {
                Toast.makeText(this, "Cannot retrieve bill information", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            
            // 安全地获取账单信息，提供默认值
            String billName = intent.getStringExtra(EXTRA_BILL_NAME);
            if (billName == null) billName = "Unnamed Bill";
            
            String billAmount = intent.getStringExtra(EXTRA_BILL_AMOUNT);
            if (billAmount == null) billAmount = "$ 0.00";
            
            String billStatus = intent.getStringExtra(EXTRA_BILL_STATUS);
            if (billStatus == null) billStatus = "Unpaid";
            
            String billMethod = intent.getStringExtra(EXTRA_BILL_METHOD);
            if (billMethod == null) billMethod = "Equal Split";
            
            billId = intent.getStringExtra(EXTRA_BILL_ID);
            if (billId == null) billId = "unknown_bill_" + System.currentTimeMillis();
            
            isPaid = "Paid".equals(billStatus);
            
            // 从存储中获取完整账单信息
            currentBill = billStorage.getBillById(billId);
            if (currentBill == null) {
                // 如果存储中没有，创建一个新的
                currentBill = new BillItem(billId, billName, billAmount, billStatus, billMethod, new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()));
            }
            
            // 检查是否为自定义金额模式
            isCustomAmountMode = "Custom".equals(currentBill.getMethod());

            // 显示账单信息
            TextView tvBillName = findViewById(R.id.tvBillName);
            TextView tvBillAmount = findViewById(R.id.tvBillAmount);
            TextView tvPaymentStatus = findViewById(R.id.tvPaymentStatus);
            TextView tvSplitMethod = findViewById(R.id.tvSplitMethod);
            
            // 安全地设置文本
            if (tvBillName != null) tvBillName.setText(currentBill.getName());
            if (tvBillAmount != null) {
                String amount = currentBill.getAmount();
                // 确保显示人民币符号
                if (!amount.startsWith("¥")) {
                    // 如果包含$符号，替换掉
                    amount = amount.replace("$", "¥");
                    if (!amount.startsWith("¥")) {
                        amount = "¥ " + amount;
                    }
                }
                tvBillAmount.setText(amount);
            }
            if (tvPaymentStatus != null) {
                // 根据状态显示对应的英文文本
                String status = currentBill.getStatus();
                if ("已支付".equals(status) || "Paid".equals(status)) {
                    tvPaymentStatus.setText("Paid");
                    tvPaymentStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                } else if ("未支付".equals(status) || "Unpaid".equals(status)) {
                    tvPaymentStatus.setText("Unpaid");
                    tvPaymentStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                } else {
                    tvPaymentStatus.setText(status);
                }
            }
            if (tvSplitMethod != null) {
                // 根据分账方式显示对应的英文文本
                String method = currentBill.getMethod();
                if ("Equal".equals(method) || "平均分配".equals(method)) {
                    tvSplitMethod.setText("Equal Split");
                } else if ("Custom".equals(method) || "自定义".equals(method)) {
                    tvSplitMethod.setText("Custom");
                } else if ("按数量".equals(method)) {
                    tvSplitMethod.setText("By Quantity");
                } else if ("按项目".equals(method)) {
                    tvSplitMethod.setText("By Item");
                } else {
                    tvSplitMethod.setText(method);
                }
            }
            
            // 显示创建日期
            TextView tvCreationDateValue = findViewById(R.id.tvCreationDateValue);
            if (tvCreationDateValue != null && currentBill != null && currentBill.getCreationDate() != null) {
                tvCreationDateValue.setText(currentBill.getCreationDate());
            }

            // 设置返回按钮
            MaterialButton btnBack = findViewById(R.id.btnBack);
            if (btnBack != null) {
                btnBack.setOnClickListener(v -> finish());
            }

            // 设置支付按钮
            MaterialButton btnPay = findViewById(R.id.btnPayBill);
            if (btnPay != null) {
                if (isPaid) {
                    btnPay.setVisibility(View.GONE);
                }
                
                btnPay.setOnClickListener(v -> {
                    try {
                        // 保存自定义金额（如果有）
                        if (isCustomAmountMode) {
                            saveCustomAmounts();
                        }
                        
                        // 更新账单状态
                        if (currentBill != null) {
                            currentBill.setStatus("Paid");
                            billStorage.updateBill(currentBill);
                        }
                        
                        // 将账单标记为已支付
                        isPaid = true;
                        // 确保当前账单状态设置为英文的Paid
                        if (currentBill != null) {
                            currentBill.setStatus("Paid");
                        }
                        if (tvPaymentStatus != null) {
                            tvPaymentStatus.setText("Paid");
                            tvPaymentStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                        }
                        if (btnPay != null) {
                            btnPay.setVisibility(View.GONE);
                        }
                        
                        Toast.makeText(this, "Bill paid successfully", Toast.LENGTH_SHORT).show();
                        
                        // 设置结果并返回
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra(EXTRA_BILL_ID, billId);
                        setResult(RESULT_BILL_PAID, resultIntent);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Payment failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            // 设置删除账单按钮
            MaterialButton btnDelete = findViewById(R.id.btnDeleteBill);
            if (btnDelete != null) {
                btnDelete.setOnClickListener(v -> {
                    try {
                        // 从数据库中删除账单
                        if (billStorage != null && billId != null) {
                            billStorage.deleteBill(billId);
                            Toast.makeText(this, "Bill deleted successfully", Toast.LENGTH_SHORT).show();
                            
                            // 设置结果并返回
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra(EXTRA_BILL_ID, billId);
                            setResult(RESULT_OK, resultIntent);
                            
                            // 关闭当前活动
                            finish();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            // 添加参与者信息
            addParticipants();
        } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error loading bill details", Toast.LENGTH_SHORT).show();
                finish(); // 出错时关闭页面
            }
    }
    
    private void addParticipants() {
        try {
            LinearLayout container = findViewById(R.id.participantsContainer);
            if (container == null) {
                return; // 如果找不到容器，直接返回
            }
            
            // 清空容器，避免重复添加
            container.removeAllViews();
            
            // 获取参与者列表
            List<String> participants = currentBill.getParticipants();
            List<Double> customAmounts = currentBill.getCustomAmounts();
            
            if (participants.isEmpty()) {
                // If no participants, use default 4 participants
                String[] defaultParticipants = {"User1", "User2", "User3", "User4"};
                for (String participant : defaultParticipants) {
                    currentBill.addParticipant(participant);
                }
                participants = currentBill.getParticipants();
            }
            
            // 计算平均金额
            double averageAmount = currentBill.getAverageAmount();
            
            // 添加参与者
            for (int i = 0; i < participants.size(); i++) {
                String participant = participants.get(i);
                
                LinearLayout participantRow = new LinearLayout(this);
                participantRow.setOrientation(LinearLayout.HORIZONTAL);
                participantRow.setPadding(12, 12, 12, 12);
                
                TextView nameTv = new TextView(this);
                LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                nameTv.setLayoutParams(nameParams);
                nameTv.setText(participant);
                nameTv.setTextSize(14);
                
                if (isCustomAmountMode && !isPaid) {
                    // 自定义金额模式且未支付时，显示输入框
                    EditText amountEdit = new EditText(this);
                    LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                    amountEdit.setLayoutParams(editParams);
                    amountEdit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    
                    // 设置默认值
                    if (i < customAmounts.size() && customAmounts.get(i) > 0) {
                        amountEdit.setText(df.format(customAmounts.get(i)));
                    } else {
                        amountEdit.setText(df.format(averageAmount));
                    }
                    
                    // 设置提示文本
                    amountEdit.setHint("输入金额");
                    
                    // 设置标签以便后续获取值
                    amountEdit.setTag("amount_edit_" + i);
                    
                    participantRow.addView(nameTv);
                    participantRow.addView(amountEdit);
                } else {
                    // 其他情况显示文本
                    TextView amountTv = new TextView(this);
                    LinearLayout.LayoutParams amountParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                    amountTv.setLayoutParams(amountParams);
                    
                    // 显示金额
                    if (i < customAmounts.size() && customAmounts.get(i) > 0) {
                        amountTv.setText("¥ " + df.format(customAmounts.get(i)));
                    } else {
                        amountTv.setText("¥ " + df.format(averageAmount));
                    }
                    amountTv.setTextSize(14);
                    
                    participantRow.addView(nameTv);
                    participantRow.addView(amountTv);
                }
                
                container.addView(participantRow);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 参与者信息加载失败不影响主要功能
        }
    }
    
    // 保存自定义金额
    private void saveCustomAmounts() {
        try {
            if (currentBill == null) return;
            
            LinearLayout container = findViewById(R.id.participantsContainer);
            if (container == null) return;
            
            List<String> participants = currentBill.getParticipants();
            // 清空自定义金额列表
            currentBill.getCustomAmounts().clear();
            
            for (int i = 0; i < participants.size(); i++) {
                EditText editText = container.findViewWithTag("amount_edit_" + i);
                if (editText != null) {
                    String text = editText.getText().toString();
                    if (!text.isEmpty()) {
                        try {
                            double amount = Double.parseDouble(text);
                            currentBill.addCustomAmount(amount);
                        } catch (NumberFormatException e) {
                            // 如果格式错误，使用0
                            currentBill.addCustomAmount(0.0);
                        }
                    } else {
                        currentBill.addCustomAmount(0.0);
                    }
                }
            }
            
            // 更新存储
            billStorage.updateBill(currentBill);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving custom amounts", Toast.LENGTH_SHORT).show();
        }
    }
}