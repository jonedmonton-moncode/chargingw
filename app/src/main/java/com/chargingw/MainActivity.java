package com.chargingw;

import android.app.Activity;
import android.os.Bundle;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextWatcher;
import java.util.Locale;

public class MainActivity extends Activity {
    private TextView voltageText, currentText, powerText;
    private EditText refreshInput;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long refreshInterval = 1000L;
    private BatteryManager batteryManager;
    private boolean active = false;

    private final Runnable updater = new Runnable() {
        @Override public void run() {
            if (!active) return;
            updateBattery();
            handler.postDelayed(this, refreshInterval);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        voltageText = findViewById(R.id.voltage);
        currentText = findViewById(R.id.current);
        powerText = findViewById(R.id.power);
        refreshInput = findViewById(R.id.refresh);
        batteryManager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
        refreshInput.setText("1");
        refreshInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    double seconds = Double.parseDouble(s.toString().replace(',', '.'));
                    if (Double.isFinite(seconds) && seconds >= 0.1 && seconds <= 3600) {
                        refreshInterval = Math.max(100L, (long) (seconds * 1000));
                        if (active) {
                            handler.removeCallbacks(updater);
                            handler.post(updater);
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    @Override protected void onResume() {
        super.onResume();
        active = true;
        handler.removeCallbacks(updater);
        handler.post(updater);
    }

    @Override protected void onPause() {
        active = false;
        handler.removeCallbacks(updater);
        super.onPause();
    }

    private void updateBattery() {
        Intent status = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (status == null) return;
        int mv = status.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
        if (mv <= 0) {
            voltageText.setText("Indisponible");
            currentText.setText("Indisponible");
            powerText.setText("Indisponible");
            return;
        }
        double voltage = mv / 1000.0;
        voltageText.setText(String.format(Locale.FRANCE, "%.3f V", voltage));
        long ua = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        if (ua == Long.MIN_VALUE || ua == Integer.MIN_VALUE) {
            currentText.setText("Indisponible");
            powerText.setText("Indisponible");
            voltageText.setTextColor(Color.WHITE);
            currentText.setTextColor(Color.WHITE);
            powerText.setTextColor(Color.WHITE);
            return;
        }
        double current = ua / 1000000.0;
        double power = voltage * current;
        int color = current > 0 ? Color.rgb(0, 220, 80) : current < 0 ? Color.rgb(255, 60, 60) : Color.WHITE;
        currentText.setText(String.format(Locale.FRANCE, "%+.3f A", current));
        powerText.setText(String.format(Locale.FRANCE, "%+.3f W", power));
        voltageText.setTextColor(color);
        currentText.setTextColor(color);
        powerText.setTextColor(color);
    }
}
