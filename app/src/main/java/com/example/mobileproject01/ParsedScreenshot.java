package com.example.mobileproject01;

final class ParsedScreenshot {
    final String title;
    final String address;
    final String phone;
    final String rawText;

    ParsedScreenshot(String title, String address, String phone, String rawText) {
        this.title = title;
        this.address = address;
        this.phone = phone;
        this.rawText = rawText;
    }
}
