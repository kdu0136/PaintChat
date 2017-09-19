package com.example.kimdongun.paintchat.toast;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kimdongun.paintchat.R;

/**
 * Created by KimDongun on 2017-08-11.
 */

public class GameRoomToast extends Toast {
    private Context context_;

    public GameRoomToast(Context context) {
        super(context);
        this.context_ = context;
    }

    public void showToast(String msg){
        View view = View.inflate(context_, R.layout.toast_game_room, null);

        TextView textView_msg = (TextView)view.findViewById(R.id.textView_msg);
        textView_msg.setText(msg);

        Toast toast = new Toast(context_);
        toast.setView(view);
        toast.setGravity(Gravity.TOP, 0, Gravity.TOP);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.show();
    }
}
