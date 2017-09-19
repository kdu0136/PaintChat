package com.example.kimdongun.paintchat.dialog;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kimdongun.paintchat.DebugHandler;
import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.adapter.ViewPagerPaintListAdapter;
import com.example.kimdongun.paintchat.fragment.FragmentPaintList;
import com.example.kimdongun.paintchat.server.OnFinishDBListener;
import com.example.kimdongun.paintchat.server.PostDB;

import java.util.ArrayList;

/**
 * Created by KimDongun on 2017-07-27.
 */

//생성 할 게임 방 설정을 하는 다이얼 로그
public class PaintListDialog extends DialogFragment implements View.OnClickListener{
    private TextView textView_quiz; //퀴즈 정답
    private Button button_save; //저장 버튼
    private ViewPager view_pager;
    private ViewPagerPaintListAdapter adapter;
    private LinearLayout layout_circle;
    private int width, height;

    private String url_ = "http://211.110.229.53/paint_image/%s";
    private ArrayList<String> fileNameArray = new ArrayList<>(); //그림파일 이름
    private ArrayList<String> quizNameArray = new ArrayList<>(); //퀴즈 정답

    public PaintListDialog() {
    }

    public void setDrawableList(ArrayList<String> fileNameArray, ArrayList<String> quizNameArray){
        this.fileNameArray.addAll(fileNameArray);
        this.quizNameArray.addAll(quizNameArray);
    }

    public void setDialogSize(int width, int height){
        this.width = width;
        this.height = height;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DebugHandler.log(getClass().getName(), "OnCreate");
    }

    @Override
    public void onStart() {
        super.onStart();
        DebugHandler.log(getClass().getName(), "onStart");

        if(getDialog() == null){
            return;
        }
        getDialog().getWindow().setLayout(width, height);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_paint_list, container);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        DebugHandler.log(getClass().getName(), "onCreateView");

        textView_quiz = (TextView)view.findViewById(R.id.textView_quiz);
        button_save = (Button)view.findViewById(R.id.button_save);
        view_pager = (ViewPager)view.findViewById(R.id.view_pager);
        adapter = new ViewPagerPaintListAdapter(getChildFragmentManager());

        layout_circle = (LinearLayout)view.findViewById(R.id.layout_circle);

        //그림 목록
        for(int i = 0; i < fileNameArray.size(); i++) {
            FragmentPaintList fragment = new FragmentPaintList();
            fragment.fileName_ = String.format(url_, fileNameArray.get(i));
            adapter.addFragment(fragment);
        }
        view_pager.setAdapter(adapter);

        if(adapter.getCount() > 0)
            textView_quiz.setText(quizNameArray.get(0));

        for(int i = 0; i < adapter.getCount(); i++){
            ImageView iv = new ImageView(getContext());
            iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            if(i == 0){
                iv.setBackgroundResource(R.drawable.page_on);
            }else{
                iv.setBackgroundResource(R.drawable.page_off);
            }
            layout_circle.addView(iv);
        }

        view_pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }
            @Override
            public void onPageSelected(int position) {
                textView_quiz.setText(quizNameArray.get(position));
                for(int i = 0; i < layout_circle.getChildCount(); i++){
                    if(i == position){
                        layout_circle.getChildAt(i).setBackgroundResource(R.drawable.page_on);
                    }else{
                        layout_circle.getChildAt(i).setBackgroundResource(R.drawable.page_off);
                    }
                }
            }
            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        button_save.setOnClickListener(this);
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DebugHandler.log(getClass().getName(), "onDestroy");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button_save: //저장 버튼
                DebugHandler.log("Save Index", fileNameArray.get(view_pager.getCurrentItem()) + "");
                final String fileName = fileNameArray.get(view_pager.getCurrentItem());
                final String extStorageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();

                PostDB postDB = new PostDB(getContext(), "Download....");
                postDB.downloadFile(String.format(url_, fileName), extStorageDirectory, fileName, new OnFinishDBListener() {
                    @Override
                    public void onSuccess(String output) {
                        if(output.equals("success")) { //다운로드 성공
                            Toast.makeText(getContext(), "다운로드를 완료되었습니다.", Toast.LENGTH_LONG).show();
                            getContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                    Uri.parse("file://" + extStorageDirectory + "/" + fileName)));
                        }else{ //다운로드 실패
                            Toast.makeText(getContext(), "다운로드에 실패하였습니다.", Toast.LENGTH_LONG).show();
                        }
                    }
                });

//                Glide.with(this)
//                        .load(String.format(url_, fileName))    // you can pass url too
//                        .asBitmap()
//                        .into(new SimpleTarget<Bitmap>() {
//                            @Override
//                            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
//                                // you can do something with loaded bitmap here
//                                String extStorageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();//Environment.getExternalStorageDirectory().toString();
//
//                                File file = new File(extStorageDirectory, fileName);
//                                try {
//                                    OutputStream outStream = new FileOutputStream(file);
//                                    //image Save
//                                    resource.compress(Bitmap.CompressFormat.PNG, 100, outStream);
//                                    //Save Image Reload (Media Scanning)
//                                    getContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
//                                            Uri.parse("file://" + extStorageDirectory + "/" + fileName)));
//                                    outStream.flush();
//                                    outStream.close();
//
//                                    Toast.makeText(getContext(),
//                                            "저장하였습니다.", Toast.LENGTH_SHORT).show();
//
//                                } catch (FileNotFoundException e) {
//                                    e.printStackTrace();
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                        });

                break;
        }
    }
}
