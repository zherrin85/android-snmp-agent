package com.example.snmpagent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

public class BootReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {
            
            // Check if auto-start is enabled (you can add this preference later)
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean autoStart = prefs.getBoolean("auto_start", true);
            
            if (autoStart) {
                Intent serviceIntent = new Intent(context, SnmpAgentService.class);
                context.startForegroundService(serviceIntent);
            }
        }
    }
}

