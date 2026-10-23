package com.chargingw;

import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.os.*;
import android.widget.RemoteViews;
import java.util.Locale;

public class ChargeService extends Service {
    private static final String CHANNEL = "charging_w";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable task;
    private NotificationManager notifications;

    static void start(Context context) {
        int[] widgets = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(new ComponentName(context, ChargingWidget.class));
        if (widgets.length > 0) {
            try {
                context.startForegroundService(new Intent(context, ChargeService.class));
            } catch (Exception ignored) { }
        }
    }

    private Notification notification(String details) {
        return new Notification.Builder(this, CHANNEL)
                .setContentTitle("Charging W")
                .setContentText(details)
                .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
                .build();
    }

    @Override public void onCreate() {
        super.onCreate();
        notifications = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notifications.createNotificationChannel(new NotificationChannel(
                CHANNEL, "Charging W", NotificationManager.IMPORTANCE_LOW));
        startForeground(1, notification("Mesure en cours"));
        task = () -> {
            update();
            int seconds = getSharedPreferences("prefs", 0).getInt("seconds", 5);
            handler.postDelayed(task, Math.max(1, seconds) * 1000L);
        };
        handler.post(task);
    }

    private void update() {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        int[] ids = manager.getAppWidgetIds(new ComponentName(this, ChargingWidget.class));
        if (ids.length == 0) { stopSelf(); return; }

        String display = "–";
        String diagnostic = "Débranché";
        Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery != null && battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0) {
            BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
            long now = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
            long avg = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);
            int mv = battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
            diagnostic = "NOW=" + now + " AVG=" + avg + " V=" + mv + "mV";

            // POCO X5: observed values around -1520 appear to be mA,
            // despite the Android API specifying microamperes.
            // This is an empirical device-specific workaround.
            long current = now;
            if (current == 0 || current == Long.MIN_VALUE || current == Integer.MIN_VALUE) {
                current = avg;
            }
            if (mv > 0 && current != 0 && current != Long.MIN_VALUE
                    && current != Integer.MIN_VALUE) {
                double absoluteCurrent = Math.abs((double) current);
                double watts;
                if (absoluteCurrent < 10000.0) {
                    // Suspected milliamperes: mA * mV / 1,000,000.
                    watts = absoluteCurrent * mv / 1_000_000.0;
                } else {
                    // Standard Android microamperes: uA * mV / 1,000,000,000.
                    watts = absoluteCurrent * mv / 1_000_000_000.0;
                }
                display = String.format(Locale.US, "%.1f W", watts);
            } else {
                display = "N/A";
            }
        }
        notifications.notify(1, notification(diagnostic));
        for (int id : ids) {
            RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget);
            views.setTextViewText(R.id.value, display);
            views.setOnClickPendingIntent(R.id.value, PendingIntent.getActivity(
                    this, 0, new Intent(this, MainActivity.class),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            manager.updateAppWidget(id, views);
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
