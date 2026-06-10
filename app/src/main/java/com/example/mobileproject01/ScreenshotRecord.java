package com.example.mobileproject01;

public final class ScreenshotRecord {
    public final long id;
    public final String title;
    public final String address;
    public final String phone;
    public final String rawText;
    public final String notes;
    public final String imagePath;
    public final String thumbnailPath;
    public final String sourceLabel;
    public final long createdAt;

    public ScreenshotRecord(long id,
                            String title,
                            String address,
                            String phone,
                            String rawText,
                            String notes,
                            String imagePath,
                            String thumbnailPath,
                            String sourceLabel,
                            long createdAt) {
        this.id = id;
        this.title = title;
        this.address = address;
        this.phone = phone;
        this.rawText = rawText;
        this.notes = notes;
        this.imagePath = imagePath;
        this.thumbnailPath = thumbnailPath;
        this.sourceLabel = sourceLabel;
        this.createdAt = createdAt;
    }

    public ScreenshotRecord withId(long newId) {
        return new ScreenshotRecord(
                newId,
                title,
                address,
                phone,
                rawText,
                notes,
                imagePath,
                thumbnailPath,
                sourceLabel,
                createdAt);
    }
}
