package com.example.mobileproject01;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

final class ScreenshotRepository {
    private static final String PREFS = "snapnest_prefs";
    private static final String KEY_PROVIDER = "ai_provider";
    private static final String PROVIDER_GEMINI = "gemini";
    private static final String PROVIDER_SILICONFLOW = "siliconflow";
    private static final String KEY_GEMINI_API_KEY = "gemini_api_key";
    private static final String KEY_SILICONFLOW_API_KEY = "siliconflow_api_key";
    private static final String KEY_SILICONFLOW_MODEL = "siliconflow_model";

    private final Context context;
    private final RecordStore store;
    private final GeminiImageAnalyzer geminiAnalyzer = new GeminiImageAnalyzer();

    ScreenshotRepository(Context context) {
        this.context = context.getApplicationContext();
        this.store = new RecordStore(this.context);
    }

    List<ScreenshotRecord> loadRecords() {
        return store.loadAll();
    }

    void saveRecord(ScreenshotRecord record) {
        store.update(record);
    }

    void deleteRecord(long id) {
        store.delete(id);
    }

    ScreenshotRecord importFromUri(Uri uri, String sourceLabel) throws Exception {
        long createdAt = System.currentTimeMillis();
        File recordsDir = new File(context.getFilesDir(), "screenshots");
        if (!recordsDir.exists() && !recordsDir.mkdirs()) {
            throw new IOException("无法创建截图目录");
        }

        String baseName = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(createdAt);
        File originalFile = new File(recordsDir, "shot_" + baseName + guessExtension(uri));
        copyUriToFile(uri, originalFile);

        File thumbFile = new File(recordsDir, "thumb_" + baseName + ".jpg");
        createThumbnail(originalFile, thumbFile);

        ParsedScreenshot parsed = analyzeScreenshot(originalFile);
        ScreenshotRecord record = new ScreenshotRecord(
                0L,
                parsed.title,
                parsed.address,
                parsed.phone,
                parsed.rawText,
                parsed.notes,
                originalFile.getAbsolutePath(),
                thumbFile.getAbsolutePath(),
                sourceLabel,
                createdAt);

        long id = store.insert(record);
        return record.withId(id);
    }

    private ParsedScreenshot analyzeScreenshot(File originalFile) throws Exception {
        String provider = getProvider();
        String apiKey = getApiKey(provider);
        if (TextUtils.isEmpty(apiKey)) {
            throw new IOException("请先填写当前模型的 API Key");
        }
        if (PROVIDER_SILICONFLOW.equals(provider)) {
            return geminiAnalyzer.analyzeSiliconFlow(originalFile, apiKey, getSiliconFlowModel());
        }
        return geminiAnalyzer.analyze(originalFile, apiKey);
    }

    private String getProvider() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_PROVIDER, PROVIDER_SILICONFLOW);
    }

    private String getApiKey(String provider) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String keyName = PROVIDER_SILICONFLOW.equals(provider) ? KEY_SILICONFLOW_API_KEY : KEY_GEMINI_API_KEY;
        return prefs.getString(keyName, "");
    }

    private String getSiliconFlowModel() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String model = prefs.getString(KEY_SILICONFLOW_MODEL, GeminiImageAnalyzer.DEFAULT_SILICONFLOW_MODEL);
        if ("zai-org/GLM-4.6V".equals(model)
                || "Qwen/Qwen2.5-VL-7B-Instruct".equals(model)
                || "Qwen/Qwen3-VL-Embedding-8B".equals(model)) {
            return GeminiImageAnalyzer.DEFAULT_SILICONFLOW_MODEL;
        }
        return model;
    }

    private void copyUriToFile(Uri uri, File outputFile) throws IOException {
        try (InputStream input = context.getContentResolver().openInputStream(uri);
             FileOutputStream output = new FileOutputStream(outputFile)) {
            if (input == null) {
                throw new IOException("无法读取截图");
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            output.flush();
        }
    }

    private void createThumbnail(File source, File thumbFile) throws IOException {
        Bitmap bitmap = decodeSampledBitmap(source.getAbsolutePath(), 480, 480);
        if (bitmap == null) {
            throw new IOException("无法生成缩略图");
        }
        try (FileOutputStream output = new FileOutputStream(thumbFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output);
        } finally {
            bitmap.recycle();
        }
    }

    private Bitmap decodeSampledBitmap(String path, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return Math.max(1, inSampleSize);
    }

    private String guessExtension(Uri uri) {
        String mime = context.getContentResolver().getType(uri);
        if (mime == null) {
            return ".png";
        }
        if (mime.contains("jpeg") || mime.contains("jpg")) {
            return ".jpg";
        }
        if (mime.contains("webp")) {
            return ".webp";
        }
        return ".png";
    }
}
