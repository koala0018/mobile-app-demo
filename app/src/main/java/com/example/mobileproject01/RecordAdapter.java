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
        card.setBackground(Styles.cardBackground(0xFF13243E, 0xFF1B3355));
        card.setElevation(dp(5));
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(8);
        card.setLayoutParams(params);
        Styles.applyClickableSurface(card);

        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.TOP);

        ImageView thumb = new ImageView(context);
        LinearLayout.LayoutParams thumbParams = new LinearLayout.LayoutParams(dp(64), dp(64));
        thumbParams.rightMargin = dp(10);
        thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
        thumb.setBackground(Styles.previewBackground());
        topRow.addView(thumb, thumbParams);

        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = Styles.itemTitle(context, "标题");
        title.setTextSize(15.5f);
        TextView meta = Styles.miniText(context, "");
        meta.setPadding(0, dp(2), 0, 0);

        LinearLayout badgeRow = new LinearLayout(context);
        badgeRow.setOrientation(LinearLayout.HORIZONTAL);
        badgeRow.setPadding(0, dp(6), 0, 0);

        TextView address = Styles.badge(context, "地址");
        address.setTextSize(10.5f);
        address.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView phone = Styles.badge(context, "电话");
        phone.setTextSize(10.5f);
        LinearLayout.LayoutParams phoneLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        phoneLp.leftMargin = dp(6);
        TextView source = Styles.badge(context, "来源");
        source.setTextSize(10.5f);
        LinearLayout.LayoutParams sourceLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        sourceLp.leftMargin = dp(6);
        badgeRow.addView(address);
        badgeRow.addView(phone, phoneLp);
        badgeRow.addView(source, sourceLp);

        column.addView(title);
        column.addView(meta);
        column.addView(badgeRow);

        topRow.addView(column);
        card.addView(topRow);

        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(8), 0, 0);

        Button editButton = Styles.secondaryButton(context, "编辑");
        Button mapButton = Styles.secondaryButton(context, "地图");
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

        return new RecordViewHolder(card, thumb, title, meta, address, phone, source, editButton, mapButton, deleteButton);
    }

    @Override
    public void onBindViewHolder(RecordViewHolder holder, int position) {
        ScreenshotRecord record = items.get(position);
        holder.title.setText(record.title);
        holder.meta.setText(dateFormat.format(new Date(record.createdAt)) + " · " + safe(record.sourceLabel));
        holder.address.setText("地址：" + compactSafe(record.address));
        holder.phone.setText("电话：" + compactSafe(record.phone));
        holder.source.setText("来源：" + compactSafe(record.sourceLabel));

        Bitmap bitmap = BitmapFactory.decodeFile(record.thumbnailPath);
        if (bitmap != null) {
            holder.thumb.setImageBitmap(bitmap);
        } else {
            holder.thumb.setImageDrawable(null);
        }

        boolean hasAddress = !isBlank(record.address);
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

    private String compactSafe(String value) {
        if (isBlank(value)) {
            return "未填写";
        }
        return value.trim();
    }

    private String safe(String value) {
        return isBlank(value) ? "未识别" : value.trim();
    }

    private String safeSnippet(String value) {
        if (isBlank(value)) {
            return "暂无识别文本";
        }
        String clean = value.trim().replace('\n', ' ');
        return clean.length() <= 96 ? clean : clean.substring(0, 95) + "…";
    }

    private boolean isBlank(String value) {
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
        final TextView source;
        final Button editButton;
        final Button mapButton;
        final Button deleteButton;

        RecordViewHolder(View itemView,
                         ImageView thumb,
                         TextView title,
                         TextView meta,
                         TextView address,
                         TextView phone,
                         TextView source,
                         Button editButton,
                         Button mapButton,
                         Button deleteButton) {
            super(itemView);
            this.thumb = thumb;
            this.title = title;
            this.meta = meta;
            this.address = address;
            this.phone = phone;
            this.source = source;
            this.editButton = editButton;
            this.mapButton = mapButton;
            this.deleteButton = deleteButton;
        }
    }
}
