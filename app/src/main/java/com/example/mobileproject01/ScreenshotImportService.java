package com.example.mobileproject01;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ScreenshotImportService extends Service {
    static final String ACTION_IMPORT_STARTED = "com.example.mobileproject01.IMPORT_STARTED";
    static final String ACTION_IMPORT_DONE = "com.example.mobileproject01.IMPORT_DONE";
    static final String ACTION_IMPORT_FAILED = "com.example.mobileproject01.IMPORT_FAILED";
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_ERROR = "error";
    static final String EXTRA_SOURCE = "source";

    private static final String CHANNEL_ID = "screenshot_import";
    private static final int NOTIFICATION_ID = 1002;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicInteger pendingJobs = new AtomicInteger();
    private ScreenshotRepository repository;

    @Override
    public void onCreate() {
        super.onCreate();
        repository = new ScreenshotRepository(this);
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification("等待识别任务"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getData() == null) {
            stopIfIdle();
            return START_NOT_STICKY;
        }

        Uri uri = intent.getData();
        String source = intent.getStringExtra(EXTRA_SOURCE);
        pendingJobs.incrementAndGet();
        sendBroadcast(new Intent(ACTION_IMPORT_STARTED));
        executor.execute(() -> processImport(uri, source == null ? "后台导入" : source));
        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private void processImport(Uri uri, String source) {
        try {
            updateNotification("正在识别截图，队列剩余 " + pendingJobs.get() + " 个");
            ScreenshotRecord record = repository.importFromUri(uri, source);
            Intent done = new Intent(ACTION_IMPORT_DONE);
            done.putExtra(EXTRA_TITLE, record.title);
            sendBroadcast(done);
        } catch (Exception e) {
            Intent failed = new Intent(ACTION_IMPORT_FAILED);
            failed.putExtra(EXTRA_ERROR, e.getMessage() == null ? "未知错误" : e.getMessage());
            sendBroadcast(failed);
        } finally {
            if (pendingJobs.decrementAndGet() <= 0) {
                stopIfIdle();
            } else {
                updateNotification("继续处理队列，剩余 " + pendingJobs.get() + " 个");
            }
        }
    }

    private void stopIfIdle() {
        if (pendingJobs.get() <= 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE);
            } else {
                stopForeground(true);
            }
            stopSelf();
        }
    }

    private void updateNotification(String text) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification(text));
        }
    }

    private Notification buildNotification(String text) {
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(text)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "截图识别",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("后台排队识别截图");
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
