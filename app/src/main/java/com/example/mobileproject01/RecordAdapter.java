package com.example.mobileproject01;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
    public interface OnRecordActionListener {
        void onRecordClicked(ScreenshotRecord record);

        void onRecordEditRequested(ScreenshotRecord record);

        void onRecordDeleteRequested(ScreenshotRecord record);

        void onRecordMapRequested(ScreenshotRecord record);
    }

    private final Context context;
    private final OnRecordActionListener listener;
    private final List<ScreenshotRecord> items = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());

    public RecordAdapter(Context context, List<ScreenshotRecord> initialItems, OnRecordActionListener listener) {
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
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(10), dp(10), dp(10), dp(10));
        card.setBackground(Styles.cardBackground(0xFFFFFFFF, 0xFFFFFFFF));
        card.setElevation(dp(3));
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(10);
        card.setLayoutParams(params);
        Styles.applyClickableSurface(card);

        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.TOP);

        ImageView thumb = new ImageView(context);
        LinearLayout.LayoutParams thumbParams = new LinearLayout.LayoutParams(dp(68), dp(68));
        thumbParams.rightMargin = dp(10);
        thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
        thumb.setBackground(Styles.previewBackground());
        topRow.addView(thumb, thumbParams);

        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = Styles.itemTitle(context, "标题");
        TextView meta = Styles.miniText(context, "");
        meta.setPadding(0, dp(2), 0, 0);
        TextView address = Styles.itemBody(context, "");
        address.setMaxLines(2);
        address.setPadding(0, dp(7), 0, 0);
        TextView phone = Styles.itemSnippet(context, "");
        phone.setPadding(0, dp(3), 0, 0);

        column.addView(title);
        column.addView(meta);
        column.addView(address);
        column.addView(phone);
        topRow.addView(column);
        card.addView(topRow);

        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(8), 0, 0);

        Button editButton = Styles.secondaryButton(context, "编辑");
        Button mapButton = Styles.accentButton(context, "地图", Styles.YELLOW);
        Button deleteButton = Styles.dangerButton(context, "删除");
        editButton.setTextSize(11.5f);
        mapButton.setTextSize(11.5f);
        deleteButton.setTextSize(11.5f);

        LinearLayout.LayoutParams actionLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        actionLp.rightMargin = dp(6);
        actions.addView(editButton, actionLp);
        LinearLayout.LayoutParams actionLp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        actionLp2.rightMargin = dp(6);
        actions.addView(mapButton, actionLp2);
        LinearLayout.LayoutParams actionLp3 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        actions.addView(deleteButton, actionLp3);
        card.addView(actions);

        return new RecordViewHolder(card, thumb, title, meta, address, phone, editButton, mapButton, deleteButton);
    }

    @Override
    public void onBindViewHolder(RecordViewHolder holder, int position) {
        ScreenshotRecord record = items.get(position);
        holder.title.setText(blank(record.title) ? "未命名截图" : record.title);
        holder.meta.setText(dateFormat.format(new Date(record.createdAt)) + " / " + safe(record.sourceLabel));
        holder.address.setText(blank(record.address) ? "地址未识别" : record.address);
        holder.phone.setText(blank(record.phone) ? "电话未识别" : record.phone);

        Bitmap bitmap = BitmapFactory.decodeFile(record.thumbnailPath);
        holder.thumb.setImageBitmap(bitmap);

        boolean hasAddress = !blank(record.address);
        holder.mapButton.setEnabled(hasAddress);
        holder.mapButton.setAlpha(hasAddress ? 1f : 0.45f);

        holder.itemView.setOnClickListener(v -> listener.onRecordClicked(record));
        holder.editButton.setOnClickListener(v -> listener.onRecordEditRequested(record));
        holder.deleteButton.setOnClickListener(v -> listener.onRecordDeleteRequested(record));
        holder.mapButton.setOnClickListener(v -> listener.onRecordMapRequested(record));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String safe(String value) {
        return blank(value) ? "本地导入" : value.trim();
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
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
        final Button editButton;
        final Button mapButton;
        final Button deleteButton;

        RecordViewHolder(View itemView,
                         ImageView thumb,
                         TextView title,
                         TextView meta,
                         TextView address,
                         TextView phone,
                         Button editButton,
                         Button mapButton,
                         Button deleteButton) {
            super(itemView);
            this.thumb = thumb;
            this.title = title;
            this.meta = meta;
            this.address = address;
            this.phone = phone;
            this.editButton = editButton;
            this.mapButton = mapButton;
            this.deleteButton = deleteButton;
        }
    }
}
