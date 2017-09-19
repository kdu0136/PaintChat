package com.example.kimdongun.paintchat;

import android.util.Log;

/**
 * Created by KimDongun on 2017-08-06.
 */

public class DebugHandler {
    private static boolean debugOn = true;

    public static void log(String title, String content){
        if(debugOn) Log.d("DebugHandler-" + title, content);
    }
    public static void logE(String title, String content){
        if(debugOn) Log.e("DebugHandler-" + title, content);
    }
}
