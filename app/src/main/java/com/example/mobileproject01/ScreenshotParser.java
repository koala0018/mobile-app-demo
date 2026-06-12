package com.example.mobileproject01;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ScreenshotParser {
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:\\+?86[- ]?)?(1[3-9]\\d{9}|(?:0\\d{2,3}[- ]?)?\\d{7,8})");

    private static final String[] ADDRESS_HINTS = {
            "地址", "位置", "定位", "附近", "路", "街", "巷", "号", "楼", "层", "广场",
            "大厦", "商场", "中心", "酒店", "餐厅", "店", "门店", "店铺"
    };

    private static final String[] TITLE_HINTS = {
            "地址", "电话", "热线", "营业", "时间", "评论", "收藏", "分享", "导航", "价格"
    };

    private ScreenshotParser() {
    }

    static ParsedScreenshot parse(String rawText) {
        List<String> lines = normalizeLines(rawText);
        String phone = extractPhone(rawText, lines);
        String address = extractAddress(lines);
        String title = extractTitle(lines, address, phone);
        if (TextUtils.isEmpty(title)) {
            title = "未命名截图";
        }
        return new ParsedScreenshot(title, address, phone, "", rawText == null ? "" : rawText.trim());
    }

    private static List<String> normalizeLines(String rawText) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        if (rawText == null) {
            return new ArrayList<>();
        }
        String[] rawLines = rawText.split("\\R");
        for (String line : rawLines) {
            String clean = cleanLine(line);
            if (!TextUtils.isEmpty(clean)) {
                unique.add(clean);
            }
        }
        return new ArrayList<>(unique);
    }

    private static String cleanLine(String line) {
        if (line == null) {
            return "";
        }
        return line.replaceAll("\\s+", " ").trim();
    }

    private static String extractPhone(String rawText, List<String> lines) {
        if (!TextUtils.isEmpty(rawText)) {
            Matcher matcher = PHONE_PATTERN.matcher(rawText);
            if (matcher.find()) {
                return matcher.group();
            }
        }
        for (String line : lines) {
            if (containsAny(line, "电话", "热线", "联系", "客服")) {
                Matcher matcher = PHONE_PATTERN.matcher(line);
                if (matcher.find()) {
                    return matcher.group();
                }
            }
        }
        return "";
    }

    private static String extractAddress(List<String> lines) {
        String candidate = "";
        for (String line : lines) {
            if (containsAny(line, ADDRESS_HINTS)) {
                String cleaned = stripPrefix(line, "地址", "位置", "定位", "附近");
                if (cleaned.length() > candidate.length()) {
                    candidate = cleaned;
                }
            }
        }
        if (!TextUtils.isEmpty(candidate)) {
            return candidate;
        }
        for (String line : lines) {
            if (looksLikeAddress(line)) {
                return line;
            }
        }
        return "";
    }

    private static String extractTitle(List<String> lines, String address, String phone) {
        for (String line : lines) {
            if (isCandidateTitle(line, address, phone)) {
                return line;
            }
        }
        return "";
    }

    private static boolean isCandidateTitle(String line, String address, String phone) {
        if (TextUtils.isEmpty(line)) {
            return false;
        }
        String lower = line.toLowerCase(Locale.ROOT);
        if (containsAny(line, TITLE_HINTS)) {
            return false;
        }
        if (line.equals(address) || line.equals(phone)) {
            return false;
        }
        if (line.length() < 2 || line.length() > 24) {
            return false;
        }
        return !lower.matches("[0-9\\s\\-]+");
    }

    private static boolean looksLikeAddress(String line) {
        return containsAny(line, ADDRESS_HINTS);
    }

    private static String stripPrefix(String line, String... prefixes) {
        if (TextUtils.isEmpty(line)) {
            return "";
        }
        String result = line.trim();
        for (String prefix : prefixes) {
            result = result.replaceFirst("^" + Pattern.quote(prefix) + "[:：\\s]*", "");
        }
        return result.trim();
    }

    private static boolean containsAny(String line, String... keys) {
        if (TextUtils.isEmpty(line)) {
            return false;
        }
        for (String key : keys) {
            if (line.contains(key)) {
                return true;
            }
        }
        return false;
    }
}
