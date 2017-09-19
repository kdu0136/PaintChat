package com.example.kimdongun.paintchat.toast;

import android.content.Context;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.kimdongun.paintchat.CustomBitmapPool;
import com.example.kimdongun.paintchat.R;

import jp.wasabeef.glide.transformations.CropCircleTransformation;

/**
 * Created by KimDongun on 2017-08-11.
 */

public class NormalChatToast extends Toast {
    private Context context_;
    private Toast toast_;

    public NormalChatToast(Context context) {
        super(context);
        this.context_ = context;
    }

    public void showToast(String profile, String nick, String msg){
        View view = View.inflate(context_, R.layout.toast_normal_chat, null);

        ImageView imageView_profile = (ImageView)view.findViewById(R.id.imageView_profile);
        TextView textView_nick = (TextView)view.findViewById(R.id.textView_nick);
        TextView textView_msg = (TextView)view.findViewById(R.id.textView_msg);

        Glide.with(context_)
                .load(profile + "Thumb.png")
                .error(R.mipmap.ic_launcher)
                .centerCrop()
                .bitmapTransform(new CropCircleTransformation(new CustomBitmapPool()))
                .into(imageView_profile);

        textView_nick.setText(nick);

        Uri uri = Uri.parse(msg);
        if (uri.getScheme() == null || uri.getScheme().isEmpty()) {
            // safe text
            textView_msg.setText(msg);
        } else {
            // has url
            textView_msg.setText("파일");
        }

        if(toast_ != null)
            toast_.cancel();
        this.toast_ = new Toast(context_);
        toast_.setView(view);
        toast_.setGravity(Gravity.CENTER, 0, 0);
        toast_.setDuration(Toast.LENGTH_SHORT);
        toast_.show();
    }
}
