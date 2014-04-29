package com.vulcan.flightlogger.util;

import com.vulcan.flightlogger.GPSActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootupReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
            Intent i = new Intent(context, GPSActivity.class);  
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);  
    }

}