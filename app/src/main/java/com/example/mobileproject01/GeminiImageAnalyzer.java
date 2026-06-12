package com.example.mobileproject01;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

final class GeminiImageAnalyzer {
    private static final String MODEL = "gemini-2.5-flash-lite";
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent";

    ParsedScreenshot analyze(File imageFile, String apiKey) throws IOException, JSONException {
        if (TextUtils.isEmpty(apiKey)) {
            throw new IOException("Missing Gemini API key");
        }

        byte[] imageBytes = toJpegBytes(imageFile);
        String encoded = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
        JSONObject payload = buildPayload(encoded);
        String response = postJson(apiKey, payload);
        return parseResponse(response);
    }

    private JSONObject buildPayload(String base64Image) throws JSONException {
        JSONObject inlineData = new JSONObject();
        inlineData.put("mime_type", "image/jpeg");
        inlineData.put("data", base64Image);

        JSONObject imagePart = new JSONObject();
        imagePart.put("inline_data", inlineData);

        JSONObject textPart = new JSONObject();
        textPart.put("text",
                "你是一个细心的信息提取助手。请只根据图片内容提取中文截图里的店名、地址、电话和可补充备注，" +
                        "并输出严格 JSON，不要输出多余说明。JSON 字段必须包含 title、address、phone、notes、rawText。 " +
                        "title 写最像店名或地点名称的短标题；address 写完整地址；phone 写联系电话；notes 写与门店相关的补充信息；" +
                        "rawText 写你能看到的关键信息摘要。若不存在则留空字符串。");

        JSONObject content = new JSONObject();
        content.put("role", "user");
        content.put("parts", new org.json.JSONArray().put(textPart).put(imagePart));

        JSONObject generationConfig = new JSONObject();
        generationConfig.put("temperature", 0.2);
        generationConfig.put("responseMimeType", "application/json");

        JSONObject root = new JSONObject();
        root.put("contents", new org.json.JSONArray().put(content));
        root.put("generationConfig", generationConfig);
        return root;
    }

    private String postJson(String apiKey, JSONObject payload) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(ENDPOINT + "?key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8.name()));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            byte[] bytes = payload.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(bytes);
            }

            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
            String body = readAll(stream);
            if (code < 200 || code >= 300) {
                throw new IOException("Gemini HTTP " + code + ": " + body);
            }
            return body;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private ParsedScreenshot parseResponse(String body) throws JSONException {
        JSONObject root = new JSONObject(body);
        String text = "";
        if (root.has("candidates")) {
            org.json.JSONArray candidates = root.getJSONArray("candidates");
            if (candidates.length() > 0) {
                JSONObject candidate = candidates.getJSONObject(0);
                if (candidate.has("content")) {
                    JSONObject content = candidate.getJSONObject("content");
                    org.json.JSONArray parts = content.optJSONArray("parts");
                    if (parts != null && parts.length() > 0) {
                        text = parts.getJSONObject(0).optString("text", "");
                    }
                }
            }
        }
        if (TextUtils.isEmpty(text) && root.has("text")) {
            text = root.optString("text", "");
        }

        JSONObject result = safeJson(text);
        String title = clean(result.optString("title", ""));
        String address = clean(result.optString("address", ""));
        String phone = clean(result.optString("phone", ""));
        String notes = clean(result.optString("notes", ""));
        String rawText = clean(result.optString("rawText", text));

        if (TextUtils.isEmpty(title)) {
            title = "未命名截图";
        }
        return new ParsedScreenshot(title, address, phone, notes, rawText);
    }

    private JSONObject safeJson(String text) throws JSONException {
        if (TextUtils.isEmpty(text)) {
            return new JSONObject();
        }
        String clean = text.trim();
        int start = clean.indexOf('{');
        int end = clean.lastIndexOf('}');
        if (start >= 0 && end > start) {
            clean = clean.substring(start, end + 1);
        }
        return new JSONObject(clean);
    }

    private byte[] toJpegBytes(File imageFile) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
        options.inSampleSize = calculateSampleSize(options, 1600, 1600);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
        if (bitmap == null) {
            return readBytes(imageFile);
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output);
            return output.toByteArray();
        } finally {
            bitmap.recycle();
        }
    }

    private int calculateSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int sampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / sampleSize) >= reqHeight && (halfWidth / sampleSize) >= reqWidth) {
                sampleSize *= 2;
            }
        }
        return Math.max(1, sampleSize);
    }

    private byte[] readBytes(File file) throws IOException {
        try (InputStream input = new java.io.FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private String readAll(InputStream input) throws IOException {
        if (input == null) {
            return "";
        }
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
