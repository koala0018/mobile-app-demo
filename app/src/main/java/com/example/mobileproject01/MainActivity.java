package com.example.mobileproject01;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
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
import android.view.animation.DecelerateInterpolator;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity implements RecordAdapter.OnRecordActionListener {
    private static final int REQUEST_PICK_IMAGE = 1001;
    private static final String PREFS = "snapnest_prefs";
    private static final String KEY_PROVIDER = "ai_provider";
    private static final String PROVIDER_GEMINI = "gemini";
    private static final String PROVIDER_SILICONFLOW = "siliconflow";
    private static final String KEY_GEMINI_API_KEY = "gemini_api_key";
    private static final String KEY_SILICONFLOW_API_KEY = "siliconflow_api_key";
    private static final String KEY_SILICONFLOW_MODEL = "siliconflow_model";
    private static final String KEY_PASSWORD_HASH = "key_password_hash";
    private static final int FILTER_ALL = 0;
    private static final int FILTER_ADDRESS = 1;
    private static final int FILTER_PHONE = 2;
    private static final int FILTER_NOTES = 3;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<ScreenshotRecord> allRecords = new ArrayList<>();
    private final List<ScreenshotRecord> visibleRecords = new ArrayList<>();

    private ScreenshotRepository repository;
    private RecordAdapter adapter;
    private RecyclerView recyclerView;
    private TextView countView;
    private TextView statusView;
    private TextView emptyView;
    private TextView providerView;
    private EditText searchField;
    private ProgressBar progressBar;
    private Button importButton;
    private Button keyButton;
    private Button allFilterButton;
    private Button addressFilterButton;
    private Button phoneFilterButton;
    private Button notesFilterButton;
    private String currentQuery = "";
    private int currentFilter = FILTER_ALL;
    private int runningImports;
    private final BroadcastReceiver importReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ScreenshotImportService.ACTION_IMPORT_STARTED.equals(action)) {
                runningImports++;
                updateStatus();
            } else if (ScreenshotImportService.ACTION_IMPORT_DONE.equals(action)) {
                runningImports = Math.max(0, runningImports - 1);
                String title = intent.getStringExtra(ScreenshotImportService.EXTRA_TITLE);
                Toast.makeText(MainActivity.this, "识别完成：" + (title == null ? "新记录" : title), Toast.LENGTH_SHORT).show();
                refreshRecords();
            } else if (ScreenshotImportService.ACTION_IMPORT_FAILED.equals(action)) {
                runningImports = Math.max(0, runningImports - 1);
                String error = intent.getStringExtra(ScreenshotImportService.EXTRA_ERROR);
                Toast.makeText(MainActivity.this, "识别失败：" + (error == null ? "未知错误" : error), Toast.LENGTH_LONG).show();
                updateStatus();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new ScreenshotRepository(this);
        buildUi();
        registerImportReceiver();
        refreshRecords();
        ensureApiKey();
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
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            startImport(data.getData(), "手动选择");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(importReceiver);
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
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(14), dp(14), dp(12));
        root.setBackgroundColor(Styles.PAPER);

        KineticStripView kineticStrip = new KineticStripView(this);
        LinearLayout.LayoutParams stripLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(54));
        stripLp.bottomMargin = dp(10);
        root.addView(kineticStrip, stripLp);

        LinearLayout topBar = new LinearLayout(this);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        TextView title = Styles.title(this, getString(R.string.app_name));
        title.setTextSize(24f);
        countView = Styles.miniText(this, "0 条");
        providerView = Styles.miniText(this, "");
        titleBox.addView(title);
        titleBox.addView(countView);
        titleBox.addView(providerView);
        topBar.addView(titleBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        keyButton = Styles.accentButton(this, "Key", Styles.BLUE);
        keyButton.setTextSize(12f);
        keyButton.setOnClickListener(v -> requestKeySettingsAccess());
        LinearLayout.LayoutParams keyLp = new LinearLayout.LayoutParams(dp(62), ViewGroup.LayoutParams.WRAP_CONTENT);
        keyLp.rightMargin = dp(8);
        topBar.addView(keyButton, keyLp);

        importButton = Styles.primaryButton(this, "导入");
        importButton.setTextSize(12f);
        importButton.setOnClickListener(v -> openImagePicker());
        topBar.addView(importButton, new LinearLayout.LayoutParams(dp(72), ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(topBar);

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        searchRow.setPadding(0, dp(10), 0, dp(8));
        searchField = Styles.inputField(this, "搜索", false);
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
        searchRow.addView(searchField, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button refreshButton = Styles.secondaryButton(this, "刷新");
        refreshButton.setTextSize(12f);
        refreshButton.setOnClickListener(v -> refreshRecords());
        LinearLayout.LayoutParams refreshLp = new LinearLayout.LayoutParams(dp(72), ViewGroup.LayoutParams.WRAP_CONTENT);
        refreshLp.leftMargin = dp(8);
        searchRow.addView(refreshButton, refreshLp);
        root.addView(searchRow);

        LinearLayout filterRow = new LinearLayout(this);
        filterRow.setGravity(Gravity.CENTER_VERTICAL);
        allFilterButton = filterButton("全部", FILTER_ALL);
        addressFilterButton = filterButton("地址", FILTER_ADDRESS);
        phoneFilterButton = filterButton("电话", FILTER_PHONE);
        notesFilterButton = filterButton("备注", FILTER_NOTES);
        filterRow.addView(allFilterButton);
        addFilter(filterRow, addressFilterButton);
        addFilter(filterRow, phoneFilterButton);
        addFilter(filterRow, notesFilterButton);
        root.addView(filterRow);

        FrameLayout listFrame = new FrameLayout(this);
        LinearLayout.LayoutParams listLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f);
        listLp.topMargin = dp(10);
        listFrame.setBackground(Styles.panelBackground());
        listFrame.setPadding(dp(8), dp(8), dp(8), dp(8));

        recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecordAdapter(this, new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);
        listFrame.addView(recyclerView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        emptyView = Styles.body(this, "暂无记录");
        emptyView.setGravity(Gravity.CENTER);
        listFrame.addView(emptyView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(listFrame, listLp);

        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        statusRow.setPadding(0, dp(8), 0, 0);
        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressLp = new LinearLayout.LayoutParams(dp(22), dp(22));
        progressLp.rightMargin = dp(8);
        statusRow.addView(progressBar, progressLp);
        statusView = Styles.miniText(this, "");
        statusRow.addView(statusView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(statusRow);

        setContentView(root);
        updateFilterButtons();
        updateStatus();
        animateIntro(root);
    }

    private Button filterButton(String text, int filter) {
        Button button = Styles.chipButton(this, text);
        button.setOnClickListener(v -> {
            currentFilter = filter;
            updateFilterButtons();
            applyFilters();
        });
        return button;
    }

    private void addFilter(LinearLayout row, Button button) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(8);
        row.addView(button, lp);
    }

    private void registerImportReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ScreenshotImportService.ACTION_IMPORT_STARTED);
        filter.addAction(ScreenshotImportService.ACTION_IMPORT_DONE);
        filter.addAction(ScreenshotImportService.ACTION_IMPORT_FAILED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(importReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(importReceiver, filter);
        }
    }

    private void ensureApiKey() {
        if (TextUtils.isEmpty(getApiKey())) {
            showApiKeyDialog(false);
        }
    }

    private void requestKeySettingsAccess() {
        if (!hasKeyPassword()) {
            showApiKeyDialog(true);
            return;
        }
        showPasswordDialog(() -> showApiKeyDialog(true));
    }

    private void showPasswordDialog(Runnable onVerified) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(18), dp(18), dp(18));
        panel.setBackground(Styles.panelBackground());

        TextView title = Styles.sectionTitle(this, "输入密码");
        TextView body = Styles.body(this, "查看或修改 API Key 前需要验证本地密码。");
        body.setPadding(0, dp(6), 0, dp(12));

        EditText password = Styles.inputField(this, "密码", false);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END);
        actions.setPadding(0, dp(14), 0, 0);
        Button confirm = Styles.primaryButton(this, "确认");
        confirm.setOnClickListener(v -> {
            if (!verifyKeyPassword(password.getText().toString())) {
                Toast.makeText(this, "密码不正确", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            onVerified.run();
        });
        actions.addView(confirm, new LinearLayout.LayoutParams(dp(96), ViewGroup.LayoutParams.WRAP_CONTENT));

        panel.addView(title);
        panel.addView(body);
        panel.addView(password);
        panel.addView(actions);
        dialog.setContentView(panel);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private void showApiKeyDialog(boolean cancellable) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(cancellable);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(18), dp(18), dp(18));
        panel.setBackground(Styles.panelBackground());

        TextView title = Styles.sectionTitle(this, "大模型识别设置");
        TextView body = Styles.body(this, "选择识别模型并填写对应 API Key。国内模式经硅基流动调用视觉模型；Key 只保存在手机本地。");
        body.setPadding(0, dp(6), 0, dp(12));

        final String[] selectedProvider = {getProvider()};
        LinearLayout providerRow = new LinearLayout(this);
        providerRow.setGravity(Gravity.CENTER_VERTICAL);
        Button geminiButton = Styles.chipButton(this, "Gemini");
        Button siliconButton = Styles.chipButton(this, "国内视觉模型");
        providerRow.addView(geminiButton, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams siliconLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        siliconLp.leftMargin = dp(8);
        providerRow.addView(siliconButton, siliconLp);

        EditText input = Styles.inputField(this, keyHint(selectedProvider[0]), false);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        input.setText(getApiKey());

        TextView modelLabel = Styles.miniText(this, "硅基流动模型名称");
        modelLabel.setPadding(0, dp(12), 0, dp(4));
        EditText modelInput = Styles.inputField(this, GeminiImageAnalyzer.DEFAULT_SILICONFLOW_MODEL, false);
        modelInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        modelInput.setText(getSiliconFlowModel());

        boolean needsPasswordCreation = !hasKeyPassword();
        TextView passwordLabel = Styles.miniText(this, "创建本地密码");
        passwordLabel.setPadding(0, dp(12), 0, dp(4));
        EditText passwordInput = Styles.inputField(this, "至少 4 位", false);
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        TextView confirmPasswordLabel = Styles.miniText(this, "确认密码");
        confirmPasswordLabel.setPadding(0, dp(8), 0, dp(4));
        EditText confirmPasswordInput = Styles.inputField(this, "再次输入密码", false);
        confirmPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        Runnable syncProviderButtons = () -> {
            Styles.setChipSelected(geminiButton, PROVIDER_GEMINI.equals(selectedProvider[0]));
            Styles.setChipSelected(siliconButton, PROVIDER_SILICONFLOW.equals(selectedProvider[0]));
            input.setHint(keyHint(selectedProvider[0]));
            input.setText(getApiKey(selectedProvider[0]));
            boolean isSiliconFlow = PROVIDER_SILICONFLOW.equals(selectedProvider[0]);
            modelLabel.setVisibility(isSiliconFlow ? View.VISIBLE : View.GONE);
            modelInput.setVisibility(isSiliconFlow ? View.VISIBLE : View.GONE);
        };
        geminiButton.setOnClickListener(v -> {
            selectedProvider[0] = PROVIDER_GEMINI;
            syncProviderButtons.run();
        });
        siliconButton.setOnClickListener(v -> {
            selectedProvider[0] = PROVIDER_SILICONFLOW;
            syncProviderButtons.run();
        });
        syncProviderButtons.run();

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END);
        actions.setPadding(0, dp(14), 0, 0);

        Button save = Styles.primaryButton(this, "保存");
        save.setOnClickListener(v -> {
            String key = input.getText().toString().trim();
            if (key.length() < 16) {
                Toast.makeText(this, "API Key 看起来不完整", Toast.LENGTH_SHORT).show();
                return;
            }
            if (needsPasswordCreation) {
                String password = passwordInput.getText().toString();
                String confirmPassword = confirmPasswordInput.getText().toString();
                if (password.length() < 4) {
                    Toast.makeText(this, "密码至少 4 位", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!password.equals(confirmPassword)) {
                    Toast.makeText(this, "两次密码不一致", Toast.LENGTH_SHORT).show();
                    return;
                }
                saveKeyPassword(password);
            }
            saveProvider(selectedProvider[0]);
            saveApiKey(selectedProvider[0], key);
            if (PROVIDER_SILICONFLOW.equals(selectedProvider[0])) {
                saveSiliconFlowModel(modelInput.getText().toString());
            }
            dialog.dismiss();
            Toast.makeText(this, "已保存，后续会自动使用当前模型识别", Toast.LENGTH_SHORT).show();
            updateStatus();
        });
        actions.addView(save, new LinearLayout.LayoutParams(dp(96), ViewGroup.LayoutParams.WRAP_CONTENT));

        panel.addView(title);
        panel.addView(body);
        panel.addView(providerRow);
        panel.addView(input);
        panel.addView(modelLabel);
        panel.addView(modelInput);
        if (needsPasswordCreation) {
            panel.addView(passwordLabel);
            panel.addView(passwordInput);
            panel.addView(confirmPasswordLabel);
            panel.addView(confirmPasswordInput);
        }
        panel.addView(actions);

        dialog.setContentView(panel);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent != null && Intent.ACTION_SEND.equals(intent.getAction())) {
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri != null) {
                startImport(uri, "系统分享");
            }
        }
    }

    private void openImagePicker() {
        if (TextUtils.isEmpty(getApiKey())) {
            showApiKeyDialog(false);
            return;
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "选择截图"), REQUEST_PICK_IMAGE);
    }

    private void startImport(Uri uri, String sourceLabel) {
        if (TextUtils.isEmpty(getApiKey())) {
            showApiKeyDialog(false);
            return;
        }
        Intent intent = new Intent(this, ScreenshotImportService.class);
        intent.setData(uri);
        intent.putExtra(ScreenshotImportService.EXTRA_SOURCE, sourceLabel);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        Toast.makeText(this, "已加入后台识别队列", Toast.LENGTH_SHORT).show();
    }

    private void refreshRecords() {
        executor.execute(() -> {
            List<ScreenshotRecord> records = repository.loadRecords();
            runOnUiThread(() -> {
                allRecords.clear();
                allRecords.addAll(records);
                applyFilters();
            });
        });
    }

    private void applyFilters() {
        visibleRecords.clear();
        for (ScreenshotRecord record : allRecords) {
            if (matchesFilter(record) && matchesQuery(record)) {
                visibleRecords.add(record);
            }
        }
        adapter.setItems(visibleRecords);
        recyclerView.setVisibility(visibleRecords.isEmpty() ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(visibleRecords.isEmpty() ? View.VISIBLE : View.GONE);
        emptyView.setText(allRecords.isEmpty() ? "暂无记录" : "没有匹配结果");
        countView.setText(visibleRecords.size() + "/" + allRecords.size() + " 条");
        updateStatus();
    }

    private boolean matchesFilter(ScreenshotRecord record) {
        if (currentFilter == FILTER_ADDRESS) {
            return !blank(record.address);
        }
        if (currentFilter == FILTER_PHONE) {
            return !blank(record.phone);
        }
        if (currentFilter == FILTER_NOTES) {
            return !blank(record.notes);
        }
        return true;
    }

    private boolean matchesQuery(ScreenshotRecord record) {
        if (TextUtils.isEmpty(currentQuery)) {
            return true;
        }
        String query = currentQuery.trim().toLowerCase(Locale.getDefault());
        return contains(record.title, query)
                || contains(record.address, query)
                || contains(record.phone, query)
                || contains(record.notes, query)
                || contains(record.rawText, query);
    }

    private void updateFilterButtons() {
        Styles.setChipSelected(allFilterButton, currentFilter == FILTER_ALL);
        Styles.setChipSelected(addressFilterButton, currentFilter == FILTER_ADDRESS);
        Styles.setChipSelected(phoneFilterButton, currentFilter == FILTER_PHONE);
        Styles.setChipSelected(notesFilterButton, currentFilter == FILTER_NOTES);
    }

    private void updateStatus() {
        boolean busy = runningImports > 0;
        progressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
        importButton.setEnabled(!TextUtils.isEmpty(getApiKey()));
        keyButton.setText(TextUtils.isEmpty(getApiKey()) ? "Key" : "改Key");
        providerView.setText(modelLabel() + (TextUtils.isEmpty(getApiKey()) ? " / 未设置 Key" : " / 已设置 Key"));
        statusView.setText(busy ? "后台识别中，可继续浏览和操作" : modelLabel() + " 图片识别已就绪");
    }

    private void animateIntro(LinearLayout root) {
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationY(dp(10));
            child.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(i * 45L)
                    .setDuration(260)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    private void showRecordDialog(ScreenshotRecord record) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ScrollView scroll = new ScrollView(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));
        panel.setBackground(Styles.panelBackground());
        scroll.addView(panel);

        ImageView preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        preview.setBackground(Styles.previewBackground());
        preview.setImageBitmap(BitmapFactory.decodeFile(record.thumbnailPath));
        LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(190));
        previewLp.bottomMargin = dp(12);
        panel.addView(preview, previewLp);

        EditText title = field(panel, "名称", record.title, false);
        EditText address = field(panel, "地址", record.address, false);
        EditText phone = field(panel, "电话", record.phone, false);
        phone.setInputType(InputType.TYPE_CLASS_PHONE);
        EditText notes = field(panel, "备注", record.notes, true);
        EditText raw = field(panel, "识别摘要", record.rawText, true);

        TextView time = Styles.miniText(this, "创建时间：" + new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(new Date(record.createdAt)));
        time.setPadding(0, dp(6), 0, dp(10));
        panel.addView(time);

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        Button map = Styles.accentButton(this, "地图", Styles.YELLOW);
        Button save = Styles.primaryButton(this, "保存");
        Button delete = Styles.dangerButton(this, "删除");
        map.setOnClickListener(v -> openMap(address.getText().toString()));
        save.setOnClickListener(v -> saveRecord(dialog, record, title, address, phone, notes, raw));
        delete.setOnClickListener(v -> {
            dialog.dismiss();
            confirmDelete(record);
        });
        addAction(actions, map);
        addAction(actions, save);
        actions.addView(delete, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        panel.addView(actions);

        dialog.setContentView(scroll);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
        dialog.show();
    }

    private EditText field(LinearLayout panel, String label, String value, boolean multiline) {
        TextView text = Styles.miniText(this, label);
        text.setPadding(dp(2), dp(8), 0, dp(4));
        EditText input = Styles.inputField(this, label, multiline);
        input.setText(value);
        panel.addView(text);
        panel.addView(input);
        return input;
    }

    private void addAction(LinearLayout row, Button button) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = dp(8);
        row.addView(button, lp);
    }

    private void saveRecord(Dialog dialog, ScreenshotRecord original, EditText title, EditText address, EditText phone, EditText notes, EditText raw) {
        ScreenshotRecord updated = new ScreenshotRecord(
                original.id,
                value(title, "未命名截图"),
                value(address, ""),
                value(phone, ""),
                value(raw, ""),
                value(notes, ""),
                original.imagePath,
                original.thumbnailPath,
                original.sourceLabel,
                original.createdAt);
        executor.execute(() -> {
            repository.saveRecord(updated);
            runOnUiThread(() -> {
                dialog.dismiss();
                Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
                refreshRecords();
            });
        });
    }

    private void confirmDelete(ScreenshotRecord record) {
        new AlertDialog.Builder(this)
                .setTitle("删除记录")
                .setMessage("确定删除「" + record.title + "」吗？")
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
        if (blank(address)) {
            Toast.makeText(this, "没有可用地址", Toast.LENGTH_SHORT).show();
            return;
        }
        String raw = address.trim();
        String keyword = Uri.encode(raw);
        String source = "jietu_xunwei";

        if (tryOpenUri("androidamap://poi?sourceApplication=" + source + "&keywords=" + keyword + "&dev=0", "com.autonavi.minimap")) {
            return;
        }
        if (tryOpenUri("androidamap://search?sourceApplication=" + source + "&keywords=" + keyword + "&dev=0", "com.autonavi.minimap")) {
            return;
        }
        if (tryOpenUri("amapuri://poi?sourceApplication=" + source + "&keywords=" + keyword + "&dev=0", "com.autonavi.minimap")) {
            return;
        }
        if (tryOpenUri("geo:0,0?q=" + keyword, null)) {
            return;
        }
        if (tryOpenUri("https://uri.amap.com/search?keyword=" + keyword, null)) {
            return;
        }
        if (tryOpenUri("https://m.amap.com/search/mapview/keywords=" + keyword, null)) {
            return;
        }
        Toast.makeText(this, "无法打开地图，请检查高德地图或浏览器权限", Toast.LENGTH_LONG).show();
    }

    private boolean tryOpenUri(String uri, String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            if (!TextUtils.isEmpty(packageName)) {
                intent.setPackage(packageName);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String getApiKey() {
        return getApiKey(getProvider());
    }

    private String getApiKey(String provider) {
        String keyName = PROVIDER_SILICONFLOW.equals(provider) ? KEY_SILICONFLOW_API_KEY : KEY_GEMINI_API_KEY;
        return getSharedPreferences(PREFS, MODE_PRIVATE).getString(keyName, "");
    }

    private void saveApiKey(String provider, String key) {
        String keyName = PROVIDER_SILICONFLOW.equals(provider) ? KEY_SILICONFLOW_API_KEY : KEY_GEMINI_API_KEY;
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit().putString(keyName, key == null ? "" : key.trim()).apply();
    }

    private String getProvider() {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_PROVIDER, PROVIDER_SILICONFLOW);
    }

    private void saveProvider(String provider) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_PROVIDER, provider).apply();
    }

    private String modelLabel() {
        return PROVIDER_SILICONFLOW.equals(getProvider()) ? "国内 " + getSiliconFlowModel() : "Gemini";
    }

    private String keyHint(String provider) {
        return PROVIDER_SILICONFLOW.equals(provider) ? "硅基流动 sk-..." : "Google AIza...";
    }

    private String getSiliconFlowModel() {
        String model = getSharedPreferences(PREFS, MODE_PRIVATE).getString(
                KEY_SILICONFLOW_MODEL,
                GeminiImageAnalyzer.DEFAULT_SILICONFLOW_MODEL);
        if ("zai-org/GLM-4.6V".equals(model)
                || "Qwen/Qwen2.5-VL-7B-Instruct".equals(model)
                || "Qwen/Qwen3-VL-Embedding-8B".equals(model)) {
            return GeminiImageAnalyzer.DEFAULT_SILICONFLOW_MODEL;
        }
        return model;
    }

    private void saveSiliconFlowModel(String model) {
        String clean = model == null ? "" : model.trim();
        if (clean.isEmpty()) {
            clean = GeminiImageAnalyzer.DEFAULT_SILICONFLOW_MODEL;
        }
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putString(KEY_SILICONFLOW_MODEL, clean)
                .apply();
    }

    private boolean hasKeyPassword() {
        return !TextUtils.isEmpty(getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_PASSWORD_HASH, ""));
    }

    private void saveKeyPassword(String password) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putString(KEY_PASSWORD_HASH, hashPassword(password))
                .apply();
    }

    private boolean verifyKeyPassword(String password) {
        String stored = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_PASSWORD_HASH, "");
        return !TextUtils.isEmpty(stored) && stored.equals(hashPassword(password));
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((password == null ? "" : password).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : bytes) {
                builder.append(String.format(Locale.US, "%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private String value(EditText input, String fallback) {
        String value = input.getText() == null ? "" : input.getText().toString().trim();
        return value.isEmpty() ? fallback : value;
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.getDefault()).contains(query);
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
