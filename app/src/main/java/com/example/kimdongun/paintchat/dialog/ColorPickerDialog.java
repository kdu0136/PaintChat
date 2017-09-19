package com.example.kimdongun.paintchat.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;

import com.example.kimdongun.paintchat.R;

/**
 * Created by KimDongun on 2017-07-27.
 */

//생성 할 게임 방 설정을 하는 다이얼 로그
public class ColorPickerDialog extends Dialog implements View.OnClickListener{
    private Context context_; //다이얼로그 만들어질 때 context

    private LinearLayout layout_pen; //펜 선택
    private LinearLayout layout_canvas; //캔버스 선택

    public String choice = "";

    /**********************************
     * AddSocialDialog(Context context, Client client) - 각 변수들을 초기화
     * client - 친구 검색하는 유저의 정보
     **********************************/
    public ColorPickerDialog(Context context) {
        super(context);
        this.context_ = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //setCanceledOnTouchOutside(false);
        setContentView(R.layout.dialog_color_picker);

        //초기화
        layout_pen = (LinearLayout)findViewById(R.id.layout_pen);
        layout_canvas = (LinearLayout)findViewById(R.id.layout_canvas);

        //터치 이벤트
        layout_pen.setOnClickListener(this);
        layout_canvas.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.layout_pen:
                choice = "pen";
                dismiss();
                break;

            case R.id.layout_canvas:
                choice = "canvas";
                dismiss();
                break;
        }
    }
}
