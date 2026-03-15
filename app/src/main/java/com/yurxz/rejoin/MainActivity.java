package com.yurxz.rejoin;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerInstances;
    private InstanceAdapter adapter;
    private List<RobloxInstance> instances;
    private View emptyState;
    private MaterialButton btnStartStop, btnAddAccount;
    private TextView tvAdbStatus, tvServiceStatus, tvStatAccounts, tvStatRejoin, tvStatUptime;
    private View dotAdb;
    private Timer uptimeTimer;

    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context ctx, Intent i) { updateUI(); }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        loadInstances();
        updateUI();
        checkPermissions();
    }

    private void checkPermissions() {
        // Cek Accessibility
        if (!RejoinAccessibilityService.isRunning) {
            new AlertDialog.Builder(this)
                .setTitle("⚡ Aktifkan Accessibility")
                .setMessage("Aktifkan Layanan Aksesibilitas YURXZ Rejoin agar deteksi Roblox lebih akurat.")
                .setPositiveButton("Aktifkan", (d, w) ->
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
                .setNegativeButton("Nanti", null).show();
            return;
        }

        // Cek Usage Stats
        if (!hasUsageStatsPermission()) {
            new AlertDialog.Builder(this)
                .setTitle("📊 Izin Usage Stats")
                .setMessage("Aktifkan Akses Penggunaan agar aplikasi bisa mendeteksi jika Roblox keluar.")
                .setPositiveButton("Aktifkan", (d, w) ->
                    startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)))
                .setNegativeButton("Nanti", null).show();
        }
    }

    private boolean hasUsageStatsPermission() {
        try {
            AppOpsManager appOps = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) { return false; }
    }

    private void initViews() {
        recyclerInstances = findViewById(R.id.recyclerInstances);
        emptyState        = findViewById(R.id.emptyState);
        btnStartStop      = findViewById(R.id.btnStartStop);
        btnAddAccount     = findViewById(R.id.btnAddAccount);
        tvAdbStatus       = findViewById(R.id.tvAdbStatus);
        tvServiceStatus   = findViewById(R.id.tvServiceStatus);
        tvStatAccounts    = findViewById(R.id.tvStatAccounts);
        tvStatRejoin      = findViewById(R.id.tvStatRejoin);
        tvStatUptime      = findViewById(R.id.tvStatUptime);
        dotAdb            = findViewById(R.id.dotAdb);

        recyclerInstances.setLayoutManager(new LinearLayoutManager(this));
        recyclerInstances.setNestedScrollingEnabled(false);

        btnStartStop.setOnClickListener(v -> toggleService());
        btnAddAccount.setOnClickListener(v -> showAddDialog());
        findViewById(R.id.btnSettings).setOnClickListener(v ->
            startActivity(new Intent(this, SettingsActivity.class)));
    }

    private void loadInstances() {
        instances = AppConfig.getInstances(this);
        adapter = new InstanceAdapter(instances, new InstanceAdapter.Listener() {
            @Override public void onDelete(int pos) { deleteInstance(pos); }
            @Override public void onManualRejoin(int pos) { manualRejoin(pos); }
            @Override public void onClone(int pos) { cloneInstance(pos); }
        });
        recyclerInstances.setAdapter(adapter);
        updateEmptyState();
        tvStatAccounts.setText(String.valueOf(instances.size()));
    }

    private void updateEmptyState() {
        boolean empty = instances.isEmpty();
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerInstances.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void updateUI() {
        runOnUiThread(() -> {
            boolean running = RejoinService.running;
            boolean accOk   = RejoinAccessibilityService.isRunning;
            boolean usageOk = hasUsageStatsPermission();

            btnStartStop.setText(running ? "■  STOP REJOIN" : "▶  START REJOIN");
            btnStartStop.setBackgroundColor(running ? 0xFFEF4444 : 0xFF7C3AED);

            tvServiceStatus.setText(running ? "AKTIF" : "TIDAK AKTIF");
            tvServiceStatus.setTextColor(running ? 0xFF22C55E : 0xFF6B6888);

            // Dot hijau kalau Accessibility DAN UsageStats aktif
            boolean allOk = accOk && usageOk;
            dotAdb.setBackgroundResource(allOk ? R.drawable.dot_green : R.drawable.dot_grey);
            tvAdbStatus.setText(accOk ? (usageOk ? "ACC ✓" : "ACC") : "ACC");
            tvAdbStatus.setTextColor(allOk ? 0xFF22C55E : 0xFF6B6888);

            tvStatRejoin.setText(String.valueOf(RejoinService.rejoinCount));

            for (RobloxInstance inst : instances) {
                String status = RejoinService.instanceStatuses.get(inst.name);
                if (status != null) inst.status = status;
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void toggleService() {
        if (RejoinService.running) {
            Intent i = new Intent(this, RejoinService.class);
            i.setAction(RejoinService.ACTION_STOP);
            startService(i);
        } else {
            if (instances.isEmpty()) {
                Toast.makeText(this, "Tambahkan akun dulu!", Toast.LENGTH_SHORT).show();
                return;
            }
            // Cek overlay permission untuk floating widget
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && !Settings.canDrawOverlays(this)) {
                new AlertDialog.Builder(this)
                    .setTitle("Izin Jendela Pop-up")
                    .setMessage("Izin tampilkan di atas aplikasi lain diperlukan untuk floating widget.")
                    .setPositiveButton("Aktifkan", (d, w) -> {
                        Intent intent = new Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    })
                    .setNegativeButton("Lewati", (d, w) -> startRejoinService())
                    .show();
                return;
            }
            startRejoinService();
        }
        updateUI();
    }

    private void startRejoinService() {
        Intent i = new Intent(this, RejoinService.class);
        i.setAction(RejoinService.ACTION_START);
        startService(i);

        // Start floating widget kalau punya izin overlay
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || Settings.canDrawOverlays(this)) {
            startService(new Intent(this, FloatingWidgetService.class));
        }
        startUptimeTimer();
        updateUI();
    }

    private void showAddDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_account, null);
        TextInputEditText etName    = dialogView.findViewById(R.id.etName);
        TextInputEditText etPsLink  = dialogView.findViewById(R.id.etPsLink);
        TextInputEditText etPackage = dialogView.findViewById(R.id.etPackage);

        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String link = etPsLink.getText() != null ? etPsLink.getText().toString().trim() : "";
            String pkg  = etPackage.getText() != null ? etPackage.getText().toString().trim() : "com.roblox.client";

            if (name.isEmpty() || link.isEmpty()) {
                Toast.makeText(this, "Nama dan PS Link wajib diisi!", Toast.LENGTH_SHORT).show();
                return;
            }
            instances.add(new RobloxInstance(name, link, pkg));
            AppConfig.saveInstances(this, instances);
            adapter.notifyItemInserted(instances.size() - 1);
            tvStatAccounts.setText(String.valueOf(instances.size()));
            updateEmptyState();
            dialog.dismiss();
            Toast.makeText(this, "✅ " + name + " ditambahkan!", Toast.LENGTH_SHORT).show();
        });
        dialog.show();
    }

    private void deleteInstance(int pos) {
        new AlertDialog.Builder(this)
            .setTitle("Hapus Akun?")
            .setMessage("Yakin hapus " + instances.get(pos).name + "?")
            .setPositiveButton("Hapus", (d, w) -> {
                instances.remove(pos);
                AppConfig.saveInstances(this, instances);
                adapter.notifyItemRemoved(pos);
                tvStatAccounts.setText(String.valueOf(instances.size()));
                updateEmptyState();
            })
            .setNegativeButton("Batal", null).show();
    }

    private void manualRejoin(int pos) {
        Toast.makeText(this, "🔄 Manual rejoin: " + instances.get(pos).name,
            Toast.LENGTH_SHORT).show();
        Intent i = new Intent(this, RejoinService.class);
        i.setAction(RejoinService.ACTION_MANUAL_REJOIN);
        i.putExtra("index", pos);
        startService(i);
    }

    private void cloneInstance(int pos) {
        RobloxInstance o = instances.get(pos);
        RobloxInstance c = new RobloxInstance(o.name + " (Copy)", o.psLink, o.packageName);
        instances.add(c);
        AppConfig.saveInstances(this, instances);
        adapter.notifyItemInserted(instances.size() - 1);
        tvStatAccounts.setText(String.valueOf(instances.size()));
        updateEmptyState();
        Toast.makeText(this, "📋 Cloned: " + c.name, Toast.LENGTH_SHORT).show();
    }

    private void startUptimeTimer() {
        if (uptimeTimer != null) uptimeTimer.cancel();
        uptimeTimer = new Timer();
        uptimeTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                if (!RejoinService.running) { cancel(); return; }
                long sec = (System.currentTimeMillis() - RejoinService.startTimeMs) / 1000;
                String up = String.format("%02d:%02d:%02d", sec/3600, (sec%3600)/60, sec%60);
                runOnUiThread(() -> tvStatUptime.setText(up));
            }
        }, 1000, 1000);
    }

    @Override protected void onResume() {
        super.onResume();
        registerReceiver(statusReceiver,
            new IntentFilter(RejoinService.ACTION_STATUS_UPDATE));
        loadInstances();
        updateUI();
    }

    @Override protected void onPause() {
        super.onPause();
        try { unregisterReceiver(statusReceiver); } catch (Exception ignored) {}
    }
}
