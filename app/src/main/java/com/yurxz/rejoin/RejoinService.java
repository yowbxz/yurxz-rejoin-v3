package com.yurxz.rejoin;

import android.app.*;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.*;
import android.os.*;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RejoinService extends Service {
    public static final String TAG = "YurxzRejoin";
    public static final String CH_ID = "rejoin_ch";
    public static final String ACTION_START = "START";
    public static final String ACTION_STOP = "STOP";
    public static final String ACTION_MANUAL_REJOIN = "MANUAL_REJOIN";
    public static final String ACTION_STATUS_UPDATE = "com.yurxz.rejoin.STATUS_UPDATE";

    public static volatile boolean running = false;
    public static volatile boolean isAnyRobloxRunning = false;
    public static volatile boolean isActive = false;
    public static volatile int rejoinCount = 0;
    public static volatile int refreshLoops = 0;
    public static volatile long startTimeMs = 0;
    public static final List<String> logs = new ArrayList<>();
    public static final Map<String, String> instanceStatuses = new HashMap<>();
    public static final Map<String, Long> lastRejoinTime = new HashMap<>();
    public static final List<String> activeInstances = new ArrayList<>();
    public static final List<String> allInstances = new ArrayList<>();

    private Handler handler;
    private Runnable monitoringRunnable;
    private final Map<String, Runnable> rejoinRunnables = new HashMap<>();
    private final Map<String, Runnable> ssRunnables = new HashMap<>();
    private ExecutorService executor;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        executor = Executors.newSingleThreadExecutor();
        createNotificationChannel();
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = power.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK, "YurxzRejoin::WakeLock");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;
        String action = intent.getAction();
        if (action == null) return START_STICKY;
        switch (action) {
            case ACTION_START: startMonitoringTask(); break;
            case ACTION_STOP: stopRejoin(); break;
            case ACTION_MANUAL_REJOIN:
                int idx = intent.getIntExtra("index", -1);
                if (idx >= 0) manualRejoin(idx);
                break;
        }
        return START_STICKY;
    }

    public static void startServiceIfNeeded(Context ctx) {
        if (!running) {
            Intent i = new Intent(ctx, RejoinService.class);
            i.setAction(ACTION_START);
            ctx.startService(i);
        }
    }

    private void startMonitoringTask() {
        if (running) return;
        running = true;
        isActive = true;
        rejoinCount = 0;
        refreshLoops = 0;
        startTimeMs = System.currentTimeMillis();
        addLog("▶ Monitoring dimulai");

        if (!wakeLock.isHeld()) wakeLock.acquire();
        if (!RejoinAccessibilityService.isRunning)
            addLog("⚠ Accessibility belum aktif");

        // Log status hasUsageStatsPermission
        if (!hasUsageStatsPermission()) {
            addLog("⚠ Izin Usage Stats belum aktif - deteksi fallback ke pidof");
        }

        // Populate allInstances
        allInstances.clear();
        for (RobloxInstance inst : AppConfig.getInstances(this)) {
            allInstances.add(inst.name);
        }

        startMyForeground();

        int interval = AppConfig.getInterval(this) * 1000;
        monitoringRunnable = new Runnable() {
            @Override public void run() {
                if (!running) return;
                refreshLoops++;
                checkAndRejoinIfClosed();
                handler.postDelayed(this, interval);
            }
        };
        handler.postDelayed(monitoringRunnable, 3000);

        if (AppConfig.getAutoScreenshot(this)) {
            setupSsRunnables();
        }

        sendStatusBroadcast();
    }

    private void setupSsRunnables() {
        List<RobloxInstance> instances = AppConfig.getInstances(this);
        int ssInterval = AppConfig.getSsInterval(this) * 1000;
        for (RobloxInstance inst : instances) {
            final String instName = inst.name;
            final String instPkg = inst.packageName;
            Runnable ssR = new Runnable() {
                @Override public void run() {
                    if (!running) return;
                    String webhook = AppConfig.getWebhook(RejoinService.this);
                    if (!webhook.isEmpty()) {
                        executor.execute(() ->
                            sendToDiscord(webhook, instName, instPkg,
                                "Auto Screenshot (" + ssInterval/1000 + "s)"));
                    }
                    handler.postDelayed(this, ssInterval);
                }
            };
            ssRunnables.put(inst.name, ssR);
            handler.postDelayed(ssR, ssInterval);
        }
    }

    private void stopRejoin() {
        running = false;
        isActive = false;
        activeInstances.clear();

        if (handler != null) {
            if (monitoringRunnable != null)
                handler.removeCallbacks(monitoringRunnable);
            for (Runnable r : rejoinRunnables.values())
                handler.removeCallbacks(r);
            for (Runnable r : ssRunnables.values())
                handler.removeCallbacks(r);
            handler.removeCallbacksAndMessages(null);
        }
        rejoinRunnables.clear();
        ssRunnables.clear();

        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();

        if (AppConfig.getAutoLowRes(this)) {
            executor.execute(() -> {
                runShell("wm size reset");
                runShell("wm density reset");
                addLog("📱 Resolusi direset ke default");
            });
        }

        if (executor != null) executor.shutdown();

        addLog("■ Monitoring dihentikan. Total loop: " + refreshLoops);
        stopForeground(true);
        stopSelf();
        sendStatusBroadcast();
    }

    private void checkAndRejoinIfClosed() {
        List<RobloxInstance> instances = AppConfig.getInstances(this);
        if (instances.isEmpty()) return;

        activeInstances.clear();

        for (RobloxInstance inst : instances) {
            try {
                isAnyRobloxRunning = isAnyRobloxRunning(inst.packageName);

                if (!isAnyRobloxRunning) {
                    addLog("⚠ " + inst.name + " tidak berjalan, rejoining...");
                    instanceStatuses.put(inst.name, "🔄 Rejoining...");
                    updateLiveStatus();

                    applySettings(inst.packageName);

                    final RobloxInstance fi = inst;
                    Runnable rejoinR = () -> {
                        startNextActivity(fi);
                        rejoinCount++;
                        lastRejoinTime.put(fi.name, System.currentTimeMillis());

                        String webhook = AppConfig.getWebhook(this);
                        if (!webhook.isEmpty()) {
                            executor.execute(() ->
                                sendToDiscord(webhook, fi.name, fi.packageName,
                                    "Auto Reconnect (Detected Closed)"));
                        }

                        instanceStatuses.put(fi.name, "✅ Rejoined #" + rejoinCount);
                        addLog("✅ " + fi.name + " rejoined! Total: " + rejoinCount);
                        updateNotification();
                        updateLiveStatus();
                    };
                    rejoinRunnables.put(inst.name, rejoinR);
                    handler.post(rejoinR);

                } else {
                    activeInstances.add(inst.name);
                    instanceStatuses.put(inst.name, "✅ Running");
                }
            } catch (Exception e) {
                instanceStatuses.put(inst.name, "❌ Error");
                addLog("❌ Error " + inst.name + ": " + e.getMessage());
            }
        }
        sendStatusBroadcast();
    }

    // hasUsageStatsPermission - cek izin sebelum query UsageEvents
    private boolean hasUsageStatsPermission() {
        try {
            AppOpsManager appOps = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isAnyRobloxRunning(String packageName) {
        // Metode 1: Accessibility
        if (RejoinAccessibilityService.isRunning) {
            String fg = RejoinAccessibilityService.lastForegroundPackage;
            if (packageName.equals(fg)) return true;
        }

        // Metode 2: UsageEvents - hanya kalau ada izin (hasUsageStatsPermission)
        if (hasUsageStatsPermission()) {
            try {
                UsageStatsManager usm = (UsageStatsManager)
                    getSystemService(Context.USAGE_STATS_SERVICE);
                long now = System.currentTimeMillis();
                UsageEvents events = usm.queryEvents(now - 10000, now);
                UsageEvents.Event event = new UsageEvents.Event();
                String latestFg = null;
                long latestEventTime = 0;
                while (events.hasNextEvent()) {
                    events.getNextEvent(event);
                    if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND
                            && event.getTimeStamp() > latestEventTime) {
                        latestFg = event.getPackageName();
                        latestEventTime = event.getTimeStamp();
                    }
                }
                if (packageName.equals(latestFg)) return true;
            } catch (Exception ignored) {}
        }

        // Metode 3: pidof fallback
        String pid = runShell("pidof " + packageName);
        return pid != null && !pid.trim().isEmpty();
    }

    private void applySettings(String pkg) {
        if (AppConfig.getAutoLowRes(this)) {
            int w = AppConfig.getResWidth(this);
            int h = AppConfig.getResHeight(this);
            int d = AppConfig.getResDensity(this);
            runShell("wm size " + w + "x" + h);
            runShell("wm density " + d);
        }
    }

    private void startNextActivity(RobloxInstance inst) {
        try {
            if (AppConfig.getFloatingWindow(this)) {
                runShell("am start --windowingMode 5 -a android.intent.action.VIEW -d \""
                    + inst.psLink + "\"");
            } else {
                runShell("am start -a android.intent.action.VIEW -d \""
                    + inst.psLink + "\"");
            }
            Thread.sleep(2000);
        } catch (Exception e) {
            addLog("❌ Gagal buka: " + e.getMessage());
        }
    }

    private void rejoinRoblox(RobloxInstance inst) {
        startNextActivity(inst);
    }

    private void manualRejoin(int idx) {
        handler.post(() -> {
            List<RobloxInstance> instances = AppConfig.getInstances(this);
            if (idx < instances.size()) {
                RobloxInstance inst = instances.get(idx);
                addLog("🔄 Manual rejoin: " + inst.name);
                applySettings(inst.packageName);
                startNextActivity(inst);
                rejoinCount++;
                lastRejoinTime.put(inst.name, System.currentTimeMillis());
                instanceStatuses.put(inst.name, "✅ Manual Rejoined #" + rejoinCount);
                updateLiveStatus();
            }
        });
    }

    private void updateLiveStatus() {
        sendStatusBroadcast();
    }

    private String runShell(String cmd) {
        try {
            java.lang.Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            p.waitFor(5, TimeUnit.SECONDS);
            return sb.toString();
        } catch (Exception e) { return ""; }
    }

    private void sendToDiscord(String url, String account, String pkg, String reason) {
        try {
            long uptime = (System.currentTimeMillis() - startTimeMs) / 1000;
            String uptimeStr = String.format("%02d:%02d:%02d",
                uptime/3600, (uptime%3600)/60, uptime%60);
            String ts = new java.text.SimpleDateFormat("HH:mm:ss dd/MM/yyyy",
                java.util.Locale.getDefault()).format(new Date());

            String body = "{\"embeds\":[{\"title\":\"⚡ YURXZ Rejoin - Status Update\","
                + "\"color\":7340237,"
                + "\"fields\":["
                + "{\"name\":\"Account\",\"value\":\"**" + account + "**\",\"inline\":true},"
                + "{\"name\":\"Package\",\"value\":\"`" + pkg + "`\",\"inline\":true},"
                + "{\"name\":\"Alasan/Aksi\",\"value\":\"" + reason + "\",\"inline\":false},"
                + "{\"name\":\"Total Rejoin\",\"value\":\"" + rejoinCount + "x\",\"inline\":true},"
                + "{\"name\":\"Loop\",\"value\":\"" + refreshLoops + "\",\"inline\":true},"
                + "{\"name\":\"Uptime\",\"value\":\"" + uptimeStr + "\",\"inline\":true},"
                + "{\"name\":\"Waktu\",\"value\":\"" + ts + "\",\"inline\":false}"
                + "],\"footer\":{\"text\":\"YURXZ Rejoin System\"}}]}";

            URL u = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);
            conn.getOutputStream().write(body.getBytes());
            conn.getResponseCode();
            conn.disconnect();
        } catch (Exception e) { addLog("❌ Webhook error: " + e.getMessage()); }
    }

    private void sendDiscordWebhook(String url, String account, String pkg, String reason) {
        sendToDiscord(url, account, pkg, reason);
    }

    public static void addLog(String msg) {
        String ts = new java.text.SimpleDateFormat("HH:mm:ss",
            java.util.Locale.getDefault()).format(new Date());
        synchronized (logs) {
            logs.add("[" + ts + "] " + msg);
            if (logs.size() > 200) logs.remove(0);
        }
        Log.d(TAG, msg);
    }

    private void sendStatusBroadcast() {
        sendBroadcast(new Intent(ACTION_STATUS_UPDATE));
    }

    private void updateNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification n = new NotificationCompat.Builder(this, CH_ID)
            .setContentTitle("⚡ YURXZ Rejoin - " + rejoinCount + "x rejoined")
            .setContentText("Loop: " + refreshLoops + " | Aktif: "
                + activeInstances.size() + " akun")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true).build();
        nm.notify(1, n);
    }

    private void startMyForeground() {
        Notification n = new NotificationCompat.Builder(this, CH_ID)
            .setContentTitle("⚡ YURXZ Rejoin Aktif")
            .setContentText("Memantau " + AppConfig.getInstances(this).size() + " akun Roblox")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true).build();
        startForeground(1, n);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CH_ID,
                getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .createNotificationChannel(ch);
        }
    }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override public void onDestroy() {
        super.onDestroy();
        running = false;
        isActive = false;
        if (handler != null) handler.removeCallbacksAndMessages(null);
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        if (executor != null) executor.shutdown();
    }
}
