package com.yurxz.rejoin;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        TextInputEditText etWebhook = findViewById(R.id.etWebhook);
        TextInputEditText etInterval = findViewById(R.id.etInterval);
        SwitchMaterial switchMute = findViewById(R.id.switchMute);
        SwitchMaterial switchLowRes = findViewById(R.id.switchLowRes);
        SwitchMaterial switchScreenshot = findViewById(R.id.switchScreenshot);
        SwitchMaterial switchFloat = findViewById(R.id.switchFloat);
        MaterialButton btnSave = findViewById(R.id.btnSaveSettings);

        // Load values
        etWebhook.setText(AppConfig.getWebhook(this));
        etInterval.setText(String.valueOf(AppConfig.getInterval(this)));
        switchMute.setChecked(AppConfig.getAutoMute(this));
        switchLowRes.setChecked(AppConfig.getAutoLowRes(this));
        switchScreenshot.setChecked(AppConfig.getAutoScreenshot(this));
        switchFloat.setChecked(AppConfig.getFloatingWindow(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> {
            String wh = etWebhook.getText() != null ? etWebhook.getText().toString().trim() : "";
            String intStr = etInterval.getText() != null ? etInterval.getText().toString().trim() : "30";
            int interval = 30;
            try { interval = Integer.parseInt(intStr); } catch (Exception ignored) {}

            AppConfig.setWebhook(this, wh);
            AppConfig.setInterval(this, Math.max(10, interval));
            AppConfig.setAutoMute(this, switchMute.isChecked());
            AppConfig.setAutoLowRes(this, switchLowRes.isChecked());
            AppConfig.setAutoScreenshot(this, switchScreenshot.isChecked());
            AppConfig.setFloatingWindow(this, switchFloat.isChecked());

            Toast.makeText(this, "✅ Tersimpan!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
