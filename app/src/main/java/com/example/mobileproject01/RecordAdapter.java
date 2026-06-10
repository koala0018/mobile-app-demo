package com.example.mobileproject01;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.RecordViewHolder> {
    public interface OnRecordClickListener {
        void onRecordClicked(ScreenshotRecord record);
    }

    private final Context context;
    private final OnRecordClickListener listener;
    private final List<ScreenshotRecord> items = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());

    public RecordAdapter(Context context, List<ScreenshotRecord> initialItems, OnRecordClickListener listener) {
        this.context = context;
        this.listener = listener;
        items.addAll(initialItems);
    }

    public void setItems(List<ScreenshotRecord> records) {
        items.clear();
        items.addAll(records);
        notifyDataSetChanged();
    }

    @Override
    public RecordViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackground(Styles.cardBackground(0xFF13243E, 0xFF1B3355));
        card.setElevation(dp(6));
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(12);
        card.setLayoutParams(params);

        ImageView thumb = new ImageView(context);
        LinearLayout.LayoutParams thumbParams = new LinearLayout.LayoutParams(dp(86), dp(86));
        thumbParams.rightMargin = dp(12);
        thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
        thumb.setBackground(Styles.previewBackground());
        card.addView(thumb, thumbParams);

        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.TOP);
        column.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = Styles.itemTitle(context, "标题");
        TextView meta = Styles.miniText(context, "");
        meta.setPadding(0, dp(4), 0, 0);
        TextView address = Styles.itemBody(context, "");
        address.setPadding(0, dp(8), 0, 0);
        TextView phone = Styles.itemBody(context, "");
        phone.setPadding(0, dp(4), 0, 0);
        TextView raw = Styles.itemSnippet(context, "");
        raw.setPadding(0, dp(8), 0, 0);

        column.addView(title);
        column.addView(meta);
        column.addView(address);
        column.addView(phone);
        column.addView(raw);
        card.addView(column);

        return new RecordViewHolder(card, thumb, title, meta, address, phone, raw);
    }

    @Override
    public void onBindViewHolder(RecordViewHolder holder, int position) {
        ScreenshotRecord record = items.get(position);
        holder.title.setText(record.title);
        holder.meta.setText(dateFormat.format(new Date(record.createdAt)) + " · " + record.sourceLabel);
        holder.address.setText("地址：" + safe(record.address));
        holder.phone.setText("电话：" + safe(record.phone));
        holder.raw.setText(safeSnippet(record.rawText));

        Bitmap bitmap = BitmapFactory.decodeFile(record.thumbnailPath);
        if (bitmap != null) {
            holder.thumb.setImageBitmap(bitmap);
        } else {
            holder.thumb.setImageDrawable(null);
        }

        holder.itemView.setOnClickListener(v -> listener.onRecordClicked(record));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "未识别" : value.trim();
    }

    private String safeSnippet(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "暂无识别文本";
        }
        String clean = value.trim().replace('\n', ' ');
        return clean.length() <= 84 ? clean : clean.substring(0, 83) + "…";
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static final class RecordViewHolder extends RecyclerView.ViewHolder {
        final ImageView thumb;
        final TextView title;
        final TextView meta;
        final TextView address;
        final TextView phone;
        final TextView raw;

        RecordViewHolder(View itemView, ImageView thumb, TextView title, TextView meta, TextView address, TextView phone, TextView raw) {
            super(itemView);
            this.thumb = thumb;
            this.title = title;
            this.meta = meta;
            this.address = address;
            this.phone = phone;
            this.raw = raw;
        }
    }
}
