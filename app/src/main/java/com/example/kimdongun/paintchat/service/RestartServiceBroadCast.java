package com.example.kimdongun.paintchat.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.kimdongun.paintchat.DebugHandler;


public class RestartServiceBroadCast extends BroadcastReceiver {
    public static final String ACTION_RESTART_SOCKET_SERVICE = "ACTION.RESTART.SOCKET_SERVICE";
    @Override
    public void onReceive(Context context, Intent intent) {
        DebugHandler.log(getClass().getName(), "RestartService called!!!!!!!!!!!!!!!!!!!!!!!");
        /* 서비스 죽일때 알람으로 다시 서비스 등록 */
        if (intent.getAction().equals(ACTION_RESTART_SOCKET_SERVICE)) {
            DebugHandler.log(getClass().getName(), "Service dead, but resurrection");
            Intent i = new Intent(context, SocketService.class);
            context.startService(i);
        }
        /* 폰 재부팅할때 서비스 등록 */
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            DebugHandler.log(getClass().getName(), "ACTION_BOOT_COMPLETED");
            Intent i = new Intent(context, SocketService.class);
            context.startService(i);
        }
    }
}