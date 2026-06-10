package com.example.mobileproject01;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity implements RecordAdapter.OnRecordClickListener {
    private static final int REQUEST_PICK_IMAGE = 1001;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ScreenshotRepository repository;
    private RecordAdapter adapter;
    private RecyclerView recyclerView;
    private StatChipView countView;
    private StatChipView latestView;
    private TextView statusView;
    private View emptyStateView;
    private boolean importing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new ScreenshotRepository(this);
        buildUi();
        refreshRecords();
        handleIncomingIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                startImport(uri, "手动选择");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    @Override
    public void onRecordClicked(ScreenshotRecord record) {
        StringBuilder message = new StringBuilder();
        message.append("店名：").append(safe(record.title)).append('\n');
        message.append("地址：").append(safe(record.address)).append('\n');
        message.append("电话：").append(safe(record.phone)).append('\n');
        message.append("来源：").append(safe(record.sourceLabel)).append('\n');
        message.append("时间：").append(formatDate(record.createdAt)).append('\n');
        message.append("\n识别文本：\n").append(safe(record.rawText));

        new AlertDialog.Builder(this)
                .setTitle(record.title)
                .setMessage(message.toString())
                .setPositiveButton("知道了", null)
                .show();
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF07111F);

        ParticleFieldView background = new ParticleFieldView(this);
        root.addView(background, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(18), dp(18), dp(20));
        root.addView(content, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout scrollChild = new LinearLayout(this);
        scrollChild.setOrientation(LinearLayout.VERTICAL);
        scrollChild.setPadding(0, 0, 0, dp(12));
        scroll.addView(scrollChild, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        content.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f));

        LinearLayout heroCard = new LinearLayout(this);
        heroCard.setOrientation(LinearLayout.VERTICAL);
        heroCard.setPadding(dp(18), dp(18), dp(18), dp(18));
        heroCard.setBackground(Styles.cardBackground(0xFF12243D, 0xFF1A3354));
        heroCard.setElevation(dp(8));

        TextView title = Styles.title(this, getString(R.string.app_name));
        TextView subtitle = Styles.body(this, "截图一下，自动识别店名、地址、电话，并存到本机。");
        subtitle.setPadding(0, dp(8), 0, dp(16));

        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setWeightSum(2f);

        countView = Styles.statChip(this, "0 条记录", "总收纳");
        latestView = Styles.statChip(this, "暂无记录", "最新一条");

        LinearLayout.LayoutParams statLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        statLp.rightMargin = dp(8);
        statsRow.addView(countView, statLp);
        LinearLayout.LayoutParams statLp2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        statsRow.addView(latestView, statLp2);

        heroCard.addView(title);
        heroCard.addView(subtitle);
        heroCard.addView(statsRow);
        scrollChild.addView(heroCard);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(14), 0, dp(10));

        Button importButton = Styles.primaryButton(this, "导入截图");
        importButton.setOnClickListener(v -> openImagePicker());

        Button refreshButton = Styles.secondaryButton(this, "刷新列表");
        refreshButton.setOnClickListener(v -> refreshRecords());

        LinearLayout.LayoutParams buttonLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        buttonLp.rightMargin = dp(8);
        actions.addView(importButton, buttonLp);

        LinearLayout.LayoutParams buttonLp2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        actions.addView(refreshButton, buttonLp2);
        scrollChild.addView(actions);

        statusView = Styles.body(this, "准备好了，分享一张截图进来就会自动识别。");
        statusView.setPadding(dp(4), dp(4), dp(4), dp(8));
        scrollChild.addView(statusView);

        LinearLayout listHeader = new LinearLayout(this);
        listHeader.setOrientation(LinearLayout.HORIZONTAL);
        listHeader.setGravity(Gravity.CENTER_VERTICAL);
        listHeader.setPadding(0, dp(8), 0, dp(10));

        TextView listTitle = Styles.sectionTitle(this, "收藏记录");
        TextView listHint = Styles.miniText(this, "点击卡片查看完整识别内容");
        listHint.setGravity(Gravity.END);

        LinearLayout.LayoutParams listTitleLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        listHeader.addView(listTitle, listTitleLp);
        listHeader.addView(listHint, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        scrollChild.addView(listHeader);

        FrameLayout listContainer = new FrameLayout(this);
        listContainer.setBackground(Styles.cardBackground(0xFF0F1F35, 0xFF152842));
        listContainer.setElevation(dp(8));
        listContainer.setPadding(dp(8), dp(8), dp(8), dp(8));
        scrollChild.addView(listContainer, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(520)));

        recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecordAdapter(this, new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);
        listContainer.addView(recyclerView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        emptyStateView = buildEmptyState();
        listContainer.addView(emptyStateView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        setContentView(root);
    }

    private View buildEmptyState() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(24), dp(24), dp(24), dp(24));

        TextView emoji = Styles.title(this, "✨");
        emoji.setTextSize(34f);

        TextView headline = Styles.sectionTitle(this, "把截图收进本机记忆盒");
        headline.setPadding(0, dp(8), 0, dp(4));

        TextView copy = Styles.body(this, "从任意 App 分享截图到这里，系统会自动 OCR 并保存店名、地址、电话、原图和时间。");
        copy.setGravity(Gravity.CENTER_HORIZONTAL);
        copy.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        copy.setPadding(dp(8), 0, dp(8), 0);

        box.addView(emoji);
        box.addView(headline);
        box.addView(copy);
        return box;
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        if (Intent.ACTION_SEND.equals(action)) {
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri != null) {
                startImport(uri, "系统分享");
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "选择截图"), REQUEST_PICK_IMAGE);
    }

    private void refreshRecords() {
        executor.execute(() -> {
            List<ScreenshotRecord> records = repository.loadRecords();
            runOnUiThread(() -> applyRecords(records));
        });
    }

    private void applyRecords(List<ScreenshotRecord> records) {
        adapter.setItems(records);
        updateStats(records);
        boolean empty = records.isEmpty();
        emptyStateView.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void updateStats(List<ScreenshotRecord> records) {
        countView.setValue(records.size() + " 条记录");
        if (records.isEmpty()) {
            latestView.setValue("暂无记录");
            statusView.setText("准备好了，分享一张截图进来就会自动识别。");
            return;
        }
        ScreenshotRecord first = records.get(0);
        latestView.setValue(shorten(first.title, 12));
        statusView.setText("最新记录：" + first.title + " · " + formatDate(first.createdAt));
    }

    private void startImport(Uri uri, String sourceLabel) {
        if (importing) {
            Toast.makeText(this, "正在处理上一张截图", Toast.LENGTH_SHORT).show();
            return;
        }
        importing = true;
        statusView.setText("正在识别截图内容，请稍等...");
        executor.execute(() -> {
            try {
                ScreenshotRecord record = repository.importFromUri(uri, sourceLabel);
                runOnUiThread(() -> {
                    importing = false;
                    Toast.makeText(this, "已保存：" + record.title, Toast.LENGTH_SHORT).show();
                    refreshRecords();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    importing = false;
                    statusView.setText("识别失败：" + e.getMessage());
                    Toast.makeText(this, "截图导入失败", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private String formatDate(long time) {
        return new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(new Date(time));
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "未识别" : value.trim();
    }

    private String shorten(String value, int max) {
        if (value == null) {
            return "";
        }
        String clean = value.trim();
        if (clean.length() <= max) {
            return clean;
        }
        return clean.substring(0, max - 1) + "…";
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
