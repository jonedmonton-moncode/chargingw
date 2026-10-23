package com.chargingw;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

public class ChargingWidget extends AppWidgetProvider {
    public static final String ACTION_REFRESH = "com.chargingw.ACTION_REFRESH";

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        ChargeService.start(context);
    }

    @Override public void onEnabled(Context context) {
        ChargeService.start(context);
    }

    @Override public void onDisabled(Context context) {
        ChargeService.stop(context);
    }

    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (ACTION_REFRESH.equals(action)
                || Intent.ACTION_POWER_CONNECTED.equals(action)
                || Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
            ChargeService.update(context);
        }
    }
}
