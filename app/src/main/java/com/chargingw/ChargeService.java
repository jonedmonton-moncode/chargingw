package com.chargingw;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.*;
import android.widget.*;
import java.util.Locale;

public class ChargeService extends Service {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable task;
    private static final String CHANNEL = "charging_w";

    static void start(Context c) {
        if (AppWidgetManager.getInstance(c)
                .getAppWidgetIds(new ComponentName(c, ChargingWidget.class)).length > 0) {
            try {
                c.startForegroundService(new Intent(c, ChargeService.class));
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        NotificationChannel channel = new NotificationChannel(
                CHANNEL, "Charging W", NotificationManager.IMPORTANCE_LOW);

        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .createNotificationChannel(channel);

        Notification notification = new Notification.Builder(this, CHANNEL)
                .setContentTitle("Charging W")
                .setContentText("Actualisation du widget active")
                .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
                .build();

        startForeground(1, notification);

        task = () -> {
            update();
            int seconds = getSharedPreferences("prefs", 0).getInt("seconds", 5);
            handler.postDelayed(task, Math.max(1, seconds) * 1000L);
        };

        handler.post(task);
    }

    private void update() {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        int[] ids = manager.getAppWidgetIds(
                new ComponentName(this, ChargingWidget.class));

        if (ids.length == 0) {
            stopSelf();
            return;
        }

        String display = "–";

        Intent battery = registerReceiver(
                null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        if (battery != null) {
            int plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);

            if (plugged != 0) {
                BatteryManager bm =
                        (BatteryManager) getSystemService(BATTERY_SERVICE);

                long current = bm.getLongProperty(
                        BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);

                int voltage = battery.getIntExtra(
                        BatteryManager.EXTRA_VOLTAGE, 0);

                if (current != Long.MIN_VALUE && current != 0 && voltage > 0) {

                    double milliamps;

                    // Adaptation aux mesures observées sur POCO X5
                    if (Math.abs((double) current) < 10000) {
                        milliamps = Math.abs((double) current);
                    } else {
                        milliamps = Math.abs((double) current) / 1000.0;
                    }

                    double watts = milliamps * voltage / 1000000.0;

                    display = String.format(Locale.US, "%.1f W", watts);

                } else {
                    display = "N/A";
                }
            }
        }

        for (int id : ids) {
            RemoteViews views =
                    new RemoteViews(getPackageName(), R.layout.widget);

            views.setTextViewText(R.id.value, display);

            views.setOnClickPendingIntent(
                    R.id.value,
                    PendingIntent.getActivity(
                            this, 0,
                            new Intent(this, MainActivity.class),
                            PendingIntent.FLAG_UPDATE_CURRENT |
                            PendingIntent.FLAG_IMMUTABLE));

            manager.updateAppWidget(id, views);
        }
    }

   
