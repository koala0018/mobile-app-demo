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
    private static final String MODEL = "gemini-2.0-flash-lite";
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent";
    private static final String SILICONFLOW_ENDPOINT = "https://api.siliconflow.cn/v1/chat/completions";
    static final String DEFAULT_SILICONFLOW_MODEL = "Qwen/Qwen3-VL-32B-Instruct";

    ParsedScreenshot analyze(File imageFile, String apiKey) throws IOException, JSONException {
        if (TextUtils.isEmpty(apiKey)) {
            throw new IOException("请先填写 Google Gemini API Key");
        }

        String encoded = Base64.encodeToString(toJpegBytes(imageFile), Base64.NO_WRAP);
        String response = postJson(apiKey, buildPayload(encoded));
        return parseResponse(response);
    }

    ParsedScreenshot analyzeSiliconFlow(File imageFile, String apiKey, String model) throws IOException, JSONException {
        if (TextUtils.isEmpty(apiKey)) {
            throw new IOException("请先填写硅基流动 API Key");
        }
        if (TextUtils.isEmpty(model)) {
            model = DEFAULT_SILICONFLOW_MODEL;
        }
        String encoded = Base64.encodeToString(toJpegBytes(imageFile), Base64.NO_WRAP);
        String response = postOpenAiCompatible(apiKey, buildOpenAiPayload(encoded, model));
        return parseOpenAiResponse(response);
    }

    private JSONObject buildPayload(String base64Image) throws JSONException {
        JSONObject inlineData = new JSONObject();
        inlineData.put("mime_type", "image/jpeg");
        inlineData.put("data", base64Image);

        JSONObject imagePart = new JSONObject();
        imagePart.put("inline_data", inlineData);

        JSONObject textPart = new JSONObject();
        textPart.put("text",
                "你是一个中文截图信息提取助手。请只根据图片内容提取店名、地点名、地址、电话和补充说明。"
                        + "只输出严格 JSON，不要 Markdown，不要解释。"
                        + "JSON 字段必须是 title、address、phone、notes、rawText。"
                        + "title 写最像店名或地点名的短标题；address 写完整地址；phone 写联系电话；"
                        + "notes 写营业时间、楼层、门店说明等补充信息；rawText 写你从图片中看到的关键文字摘要。"
                        + "如果某字段不存在，请返回空字符串。");

        JSONObject content = new JSONObject();
        content.put("role", "user");
        content.put("parts", new org.json.JSONArray().put(textPart).put(imagePart));

        JSONObject generationConfig = new JSONObject();
        generationConfig.put("temperature", 0.1);
        generationConfig.put("candidateCount", 1);
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
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(35000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Connection", "close");

            byte[] bytes = payload.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
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

    private JSONObject buildOpenAiPayload(String base64Image, String model) throws JSONException {
        JSONObject textPart = new JSONObject();
        textPart.put("type", "text");
        textPart.put("text",
                "你是一个中文截图信息提取助手。请只根据图片内容提取店名、地点名、地址、电话和补充说明。"
                        + "只输出严格 JSON，不要 Markdown，不要解释。"
                        + "JSON 字段必须是 title、address、phone、notes、rawText。"
                        + "如果字段不存在，请返回空字符串。");

        JSONObject imageUrl = new JSONObject();
        imageUrl.put("url", "data:image/jpeg;base64," + base64Image);
        JSONObject imagePart = new JSONObject();
        imagePart.put("type", "image_url");
        imagePart.put("image_url", imageUrl);

        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", new org.json.JSONArray().put(textPart).put(imagePart));

        JSONObject root = new JSONObject();
        root.put("model", model);
        root.put("messages", new org.json.JSONArray().put(message));
        root.put("temperature", 0.1);
        root.put("max_tokens", 700);
        root.put("response_format", new JSONObject().put("type", "json_object"));
        return root;
    }

    private String postOpenAiCompatible(String apiKey, JSONObject payload) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(SILICONFLOW_ENDPOINT);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(35000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Connection", "close");

            byte[] bytes = payload.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(bytes);
            }

            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
            String body = readAll(stream);
            if (code < 200 || code >= 300) {
                if (code == 403 && body.toLowerCase(java.util.Locale.ROOT).contains("model")) {
                    throw new IOException("当前硅基流动账号不可调用该模型，请在 Key 设置里换成控制台可用的视觉模型 ID。原始错误：" + body);
                }
                throw new IOException("硅基流动 HTTP " + code + ": " + body);
            }
            return body;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private ParsedScreenshot parseOpenAiResponse(String body) throws JSONException {
        JSONObject root = new JSONObject(body);
        String text = "";
        org.json.JSONArray choices = root.optJSONArray("choices");
        if (choices != null && choices.length() > 0) {
            JSONObject message = choices.getJSONObject(0).optJSONObject("message");
            if (message != null) {
                text = message.optString("content", "");
            }
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

    private ParsedScreenshot parseResponse(String body) throws JSONException {
        JSONObject root = new JSONObject(body);
        String text = "";
        org.json.JSONArray candidates = root.optJSONArray("candidates");
        if (candidates != null && candidates.length() > 0) {
            JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
            if (content != null) {
                org.json.JSONArray parts = content.optJSONArray("parts");
                if (parts != null && parts.length() > 0) {
                    text = parts.getJSONObject(0).optString("text", "");
                }
            }
        }
        if (TextUtils.isEmpty(text)) {
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
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output);
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
