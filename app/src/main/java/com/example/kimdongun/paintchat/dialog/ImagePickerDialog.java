package com.example.kimdongun.paintchat.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.activity.CameraActivity;
import com.example.kimdongun.paintchat.activity.CanvasActivity;
import com.example.kimdongun.paintchat.activity.MainActivity;

import static com.example.kimdongun.paintchat.activity.MainActivity.REQ_CODE_SELECT_CAMERA;
import static com.example.kimdongun.paintchat.activity.MainActivity.REQ_CODE_SELECT_CANVAS;
import static com.example.kimdongun.paintchat.activity.MainActivity.REQ_CODE_SELECT_IMAGE;

/**
 * Created by KimDongun on 2017-07-27.
 */

//프로필 or 배경 사진 변경을 하는 다이얼 로그
public class ImagePickerDialog extends Dialog implements View.OnClickListener{
    private Context context_; //다이얼로그 만들어질 때 context

    private TextView textView_photo; //사진 촬영
    private TextView textView_gallery; //앨범 선택
    private TextView textView_paint; //직접 그리기

    /**********************************
     * ImagePickerDialog(Context context, String imageName) - 각 변수들을 초기화
     **********************************/
    public ImagePickerDialog(Context context) {
        super(context);
        this.context_ = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //setCanceledOnTouchOutside(false);
        setContentView(R.layout.dialog_image_picker);

        //초기화
        textView_photo = (TextView) findViewById(R.id.textView_photo);
        textView_gallery = (TextView) findViewById(R.id.textView_gallery);
        textView_paint = (TextView) findViewById(R.id.textView_paint);

        //터치 이벤트
        textView_photo.setOnClickListener(this);
        textView_gallery.setOnClickListener(this);
        textView_paint.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.textView_photo: //사진 촬영 버튼
                Intent intent0 = new Intent(context_, CameraActivity.class);
//                intent0.putExtra("from", "main");
                ((MainActivity)context_).startActivityForResult(intent0, REQ_CODE_SELECT_CAMERA);
                break;

            case R.id.textView_gallery: //앨범 선택 버튼
                Intent intent1 = new Intent(Intent.ACTION_PICK);
                intent1.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
                intent1.setData(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                ((MainActivity) context_).startActivityForResult(intent1, REQ_CODE_SELECT_IMAGE);
                break;

            case R.id.textView_paint: //직접 그리기 버튼
                Intent intent2 = new Intent(context_, CanvasActivity.class);
                intent2.putExtra("from", "main");
                ((MainActivity)context_).startActivityForResult(intent2, REQ_CODE_SELECT_CANVAS);
                break;
        }
    }
}
