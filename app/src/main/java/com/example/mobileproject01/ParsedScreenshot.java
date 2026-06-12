package com.example.mobileproject01;

final class ParsedScreenshot {
    final String title;
    final String address;
    final String phone;
    final String notes;
    final String rawText;

    ParsedScreenshot(String title, String address, String phone, String notes, String rawText) {
        this.title = title;
        this.address = address;
        this.phone = phone;
        this.notes = notes;
        this.rawText = rawText;
    }
}
