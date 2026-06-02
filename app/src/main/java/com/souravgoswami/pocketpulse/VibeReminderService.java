package com.souravgoswami.pocketpulse;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import java.util.Locale;
import java.util.Random;

public class VibeReminderService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {
    static final String ACTION_START = "com.souravgoswami.pocketpulse.action.START";
    static final String ACTION_STOP = "com.souravgoswami.pocketpulse.action.STOP";
    static final String ACTION_REFRESH = "com.souravgoswami.pocketpulse.action.REFRESH";

    private static final String CHANNEL_ID = "pocket_pulse_running";
    private static final int NOTIFICATION_ID = 24;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private boolean running;
    private long lastDelayMs;
    private PowerManager.WakeLock wakeLock;

    private final Runnable pulseRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            ReminderSettings settings = ReminderSettings.load(VibeReminderService.this);
            VibrationPlayer.play(VibeReminderService.this, settings);
            scheduleNext(settings.nextDelayMs(random));
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        getSharedPreferences(ReminderSettings.PREFS, MODE_PRIVATE)
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopReminder();
            return START_NOT_STICKY;
        }
        if (ACTION_REFRESH.equals(action)) {
            if (running) {
                updateNotification(lastDelayMs > 0 ? lastDelayMs : ReminderSettings.load(this).nextDelayMs(random));
                return START_STICKY;
            }
            stopSelf();
            return START_NOT_STICKY;
        }

        running = true;
        ReminderSettings.setRunning(this, true);
        ReminderSettings settings = ReminderSettings.load(this);
        long delayMs = settings.nextDelayMs(random);
        applyWakeLockPolicy(settings);
        startAsForeground(delayMs);
        scheduleNext(delayMs);
        return START_STICKY;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (!running || ReminderSettings.KEY_RUNNING.equals(key)) {
            return;
        }
        ReminderSettings settings = ReminderSettings.load(this);
        applyWakeLockPolicy(settings);
        scheduleNext(settings.nextDelayMs(random));
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(pulseRunnable);
        getSharedPreferences(ReminderSettings.PREFS, MODE_PRIVATE)
                .unregisterOnSharedPreferenceChangeListener(this);
        releaseWakeLock();
        ReminderSettings.setRunning(this, false);
        running = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void scheduleNext(long delayMs) {
        lastDelayMs = delayMs;
        handler.removeCallbacks(pulseRunnable);
        updateNotification(delayMs);
        handler.postDelayed(pulseRunnable, delayMs);
    }

    private void applyWakeLockPolicy(ReminderSettings settings) {
        if (settings.highReliabilityMode && running) {
            acquireWakeLock();
        } else {
            releaseWakeLock();
        }
    }

    private void acquireWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            return;
        }
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager == null) {
            return;
        }
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PocketPulse:ReminderWakeLock");
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire();
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        wakeLock = null;
    }

    private void startAsForeground(long delayMs) {
        Notification notification = buildNotification(delayMs);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void stopReminder() {
        handler.removeCallbacks(pulseRunnable);
        releaseWakeLock();
        ReminderSettings.setRunning(this, false);
        running = false;
        stopForeground(true);
        stopSelf();
    }

    private Notification buildNotification(long delayMs) {
        AppSettings appSettings = AppSettings.load(this);
        ReminderSettings reminderSettings = ReminderSettings.load(this);
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                this,
                0,
                openIntent,
                pendingIntentFlags()
        );

        Intent stopIntent = new Intent(this, VibeReminderService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                1,
                stopIntent,
                pendingIntentFlags()
        );

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        builder.setSmallIcon(R.drawable.ic_stat_vibrate)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(formatDelay(delayMs, reminderSettings.highReliabilityMode))
                .setContentIntent(openPendingIntent)
                .setOngoing(true)
                .setShowWhen(false)
                .setColor(appSettings.accentColor)
                .addAction(R.drawable.ic_stat_vibrate, "Stop", stopPendingIntent);

        return builder.build();
    }

    private void updateNotification(long delayMs) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification(delayMs));
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Pocket Pulse running",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Shows that phone presence reminders are active.");
        channel.setSound(null, null);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private static int pendingIntentFlags() {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return flags;
    }

    private static String formatDelay(long delayMs, boolean highReliabilityMode) {
        long seconds = Math.max(1, delayMs / 1000L);
        String suffix = highReliabilityMode ? " - screen-off mode on" : "";
        return String.format(Locale.getDefault(), "Next pulse in about %d seconds%s", seconds, suffix);
    }
}
