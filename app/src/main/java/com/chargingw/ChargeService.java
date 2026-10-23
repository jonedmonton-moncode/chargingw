package com.chargingw;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.widget.RemoteViews;
import java.util.Locale;

// Kept the existing class name to minimize changes to the project.
// No Android Service is started: only scheduled broadcasts are used.
public final class ChargeService {
    private static final int ALARM_REQUEST = 51;
    private static final int MIN_SECONDS = 15 * 60;

    private ChargeService() { }

    private static PendingIntent alarmIntent(Context context) {
        Intent intent = new Intent(context, ChargingWidget.class);
        intent.setAction(ChargingWidget.ACTION_REFRESH);
        return PendingIntent.getBroadcast(context, ALARM_REQUEST, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void start(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, ChargingWidget.class));
        if (ids.length == 0) {
            stop(context);
            return;
        }
        int requested = context.getSharedPreferences("prefs", 0).getInt("seconds", MIN_SECONDS);
        long interval = Math.max(MIN_SECONDS, requested) * 1000L;
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms != null) {
            PendingIntent pending = alarmIntent(context);
            alarms.cancel(pending);
            alarms.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    android.os.SystemClock.elapsedRealtime() + interval,
                    interval, pending);
        }
        update(context);
    }

    public static void stop(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms != null) alarms.cancel(alarmIntent(context));
    }

    public static void update(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, ChargingWidget.class));
        if (ids.length == 0) return;

        String wattsText = "N/A";
        String ampsText = "";
        Intent battery = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery != null) {
            int mv = battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
            int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
            int plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            if (bm != null && mv > 0) {
                long now = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
                long avg = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);
                long raw = valid(now) ? now : avg;
                if (valid(raw)) {
                    double magnitude = Math.abs((double) raw);
                    // Empirical POCO X5 workaround: small raw readings appear to be mA.
                    double amps = magnitude < 10000.0 ? magnitude / 1000.0 : magnitude / 1000000.0;
                    // Use battery status / plugged state, not the device-specific raw sign.
                    boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL;
                    double sign = charging ? 1.0 : -1.0;
                    // A connected device may still discharge; show negative in that case.
                    if (status == BatteryManager.BATTERY_STATUS_UNKNOWN && plugged != 0) {
                        sign = 1.0;
                    }
                    double signedAmps = sign * amps;
                    double signedWatts = signedAmps * mv / 1000.0;
                    wattsText = String.format(Locale.US, "%.1f W", signedWatts);
                    ampsText = String.format(Locale.US, "%.2f A", signedAmps);
                }
            }
        }
        for (int id : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
            views.setTextViewText(R.id.value, wattsText);
            views.setTextViewText(R.id.amps, ampsText);
            views.setOnClickPendingIntent(R.id.value, PendingIntent.getActivity(context, 0,
                    new Intent(context, MainActivity.class),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            views.setOnClickPendingIntent(R.id.amps, PendingIntent.getActivity(context, 0,
                    new Intent(context, MainActivity.class),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            manager.updateAppWidget(id, views);
        }
    }

    private static boolean valid(long value) {
        return value != 0 && value != Long.MIN_VALUE && value != Integer.MIN_VALUE;
    }
}
