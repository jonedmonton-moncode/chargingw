package com.chargingw;
import android.content.*;
public class BootReceiver extends BroadcastReceiver { @Override public void onReceive(Context c,Intent i){ChargeService.start(c);} }
