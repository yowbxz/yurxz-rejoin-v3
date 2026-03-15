package com.yurxz.rejoin;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import java.util.concurrent.TimeUnit;

public class SettingsActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        TextInputEditText etWebhook    = findViewById(R.id.etWebhook);
        TextInputEditText etInterval   = findViewById(R.id.etInterval);
        TextInputEditText etSsInterval = findViewById(R.id.etSsInterval);
        TextInputEditText etResWidth   = findViewById(R.id.etResWidth);
        TextInputEditText etResHeight  = findViewById(R.id.etResHeight);
        TextInputEditText etResDensity = findViewById(R.id.etResDensity);
        SwitchMaterial switchLowRes    = findViewById(R.id.switchLowRes);
        SwitchMaterial switchScreenshot= findViewById(R.id.switchScreenshot);
        SwitchMaterial switchFloat     = findViewById(R.id.switchFloat);
        MaterialButton btnSave         = findViewById(R.id.btnSaveSettings);
        MaterialButton btnResetRes     = findViewById(R.id.btnResetRes);

        // Load values
        etWebhook.setText(AppConfig.getWebhook(this));
        etInterval.setText(String.valueOf(AppConfig.getInterval(this)));
        etSsInterval.setText(String.valueOf(AppConfig.getSsInterval(this)));
        etResWidth.setText(String.valueOf(AppConfig.getResWidth(this)));
        etResHeight.setText(String.valueOf(AppConfig.getResHeight(this)));
        etResDensity.setText(String.valueOf(AppConfig.getResDensity(this)));
        switchLowRes.setChecked(AppConfig.getAutoLowRes(this));
        switchScreenshot.setChecked(AppConfig.getAutoScreenshot(this));
        switchFloat.setChecked(AppConfig.getFloatingWindow(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Reset resolusi HP sekarang
        btnResetRes.setOnClickListener(v -> {
            new Thread(() -> {
                try {
                    java.lang.Process p1 = Runtime.getRuntime().exec(
                        new String[]{"sh", "-c", "wm size reset"});
                    p1.waitFor(3, TimeUnit.SECONDS);
                    java.lang.Process p2 = Runtime.getRuntime().exec(
                        new String[]{"sh", "-c", "wm density reset"});
                    p2.waitFor(3, TimeUnit.SECONDS);
                    runOnUiThread(() ->
                        Toast.makeText(this, "✅ Resolusi direset ke default!", Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    runOnUiThread(() ->
                        Toast.makeText(this, "❌ Gagal reset: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }).start();
        });

        btnSave.setOnClickListener(v -> {
            int interval   = parseIntSafe(etInterval, 30);
            int ssInterval = parseIntSafe(etSsInterval, 60);
            int resW       = parseIntSafe(etResWidth, 540);
            int resH       = parseIntSafe(etResHeight, 960);
            int resD       = parseIntSafe(etResDensity, 120);
            String wh      = etWebhook.getText() != null ? etWebhook.getText().toString().trim() : "";

            AppConfig.setWebhook(this, wh);
            AppConfig.setInterval(this, Math.max(10, interval));
            AppConfig.setSsInterval(this, Math.max(10, ssInterval));
            AppConfig.setResWidth(this, resW);
            AppConfig.setResHeight(this, resH);
            AppConfig.setResDensity(this, resD);
            AppConfig.setAutoLowRes(this, switchLowRes.isChecked());
            AppConfig.setAutoScreenshot(this, switchScreenshot.isChecked());
            AppConfig.setFloatingWindow(this, switchFloat.isChecked());

            Toast.makeText(this, "✅ Tersimpan!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private int parseIntSafe(TextInputEditText et, int def) {
        try {
            String s = et.getText() != null ? et.getText().toString().trim() : "";
            return s.isEmpty() ? def : Integer.parseInt(s);
        } catch (Exception e) { return def; }
    }
}
