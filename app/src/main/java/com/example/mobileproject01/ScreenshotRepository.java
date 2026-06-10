package com.example.mobileproject01;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.TextUtils;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ScreenshotRepository {
    private final Context context;
    private final RecordStore store;

    ScreenshotRepository(Context context) {
        this.context = context.getApplicationContext();
        this.store = new RecordStore(this.context);
    }

    List<ScreenshotRecord> loadRecords() {
        return store.loadAll();
    }

    ScreenshotRecord importFromUri(Uri uri, String sourceLabel) throws Exception {
        String rawText = recognizeText(uri);
        ParsedScreenshot parsed = ScreenshotParser.parse(rawText);
        long createdAt = System.currentTimeMillis();

        File recordsDir = new File(context.getFilesDir(), "screenshots");
        if (!recordsDir.exists() && !recordsDir.mkdirs()) {
            throw new IOException("无法创建截图目录");
        }

        String baseName = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(createdAt);
        String extension = guessExtension(uri);
        File originalFile = new File(recordsDir, "shot_" + baseName + extension);
        copyUriToFile(uri, originalFile);

        File thumbFile = new File(recordsDir, "thumb_" + baseName + ".jpg");
        createThumbnail(originalFile, thumbFile);

        ScreenshotRecord record = new ScreenshotRecord(
                0L,
                parsed.title,
                parsed.address,
                parsed.phone,
                parsed.rawText,
                originalFile.getAbsolutePath(),
                thumbFile.getAbsolutePath(),
                sourceLabel,
                createdAt);

        long id = store.insert(record);
        return new ScreenshotRecord(
                id,
                record.title,
                record.address,
                record.phone,
                record.rawText,
                record.imagePath,
                record.thumbnailPath,
                record.sourceLabel,
                record.createdAt);
    }

    private String recognizeText(Uri uri) throws Exception {
        InputImage image = InputImage.fromFilePath(context, uri);
        StringBuilder builder = new StringBuilder();
        appendRecognizedText(builder, image, new ChineseTextRecognizerOptions.Builder().build());
        appendRecognizedText(builder, image, TextRecognizerOptions.DEFAULT_OPTIONS);
        String text = builder.toString().trim();
        if (TextUtils.isEmpty(text)) {
            return "未识别到文本";
        }
        return text;
    }

    private void appendRecognizedText(StringBuilder builder, InputImage image, Object options) throws Exception {
        Text text;
        if (options instanceof ChineseTextRecognizerOptions) {
            text = Tasks.await(TextRecognition.getClient((ChineseTextRecognizerOptions) options).process(image));
        } else {
            text = Tasks.await(TextRecognition.getClient((TextRecognizerOptions) options).process(image));
        }
        if (text == null) {
            return;
        }
        String recognized = text.getText();
        if (!TextUtils.isEmpty(recognized)) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(recognized);
        }
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
