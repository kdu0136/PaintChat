package com.example.kimdongun.paintchat;

import android.content.Context;
import android.os.Handler;

/**
 * Created by KimDongun on 2017-09-11.
 */

public class LocationThread extends Thread {
    Context mContext;
    Handler handler;
    boolean isRun = true;

    public LocationThread(Context context, Handler handler) {
        this.mContext = context;
        this.handler = handler;
    }

    public void stopForever() {
        synchronized (this) {
            this.isRun = false;
        }
    }

    public void run() {
        //반복으로 수행할 작업
        while (isRun) {
            handler.sendEmptyMessage(0); //핸들러에게 메세지 보냄
            try {
                Thread.sleep(1000); //1분마다
            } catch (Exception e) {
            }
        }
    }
}
