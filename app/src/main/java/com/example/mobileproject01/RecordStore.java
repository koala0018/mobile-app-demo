package com.example.mobileproject01;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

final class RecordStore extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "snapnest.db";
    private static final int DATABASE_VERSION = 2;
    private static final String TABLE = "records";

    RecordStore(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "address TEXT," +
                "phone TEXT," +
                "raw_text TEXT," +
                "notes TEXT DEFAULT ''," +
                "image_path TEXT NOT NULL," +
                "thumbnail_path TEXT NOT NULL," +
                "source_label TEXT," +
                "created_at INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE + " ADD COLUMN notes TEXT DEFAULT ''");
        }
    }

    long insert(ScreenshotRecord record) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", record.title);
        values.put("address", record.address);
        values.put("phone", record.phone);
        values.put("raw_text", record.rawText);
        values.put("notes", record.notes);
        values.put("image_path", record.imagePath);
        values.put("thumbnail_path", record.thumbnailPath);
        values.put("source_label", record.sourceLabel);
        values.put("created_at", record.createdAt);
        return db.insert(TABLE, null, values);
    }

    int update(ScreenshotRecord record) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", record.title);
        values.put("address", record.address);
        values.put("phone", record.phone);
        values.put("raw_text", record.rawText);
        values.put("notes", record.notes);
        values.put("source_label", record.sourceLabel);
        return db.update(TABLE, values, "id=?", new String[]{String.valueOf(record.id)});
    }

    int delete(long id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE, "id=?", new String[]{String.valueOf(id)});
    }

    List<ScreenshotRecord> loadAll() {
        List<ScreenshotRecord> records = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(TABLE, null, null, null, null, null, "created_at DESC")) {
            int idIndex = cursor.getColumnIndexOrThrow("id");
            int titleIndex = cursor.getColumnIndexOrThrow("title");
            int addressIndex = cursor.getColumnIndexOrThrow("address");
            int phoneIndex = cursor.getColumnIndexOrThrow("phone");
            int rawIndex = cursor.getColumnIndexOrThrow("raw_text");
            int imageIndex = cursor.getColumnIndexOrThrow("image_path");
            int thumbIndex = cursor.getColumnIndexOrThrow("thumbnail_path");
            int sourceIndex = cursor.getColumnIndexOrThrow("source_label");
            int createdIndex = cursor.getColumnIndexOrThrow("created_at");

            while (cursor.moveToNext()) {
                records.add(new ScreenshotRecord(
                        cursor.getLong(idIndex),
                        cursor.getString(titleIndex),
                        cursor.getString(addressIndex),
                        cursor.getString(phoneIndex),
                        cursor.getString(rawIndex),
                        cursor.getString(cursor.getColumnIndexOrThrow("notes")),
                        cursor.getString(imageIndex),
                        cursor.getString(thumbIndex),
                        cursor.getString(sourceIndex),
                        cursor.getLong(createdIndex)));
            }
        }
        return records;
    }
}
