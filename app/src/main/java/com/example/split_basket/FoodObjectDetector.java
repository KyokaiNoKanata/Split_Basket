package com.example.split_basket;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

record FoodDetectionResult(String label, float score) {
}

public class FoodObjectDetector {
    private final Context appContext;
    // COCO SSD MobileNet v1 量化模型：输入 300x300x3 的 UINT8
    private final int inputSize = 300;
    private final float scoreThreshold = 0.6f;
    private final boolean isQuantized = true;
    private Interpreter interpreter;
    private List<String> labels = new ArrayList<>();

    public FoodObjectDetector(Context context) {
        this.appContext = context.getApplicationContext();
        try {
            // 使用 COCO SSD MobileNet v1 的 tflite 与标签文件
            this.interpreter = new Interpreter(loadModelFile("detect.tflite"), new Interpreter.Options());
            this.labels = loadLabels("labelmap.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        if (interpreter != null) {
            interpreter = null;
        }
    }

    public FoodDetectionResult detectTopLabel(android.net.Uri imageUri) {
        if (interpreter == null)
            return null;
        try {
            Bitmap bitmap = decodeUri(imageUri);
            if (bitmap == null)
                return null;
            Bitmap cropped = centerCropToSquare(bitmap);
            Bitmap scaled = Bitmap.createScaledBitmap(cropped, inputSize, inputSize, true);
            ByteBuffer input = isQuantized ? convertBitmapToUint8(scaled) : convertBitmapToFloat(scaled);

            // SSD 输出张量：locations [1][N][4], classes [1][N], scores [1][N], numDetections
            // [1]
            int numDetections = 10; // 常见默认输出数量
            float[][][] locations = new float[1][numDetections][4];
            float[][] classes = new float[1][numDetections];
            float[][] scores = new float[1][numDetections];
            float[] detections = new float[1];

            Map<Integer, Object> outputs = new HashMap<>();
            outputs.put(0, locations);
            outputs.put(1, classes);
            outputs.put(2, scores);
            outputs.put(3, detections);

            Object[] inputs = new Object[] { input };
            interpreter.runForMultipleInputsOutputs(inputs, outputs);

            // 取最高分的类别
            int bestIndex = -1;
            float bestScore = 0f;
            for (int i = 0; i < numDetections; i++) {
                float s = scores[0][i];
                if (s > bestScore) {
                    bestScore = s;
                    bestIndex = i;
                }
            }
            if (bestIndex >= 0 && bestScore >= scoreThreshold) {
                // 标签索引自适应（COCO 的 labelmap 第一行通常是 "???", 类索引从 1 开始）
                int labelIdx = (int) classes[0][bestIndex];
                if (!labels.isEmpty() && !"???".equals(labels.get(0)) && labelIdx > 0) {
                    labelIdx -= 1;
                }
                // Food.AI 的标签文件第一行通常为 "???"
                String label = labelIdx >= 0 && labelIdx < labels.size() ? labels.get(labelIdx) : "unknown";
                return new FoodDetectionResult(label, bestScore);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private MappedByteBuffer loadModelFile(String assetName) throws Exception {
        AssetFileDescriptor fileDescriptor = appContext.getAssets().openFd(assetName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private List<String> loadLabels(String assetName) throws Exception {
        List<String> result = new ArrayList<>();
        try (InputStream is = appContext.getAssets().open(assetName);
                BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                result.add(line.trim());
            }
        }
        return result;
    }

    private ByteBuffer convertBitmapToUint8(Bitmap bitmap) {
        ByteBuffer input = ByteBuffer.allocateDirect(inputSize * inputSize * 3);
        int[] intValues = new int[inputSize * inputSize];
        bitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize);
        int pixel = 0;
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                int val = intValues[pixel++];
                input.put((byte) ((val >> 16) & 0xFF)); // R
                input.put((byte) ((val >> 8) & 0xFF)); // G
                input.put((byte) (val & 0xFF)); // B
            }
        }
        input.rewind();
        return input;
    }

    private ByteBuffer convertBitmapToFloat(Bitmap bitmap) {
        ByteBuffer input = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3);
        input.order(ByteOrder.nativeOrder());
        int[] intValues = new int[inputSize * inputSize];
        bitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize);

        int pixel = 0;
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                int val = intValues[pixel++];
                float r = ((val >> 16) & 0xFF) / 255.0f;
                float g = ((val >> 8) & 0xFF) / 255.0f;
                float b = (val & 0xFF) / 255.0f;
                input.putFloat(r);
                input.putFloat(g);
                input.putFloat(b);
            }
        }
        input.rewind();
        return input;
    }

    private Bitmap decodeUri(android.net.Uri uri) throws Exception {
        try (java.io.InputStream is = appContext.getContentResolver().openInputStream(uri)) {
            Bitmap bm = android.graphics.BitmapFactory.decodeStream(is);
            if (bm == null)
                return null;
            try (java.io.InputStream eis = appContext.getContentResolver().openInputStream(uri)) {
                if (eis != null) {
                    androidx.exifinterface.media.ExifInterface exif = new androidx.exifinterface.media.ExifInterface(
                            eis);
                    int o = exif.getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                            androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL);
                    bm = rotateBitmapIfRequired(bm, o);
                }
            } catch (Exception ignore) {
            }
            return bm;
        }
    }

    private Bitmap centerCropToSquare(Bitmap src) {
        int w = src.getWidth(), h = src.getHeight();
        int size = Math.min(w, h);
        int x = (w - size) / 2, y = (h - size) / 2;
        return Bitmap.createBitmap(src, x, y, size, size);
    }

    private Bitmap rotateBitmapIfRequired(Bitmap bm, int orientation) {
        android.graphics.Matrix m = new android.graphics.Matrix();
        switch (orientation) {
            case androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90:
                m.postRotate(90);
                break;
            case androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180:
                m.postRotate(180);
                break;
            case androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270:
                m.postRotate(270);
                break;
            case androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                m.preScale(-1, 1);
                break;
            case androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_VERTICAL:
                m.preScale(1, -1);
                break;
            default:
                return bm;
        }
        return Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
    }
}