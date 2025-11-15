package com.example.split_basket;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class InventoryAddActivity extends AppCompatActivity {
    private static final int REQ_CAMERA = 101;
    private static final int REQ_NOTIF = 102;

    private EditText inputName, inputQty, inputDays;
    private Spinner spinnerCat;
    private MaterialButton btnCamera, btnSave;

    private android.graphics.Bitmap captured; // 保留，占位用
    private android.net.Uri currentPhotoUri;  // 新增：拍照输出的 Uri
    private java.io.File currentPhotoFile;    // 新增：拍照输出的文件
    private FoodObjectDetector foodDetector;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory_add);

        inputName = findViewById(R.id.inputName);
        inputQty = findViewById(R.id.inputQty);
        inputDays = findViewById(R.id.inputDays);
        spinnerCat = findViewById(R.id.spinnerCategory);
        btnCamera = findViewById(R.id.btnCamera);
        btnSave = findViewById(R.id.btnSave);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"Vegetable", "Meat", "Fruit", "Other"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCat.setAdapter(adapter);

        // 初始化食物检测器，避免拍照返回时发生空指针
        foodDetector = new FoodObjectDetector(getApplicationContext());

        btnCamera.setOnClickListener(v -> {
            // 使用 FileProvider 拍摄原图到指定文件
            try {
                currentPhotoFile = createImageFile();
                currentPhotoUri = androidx.core.content.FileProvider.getUriForFile(
                        this, getPackageName() + ".fileprovider", currentPhotoFile
                );
                Intent cam = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                        .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                cam.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                startActivityForResult(cam, REQ_CAMERA);
            } catch (Exception e) {
                Toast.makeText(this, "Failed to open camera", Toast.LENGTH_SHORT).show();
            }
        });

        btnSave.setOnClickListener(v -> {
            String name = inputName.getText().toString().trim();
            String qtyStr = inputQty.getText().toString().trim();
            int qty = android.text.TextUtils.isEmpty(qtyStr) ? 1 : Integer.parseInt(qtyStr);
            String cat = (String) spinnerCat.getSelectedItem();
            String daysStr = inputDays.getText().toString().trim();

            long now = System.currentTimeMillis();
            Long expire = null;
            if (!android.text.TextUtils.isEmpty(daysStr)) {
                try {
                    int days = Integer.parseInt(daysStr);
                    expire = now + days * 24L * 60 * 60 * 1000;
                } catch (NumberFormatException ignored) {
                }
            }
            if (android.text.TextUtils.isEmpty(name)) {
                name = "Unnamed";
            }

            InventoryRepository repo = new InventoryRepository(this);
            InventoryItem item = new InventoryItem(String.valueOf(now), name, qty, cat, expire, now);
            // 新增：保存照片 Uri
            if (currentPhotoUri != null) {
                item.photoUri = currentPhotoUri.toString();
            }
            repo.addItem(item);

            // 智能提醒：按用户输入的“提前天数”触发（这里用 daysStr 作为提前天数）
            if (expire != null && !android.text.TextUtils.isEmpty(daysStr)) {
                try {
                    int daysAhead = Integer.parseInt(daysStr);
                    long trigger = expire - daysAhead * 24L * 60 * 60 * 1000;
                    if (trigger > now) {
                        ExpiryReminderScheduler.scheduleReminder(this, item.id, item.name, trigger);
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            Toast.makeText(this, "Saved: " + item.name, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private java.io.File createImageFile() throws java.io.IOException {
        String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new java.util.Date());
        String fileName = "IMG_" + timeStamp;
        java.io.File storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        return java.io.File.createTempFile(fileName, ".jpg", storageDir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CAMERA && resultCode == RESULT_OK) {
            if (currentPhotoUri != null && foodDetector != null) {
                FoodDetectionResult result = foodDetector.detectTopLabel(currentPhotoUri);
                if (result != null && result.label() != null && result.score() >= 0.6f) {
                    inputName.setText(result.label());            // 优先使用检测器的具体类别（如 orange）
                    setSpinnerCategoryByLabel(result.label());
                    Toast.makeText(this, "识别结果：" + result.label(), Toast.LENGTH_SHORT).show();
                } else {
                    // 检测器无结果或低分，再尝试 ML Kit 标签器
                    runImageLabeling(currentPhotoUri);
                }
            } else {
                runImageLabeling(currentPhotoUri);
            }
        } else {
            inputName.setText("Captured Item");
            Toast.makeText(this, "Photo saved", Toast.LENGTH_SHORT).show();
        }
    }

    private void runImageLabeling(android.net.Uri photoUri) {
        try {
            com.google.mlkit.vision.common.InputImage image =
                    com.google.mlkit.vision.common.InputImage.fromFilePath(this, photoUri);
            com.google.mlkit.vision.label.ImageLabeler labeler =
                    com.google.mlkit.vision.label.ImageLabeling.getClient(
                            new com.google.mlkit.vision.label.defaults.ImageLabelerOptions.Builder().build()
                    );
            labeler.process(image)
                    .addOnSuccessListener(labels -> {
                        if (labels == null || labels.isEmpty()) {
                            inputName.setText("Captured Item");
                            Toast.makeText(this, "No label detected", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // 取最高置信度标签作为名称建议
                        com.google.mlkit.vision.label.ImageLabel top = labels.get(0);
                        String suggested = top.getText();
                        if (android.text.TextUtils.isEmpty(inputName.getText().toString().trim())) {
                            inputName.setText(suggested);
                        }

                        // 根据标签集合推断类别并设置 Spinner
                        String cat = inferCategory(labels);
                        setSpinnerCategory(cat);

                        Toast.makeText(this, "Detected: " + suggested, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        inputName.setText("Captured Item");
                        Toast.makeText(this, "Labeling failed", Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            inputName.setText("Captured Item");
            Toast.makeText(this, "Image read failed", Toast.LENGTH_SHORT).show();
        }
    }

    private String inferCategory(java.util.List<com.google.mlkit.vision.label.ImageLabel> labels) {
        java.util.Set<String> texts = new java.util.HashSet<>();
        for (com.google.mlkit.vision.label.ImageLabel l : labels) {
            texts.add(l.getText().toLowerCase());
        }
        // 简单规则映射，可按需扩充
        if (containsAny(texts, "beef", "pork", "chicken", "meat", "lamb")) return "Meat";
        if (containsAny(texts, "apple", "banana", "orange", "grape", "fruit")) return "Fruit";
        if (containsAny(texts, "cabbage", "carrot", "lettuce", "broccoli", "vegetable"))
            return "Vegetable";
        return "Other";
    }

    private boolean containsAny(java.util.Set<String> set, String... keys) {
        for (String k : keys) if (set.contains(k)) return true;
        return false;
    }

    private void setSpinnerCategory(String category) {
        // spinnerCat 的 adapter 内容是 ["Vegetable", "Meat", "Fruit", "Other"]
        String[] cats = new String[]{"Vegetable", "Meat", "Fruit", "Other"};
        int index = java.util.Arrays.asList(cats).indexOf(category);
        if (index >= 0) spinnerCat.setSelection(index);
    }

    private void setSpinnerCategoryByLabel(String label) {
        // 使用现有的 spinnerCat
        if (label == null || spinnerCat == null || spinnerCat.getAdapter() == null) return;
        String l = label.toLowerCase();
        int index;

        if (containsAny(l, "apple", "banana", "orange", "grape", "peach", "pear", "mango", "strawberry", "pineapple", "lemon", "lime")) {
            index = findSpinnerIndex("Fruit");
        } else if (containsAny(l, "broccoli", "carrot", "cucumber", "tomato", "onion", "potato", "lettuce", "pepper")) {
            index = findSpinnerIndex("Vegetable");
        } else if (containsAny(l, "beef", "chicken", "pork", "fish", "shrimp", "meat")) {
            index = findSpinnerIndex("Meat");
        } else {
            index = findSpinnerIndex("Other");
        }
        if (index >= 0) spinnerCat.setSelection(index);
    }

    private boolean containsAny(String text, String... keys) {
        for (String k : keys) if (text.contains(k)) return true;
        return false;
    }

    private int findSpinnerIndex(String targetText) {
        if (spinnerCat == null || spinnerCat.getAdapter() == null) return -1;
        for (int i = 0; i < spinnerCat.getAdapter().getCount(); i++) {
            Object item = spinnerCat.getAdapter().getItem(i);
            if (item != null && targetText.equalsIgnoreCase(item.toString())) {
                return i;
            }
        }
        return -1;
    }

    private void ensureNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(
                        this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        REQ_NOTIF
                );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @Nullable String[] permissions, @Nullable int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIF) {
            boolean granted = grantResults != null
                    && grantResults.length > 0
                    && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED;
            android.widget.Toast.makeText(this, granted ? "已授予通知权限" : "未授予通知权限，提醒可能无法显示", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
}