package com.chargingw;
import android.appwidget.*;import android.content.*;import android.widget.*;import android.app.*;
public class ChargingWidget extends AppWidgetProvider {
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){for(int id:ids){RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget);v.setTextViewText(R.id.value,"–");v.setOnClickPendingIntent(R.id.value,PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));m.updateAppWidget(id,v);}ChargeService.start(c);}
 @Override public void onEnabled(Context c){ChargeService.start(c);}
 @Override public void onDisabled(Context c){c.stopService(new Intent(c,ChargeService.class));}
}
