package com.example.mobileproject01;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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

public class MainActivity extends Activity implements RecordAdapter.OnRecordActionListener {
    private static final int REQUEST_PICK_IMAGE = 1001;
    private static final int FILTER_ALL = 0;
    private static final int FILTER_ADDRESS = 1;
    private static final int FILTER_PHONE = 2;
    private static final int FILTER_RECENT = 3;
    private static final int FILTER_NOTES = 4;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<ScreenshotRecord> allRecords = new ArrayList<>();
    private final List<ScreenshotRecord> visibleRecords = new ArrayList<>();

    private ScreenshotRepository repository;
    private RecordAdapter adapter;
    private RecyclerView recyclerView;
    private StatChipView countView;
    private StatChipView latestView;
    private TextView statusView;
    private View emptyStateView;
    private TextView emptyTitleView;
    private TextView emptyBodyView;
    private Button emptyActionButton;
    private EditText searchField;
    private Button allFilterButton;
    private Button addressFilterButton;
    private Button phoneFilterButton;
    private Button recentFilterButton;
    private Button notesFilterButton;
    private Button refreshButton;
    private Button modelButton;
    private ProgressBar refreshProgress;
    private String currentQuery = "";
    private int currentFilter = FILTER_ALL;
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
        showRecordDialog(record);
    }

    @Override
    public void onRecordEditRequested(ScreenshotRecord record) {
        showRecordDialog(record);
    }

    @Override
    public void onRecordDeleteRequested(ScreenshotRecord record) {
        confirmDelete(record);
    }

    @Override
    public void onRecordMapRequested(ScreenshotRecord record) {
        openMap(record.address);
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF07111F);

        ParticleFieldView background = new ParticleFieldView(this);
        root.addView(background, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        View glow = new View(this);
        GradientDrawable glowDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0x336EE7FF, 0x00000000});
        glow.setBackground(glowDrawable);
        root.addView(glow, new FrameLayout.LayoutParams(
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
        heroCard.setBackground(Styles.panelBackground());
        heroCard.setElevation(dp(8));

        TextView eyebrow = Styles.badge(this, "本地收藏 · OCR 归档");
        eyebrow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = Styles.title(this, getString(R.string.app_name));
        title.setPadding(0, dp(12), 0, 0);

        TextView subtitle = Styles.body(this, "把看到的店面、地址、电话和截图本身都收进来，之后还能搜索、编辑、删除并一键打开高德地图。识别优先走 Gemini 图像理解。");
        subtitle.setPadding(0, dp(10), 0, dp(16));

        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setWeightSum(2f);

        countView = Styles.statChip(this, "0 条记录", "全部");
        latestView = Styles.statChip(this, "暂无记录", "最近一条");

        LinearLayout.LayoutParams statLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        statLp.rightMargin = dp(8);
        statsRow.addView(countView, statLp);
        LinearLayout.LayoutParams statLp2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        statsRow.addView(latestView, statLp2);

        heroCard.addView(eyebrow);
        heroCard.addView(title);
        heroCard.addView(subtitle);
        heroCard.addView(statsRow);
        scrollChild.addView(heroCard);

        LinearLayout searchCard = new LinearLayout(this);
        searchCard.setOrientation(LinearLayout.VERTICAL);
        searchCard.setPadding(dp(12), dp(12), dp(12), dp(12));
        searchCard.setBackground(Styles.panelBackground());
        searchCard.setElevation(dp(6));
        LinearLayout.LayoutParams searchCardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        searchCardLp.topMargin = dp(14);
        scrollChild.addView(searchCard, searchCardLp);

        TextView searchLabel = Styles.sectionTitle(this, "搜索与筛选");
        searchLabel.setTextSize(16f);
        searchCard.addView(searchLabel);

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setPadding(0, dp(8), 0, 0);

        searchField = Styles.inputField(this, "搜索标题、地址、电话、备注", false);
        searchField.setInputType(InputType.TYPE_CLASS_TEXT);
        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                currentQuery = s == null ? "" : s.toString();
                applyFilters();
            }
        });

        Button clearButton = Styles.secondaryButton(this, "清空");
        clearButton.setTextSize(12f);
        clearButton.setOnClickListener(v -> searchField.setText(""));

        LinearLayout.LayoutParams searchFieldLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.72f);
        searchFieldLp.rightMargin = dp(8);
        searchRow.addView(searchField, searchFieldLp);
        searchRow.addView(clearButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        searchCard.addView(searchRow);

        LinearLayout filterRow = new LinearLayout(this);
        filterRow.setOrientation(LinearLayout.HORIZONTAL);
        filterRow.setPadding(0, dp(12), 0, 0);

        allFilterButton = Styles.chipButton(this, "全部");
        addressFilterButton = Styles.chipButton(this, "有地址");
        phoneFilterButton = Styles.chipButton(this, "有电话");
        recentFilterButton = Styles.chipButton(this, "近 7 天");
        notesFilterButton = Styles.chipButton(this, "有备注");

        allFilterButton.setOnClickListener(v -> setFilter(FILTER_ALL));
        addressFilterButton.setOnClickListener(v -> setFilter(FILTER_ADDRESS));
        phoneFilterButton.setOnClickListener(v -> setFilter(FILTER_PHONE));
        recentFilterButton.setOnClickListener(v -> setFilter(FILTER_RECENT));
        notesFilterButton.setOnClickListener(v -> setFilter(FILTER_NOTES));

        filterRow.addView(allFilterButton);
        LinearLayout.LayoutParams chipMargin1 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        chipMargin1.leftMargin = dp(8);
        filterRow.addView(addressFilterButton, chipMargin1);
        LinearLayout.LayoutParams chipMargin2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        chipMargin2.leftMargin = dp(8);
        filterRow.addView(phoneFilterButton, chipMargin2);
        LinearLayout.LayoutParams chipMargin3 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        chipMargin3.leftMargin = dp(8);
        filterRow.addView(recentFilterButton, chipMargin3);
        LinearLayout.LayoutParams chipMargin4 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        chipMargin4.leftMargin = dp(8);
        filterRow.addView(notesFilterButton, chipMargin4);
        searchCard.addView(filterRow);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(10), 0, 0);

        Button importButton = Styles.primaryButton(this, "导入截图");
        importButton.setTextSize(12.5f);
        importButton.setOnClickListener(v -> openImagePicker());

        refreshButton = Styles.secondaryButton(this, "刷新");
        refreshButton.setTextSize(12.5f);
        refreshButton.setOnClickListener(v -> refreshRecords());

        modelButton = Styles.secondaryButton(this, "模型");
        modelButton.setTextSize(12.5f);
        modelButton.setOnClickListener(v -> showModelSettings());

        LinearLayout.LayoutParams buttonLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        buttonLp.rightMargin = dp(8);
        actions.addView(importButton, buttonLp);
        LinearLayout.LayoutParams buttonLp2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        buttonLp2.rightMargin = dp(8);
        actions.addView(refreshButton, buttonLp2);
        LinearLayout.LayoutParams buttonLp3 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.75f);
        actions.addView(modelButton, buttonLp3);
        searchCard.addView(actions);

        LinearLayout refreshRow = new LinearLayout(this);
        refreshRow.setOrientation(LinearLayout.HORIZONTAL);
        refreshRow.setGravity(Gravity.CENTER_VERTICAL);
        refreshRow.setPadding(0, dp(8), 0, 0);
        refreshProgress = new ProgressBar(this);
        refreshProgress.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressLp = new LinearLayout.LayoutParams(dp(18), dp(18));
        progressLp.rightMargin = dp(8);
        refreshRow.addView(refreshProgress, progressLp);

        TextView refreshHint = Styles.miniText(this, "刷新时会显示加载状态，识别优先使用 Gemini 图像理解，缺少 key 时回退到本地识别。");
        refreshHint.setTextSize(11.5f);
        refreshRow.addView(refreshHint, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f));
        searchCard.addView(refreshRow);

        statusView = Styles.body(this, "准备好了，导入一张截图后就能自动识别并存档。");
        statusView.setPadding(dp(4), dp(8), dp(4), dp(8));
        scrollChild.addView(statusView);

        LinearLayout listHeader = new LinearLayout(this);
        listHeader.setOrientation(LinearLayout.HORIZONTAL);
        listHeader.setGravity(Gravity.CENTER_VERTICAL);
        listHeader.setPadding(0, dp(10), 0, dp(10));

        TextView listTitle = Styles.sectionTitle(this, "收藏记录");
        TextView listHint = Styles.miniText(this, "点击卡片打开详情，卡片按钮可直接编辑、删除或跳地图");
        listHint.setGravity(Gravity.END);

        LinearLayout.LayoutParams listTitleLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        listHeader.addView(listTitle, listTitleLp);
        listHeader.addView(listHint, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        scrollChild.addView(listHeader);

        FrameLayout listContainer = new FrameLayout(this);
        listContainer.setBackground(Styles.panelBackground());
        listContainer.setElevation(dp(8));
        listContainer.setPadding(dp(10), dp(10), dp(10), dp(10));
        scrollChild.addView(listContainer, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(540)));

        recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecordAdapter(this, new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);
        recyclerView.setVisibility(View.GONE);
        listContainer.addView(recyclerView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        emptyStateView = buildEmptyState();
        listContainer.addView(emptyStateView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        setContentView(root);
        updateFilterButtons();
    }

    private View buildEmptyState() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(24), dp(24), dp(24), dp(24));

        TextView emoji = Styles.title(this, "✦");
        emoji.setTextSize(34f);

        emptyTitleView = Styles.sectionTitle(this, "还没有任何收藏");
        emptyTitleView.setPadding(0, dp(8), 0, dp(4));

        emptyBodyView = Styles.body(this, "从任意 App 分享截图到这里，系统会自动 OCR 并保存店名、地址、电话、原图和时间。");
        emptyBodyView.setGravity(Gravity.CENTER_HORIZONTAL);
        emptyBodyView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        emptyBodyView.setPadding(dp(8), 0, dp(8), 0);

        emptyActionButton = Styles.primaryButton(this, "导入第一张截图");
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        btnLp.topMargin = dp(16);
        emptyActionButton.setOnClickListener(v -> openImagePicker());

        box.addView(emoji);
        box.addView(emptyTitleView);
        box.addView(emptyBodyView);
        box.addView(emptyActionButton, btnLp);
        return box;
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
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
        setRefreshing(true);
        executor.execute(() -> {
            List<ScreenshotRecord> records = repository.loadRecords();
            runOnUiThread(() -> {
                allRecords.clear();
                allRecords.addAll(records);
                applyFilters();
                setRefreshing(false);
            });
        });
    }

    private void setRefreshing(boolean refreshing) {
        if (refreshProgress != null) {
            refreshProgress.setVisibility(refreshing ? View.VISIBLE : View.GONE);
        }
        if (refreshButton != null) {
            refreshButton.setEnabled(!refreshing);
            refreshButton.setText(refreshing ? "刷新中…" : "刷新");
        }
    }

    private void applyFilters() {
        visibleRecords.clear();
        for (ScreenshotRecord record : allRecords) {
            if (matchesQuery(record) && matchesFilter(record)) {
                visibleRecords.add(record);
            }
        }
        adapter.setItems(visibleRecords);
        updateStats();
        updateEmptyState();
    }

    private boolean matchesQuery(ScreenshotRecord record) {
        if (TextUtils.isEmpty(currentQuery)) {
            return true;
        }
        String query = currentQuery.trim().toLowerCase(Locale.getDefault());
        return containsIgnoreCase(record.title, query)
                || containsIgnoreCase(record.address, query)
                || containsIgnoreCase(record.phone, query)
                || containsIgnoreCase(record.notes, query)
                || containsIgnoreCase(record.sourceLabel, query)
                || containsIgnoreCase(record.rawText, query);
    }

    private boolean matchesFilter(ScreenshotRecord record) {
        switch (currentFilter) {
            case FILTER_ADDRESS:
                return !isBlank(record.address);
            case FILTER_PHONE:
                return !isBlank(record.phone);
            case FILTER_RECENT:
                return record.createdAt >= System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L;
            case FILTER_NOTES:
                return !isBlank(record.notes);
            case FILTER_ALL:
            default:
                return true;
        }
    }

    private void setFilter(int filter) {
        currentFilter = filter;
        updateFilterButtons();
        applyFilters();
    }

    private void updateFilterButtons() {
        allFilterButton.setBackground(Styles.chipBackground(currentFilter == FILTER_ALL));
        addressFilterButton.setBackground(Styles.chipBackground(currentFilter == FILTER_ADDRESS));
        phoneFilterButton.setBackground(Styles.chipBackground(currentFilter == FILTER_PHONE));
        recentFilterButton.setBackground(Styles.chipBackground(currentFilter == FILTER_RECENT));
        notesFilterButton.setBackground(Styles.chipBackground(currentFilter == FILTER_NOTES));
    }

    private void updateStats() {
        countView.setValue(allRecords.size() + " 条记录");
        if (visibleRecords.isEmpty()) {
            latestView.setValue("无匹配结果");
        } else {
            latestView.setValue(shorten(visibleRecords.get(0).title, 12));
        }
        statusView.setText(buildStatusText());
    }

    private void updateEmptyState() {
        boolean noData = allRecords.isEmpty();
        boolean noMatch = !noData && visibleRecords.isEmpty();
        emptyStateView.setVisibility((noData || noMatch) ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility((noData || noMatch) ? View.GONE : View.VISIBLE);

        if (noData) {
            emptyTitleView.setText("还没有任何收藏");
            emptyBodyView.setText("从任意 App 分享截图到这里，系统会自动 OCR 并保存店名、地址、电话、原图和时间。");
            emptyActionButton.setText("导入第一张截图");
            emptyActionButton.setOnClickListener(v -> openImagePicker());
        } else if (noMatch) {
            emptyTitleView.setText("没有找到匹配结果");
            emptyBodyView.setText("换个关键词，或者切回“全部”查看当前收藏。");
            emptyActionButton.setText("清空搜索");
            emptyActionButton.setOnClickListener(v -> {
                searchField.setText("");
                setFilter(FILTER_ALL);
            });
        }
    }

    private String filterLabel(int filter) {
        switch (filter) {
            case FILTER_ADDRESS:
                return "有地址";
            case FILTER_PHONE:
                return "有电话";
            case FILTER_RECENT:
                return "近 7 天";
            case FILTER_NOTES:
                return "有备注";
            case FILTER_ALL:
            default:
                return "全部";
        }
    }

    private void startImport(Uri uri, String sourceLabel) {
        if (importing) {
            Toast.makeText(this, "正在处理上一张截图，请稍等", Toast.LENGTH_SHORT).show();
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

    private void showRecordDialog(ScreenshotRecord record) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));
        panel.setBackground(Styles.panelBackground());
        scroll.addView(panel, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = Styles.sectionTitle(this, "编辑详情");
        TextView subtitle = Styles.body(this, "你可以修改识别结果、补充备注、删除记录或直接跳转到高德地图。");
        subtitle.setPadding(0, dp(6), 0, dp(12));
        panel.addView(title);
        panel.addView(subtitle);

        ImageView preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        preview.setBackground(Styles.previewBackground());
        int previewHeight = dp(210);
        preview.setImageBitmap(BitmapFactory.decodeFile(record.thumbnailPath));
        LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                previewHeight);
        previewLp.bottomMargin = dp(14);
        panel.addView(preview, previewLp);

        EditText titleField = Styles.inputField(this, "标题", false);
        titleField.setText(record.title);
        panel.addView(wrapField("标题", titleField));

        EditText addressField = Styles.inputField(this, "地址", false);
        addressField.setText(record.address);
        panel.addView(wrapField("地址", addressField));

        EditText phoneField = Styles.inputField(this, "电话", false);
        phoneField.setInputType(InputType.TYPE_CLASS_PHONE);
        phoneField.setText(record.phone);
        panel.addView(wrapField("电话", phoneField));

        EditText sourceField = Styles.inputField(this, "来源", false);
        sourceField.setText(record.sourceLabel);
        panel.addView(wrapField("来源", sourceField));

        EditText notesField = Styles.inputField(this, "备注 / 补充信息", true);
        notesField.setText(record.notes);
        panel.addView(wrapField("备注", notesField));

        EditText rawField = Styles.inputField(this, "OCR 原始识别文本", true);
        rawField.setMinLines(5);
        rawField.setText(record.rawText);
        panel.addView(wrapField("识别文本", rawField));

        TextView createdAt = Styles.miniText(this, "创建时间：" + formatDate(record.createdAt));
        createdAt.setPadding(0, dp(10), 0, dp(10));
        panel.addView(createdAt);

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setPadding(0, dp(6), 0, 0);

        Button mapButton = Styles.secondaryButton(this, "打开高德");
        mapButton.setEnabled(!isBlank(record.address));
        mapButton.setAlpha(isBlank(record.address) ? 0.45f : 1f);
        mapButton.setOnClickListener(v -> openMap(addressField.getText().toString().trim()));

        Button saveButton = Styles.primaryButton(this, "保存修改");
        saveButton.setOnClickListener(v -> saveEditedRecord(dialog, record, titleField, addressField, phoneField, sourceField, notesField, rawField));

        Button deleteButton = Styles.dangerButton(this, "删除记录");
        deleteButton.setOnClickListener(v -> {
            dialog.dismiss();
            confirmDelete(record);
        });

        LinearLayout.LayoutParams actionLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        actionLp.rightMargin = dp(8);
        actionRow.addView(mapButton, actionLp);

        LinearLayout.LayoutParams actionLp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        actionLp2.rightMargin = dp(8);
        actionRow.addView(saveButton, actionLp2);

        LinearLayout.LayoutParams actionLp3 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        actionRow.addView(deleteButton, actionLp3);
        panel.addView(actionRow);

        dialog.setContentView(scroll);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
        dialog.show();
    }

    private View wrapField(String label, EditText field) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);

        TextView labelView = Styles.miniText(this, label);
        labelView.setPadding(dp(4), 0, dp(4), dp(6));
        wrapper.addView(labelView);
        wrapper.addView(field);
        wrapper.setLayoutParams(lp);
        return wrapper;
    }

    private void saveEditedRecord(Dialog dialog,
                                  ScreenshotRecord original,
                                  EditText titleField,
                                  EditText addressField,
                                  EditText phoneField,
                                  EditText sourceField,
                                  EditText notesField,
                                  EditText rawField) {
        ScreenshotRecord updated = new ScreenshotRecord(
                original.id,
                trimToEmpty(titleField.getText().toString(), "未命名截图"),
                trimToEmpty(addressField.getText().toString(), ""),
                trimToEmpty(phoneField.getText().toString(), ""),
                trimToEmpty(rawField.getText().toString(), ""),
                trimToEmpty(notesField.getText().toString(), ""),
                original.imagePath,
                original.thumbnailPath,
                trimToEmpty(sourceField.getText().toString(), "本地"),
                original.createdAt);

        executor.execute(() -> {
            repository.saveRecord(updated);
            runOnUiThread(() -> {
                dialog.dismiss();
                Toast.makeText(this, "修改已保存", Toast.LENGTH_SHORT).show();
                refreshRecords();
            });
        });
    }

    private void confirmDelete(ScreenshotRecord record) {
        new AlertDialog.Builder(this)
                .setTitle("删除记录")
                .setMessage("确定删除「" + record.title + "」吗？原图和缩略图会保留在本地文件夹里，但记录会从列表移除。")
                .setPositiveButton("删除", (dialog, which) -> executor.execute(() -> {
                    repository.deleteRecord(record.id);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                        refreshRecords();
                    });
                }))
                .setNegativeButton("取消", null)
                .show();
    }

    private void openMap(String address) {
        if (isBlank(address)) {
            Toast.makeText(this, "没有可用地址", Toast.LENGTH_SHORT).show();
            return;
        }

        String keyword = Uri.encode(address.trim());
        Intent amapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                "androidamap://search?sourceApplication=SnapNest&keyword=" + keyword + "&dev=0"));
        amapIntent.setPackage("com.autonavi.minimap");

        if (amapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(amapIntent);
            return;
        }

        Intent fallback = new Intent(Intent.ACTION_VIEW, Uri.parse("https://uri.amap.com/search?keyword=" + keyword));
        if (fallback.resolveActivity(getPackageManager()) != null) {
            startActivity(fallback);
        } else {
            Toast.makeText(this, "未安装高德地图", Toast.LENGTH_SHORT).show();
        }
    }

    private void showModelSettings() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));
        panel.setBackground(Styles.panelBackground());

        TextView title = Styles.sectionTitle(this, "Gemini 模型设置");
        TextView hint = Styles.body(this, "填入 Google AI Studio 的 API key 后，导入截图时会优先使用 Gemini 2.5 Flash-Lite 做图片理解。没有 key 时会自动回退到本地 OCR。");
        hint.setPadding(0, dp(6), 0, dp(12));

        EditText keyField = Styles.inputField(this, "API Key", false);
        keyField.setText(getGeminiApiKey());

        TextView state = Styles.miniText(this, isBlank(getGeminiApiKey()) ? "当前：本地识别回退模式" : "当前：Gemini 优先模式");
        state.setPadding(0, dp(8), 0, dp(12));

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);

        Button saveButton = Styles.primaryButton(this, "保存");
        saveButton.setOnClickListener(v -> {
            saveGeminiApiKey(keyField.getText().toString());
            Toast.makeText(this, "模型设置已保存", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            updateStatusLine();
        });

        Button clearButton = Styles.secondaryButton(this, "清空");
        clearButton.setOnClickListener(v -> {
            keyField.setText("");
            saveGeminiApiKey("");
            state.setText("当前：本地识别回退模式");
            updateStatusLine();
        });

        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        saveLp.rightMargin = dp(8);
        actionRow.addView(saveButton, saveLp);
        LinearLayout.LayoutParams clearLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        actionRow.addView(clearButton, clearLp);

        panel.addView(title);
        panel.addView(hint);
        panel.addView(keyField);
        panel.addView(state);
        panel.addView(actionRow);

        dialog.setContentView(panel);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private void updateStatusLine() {
        statusView.setText(buildStatusText());
    }

    private String buildStatusText() {
        StringBuilder builder = new StringBuilder();
        builder.append("当前显示 ").append(visibleRecords.size()).append("/").append(allRecords.size()).append(" 条");
        builder.append(" · 筛选：").append(filterLabel(currentFilter));
        builder.append(" · 识别：").append(isBlank(getGeminiApiKey()) ? "本地回退" : "Gemini 优先");
        if (!TextUtils.isEmpty(currentQuery)) {
            builder.append(" · 关键词：").append(currentQuery.trim());
        }
        return builder.toString();
    }

    private String getGeminiApiKey() {
        SharedPreferences prefs = getSharedPreferences("snapnest_prefs", MODE_PRIVATE);
        return prefs.getString("gemini_api_key", "");
    }

    private void saveGeminiApiKey(String apiKey) {
        SharedPreferences prefs = getSharedPreferences("snapnest_prefs", MODE_PRIVATE);
        prefs.edit().putString("gemini_api_key", apiKey == null ? "" : apiKey.trim()).apply();
    }

    private boolean containsIgnoreCase(String value, String query) {
        if (TextUtils.isEmpty(value)) {
            return false;
        }
        return value.toLowerCase(Locale.getDefault()).contains(query);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToEmpty(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String clean = value.trim();
        return clean.isEmpty() ? fallback : clean;
    }

    private String formatDate(long time) {
        return new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(new Date(time));
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
