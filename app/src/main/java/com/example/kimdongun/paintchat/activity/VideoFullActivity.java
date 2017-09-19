package com.example.kimdongun.paintchat.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.server.OnFinishDBListener;
import com.example.kimdongun.paintchat.server.PostDB;

import java.io.File;

public class VideoFullActivity extends AppCompatActivity implements View.OnClickListener {
    private VideoView myVideo; //비디오 뷰
    private ImageView imageView; //비디오 썸네일 이미지
    private ImageView imageView_play; //비디오 재생 이미지
    private String videoUrl; //비디오 Uri
    private String fileName_; //비디오 파일 이름
    private String extStorageDirectory_; //저장소

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_full);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        myVideo = (VideoView)findViewById(R.id.myVideo);
        imageView = (ImageView)findViewById(R.id.imageView);
        imageView_play = (ImageView)findViewById(R.id.imageView_play);
        MediaController videoController = new MediaController(this); //비디오 컨트롤러

        Intent intent = getIntent();
        videoUrl = intent.getStringExtra("url");
        setTitle("비디오");

        String[] file = videoUrl.split("/");
        fileName_ = file[file.length-1];
        extStorageDirectory_ = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();

        myVideo.setVisibility(View.GONE); //비디오 뷰 초기에 안 보이게
        imageView.setVisibility(View.VISIBLE); //썸네일이랑 재생 이미지만 보이게
        imageView_play.setVisibility(View.VISIBLE);
        Glide.with(VideoFullActivity.this) //썸네일 이미지 초기화
                .load(videoUrl + "Thumb.png")
                .thumbnail(0.1f)
                .placeholder(R.drawable.prepare_image)
                .into(imageView);

        videoController.setAnchorView(myVideo);
        myVideo.setMediaController(videoController);

        //터치 이벤트
        imageView.setOnClickListener(this);
        imageView_play.setOnClickListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home: //홈 버튼 클릭
                onBackPressed();
                break;

            case R.id.action_save: //비디오 저장 버튼
                downloadVideo();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_image_full, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.imageView_play: //재생 버튼
            case R.id.imageView: //썸네일 이미지
                if(new File(extStorageDirectory_, fileName_).exists()){ //동영상 파일을 이미 다운 받은 경우
                    myVideo.setVisibility(View.VISIBLE);
                    imageView.setVisibility(View.GONE);
                    imageView_play.setVisibility(View.GONE);
                    myVideo.setVideoPath(extStorageDirectory_ + "/" + fileName_);
                    myVideo.start();
                }else{
                    AlertDialog.Builder alert_confirm = new AlertDialog.Builder(VideoFullActivity.this);
                    alert_confirm.setMessage("해당 단말기에 파일이 존재하지 않습니다.\n데이터를 사용해서 동영상을 재생하겠습니까?").setCancelable(false).setPositiveButton("재생",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) { //데이터를 사용해서 동영상 재생
                                    myVideo.setVisibility(View.VISIBLE);
                                    imageView.setVisibility(View.GONE);
                                    imageView_play.setVisibility(View.GONE);
                                    Uri uri = Uri.parse(videoUrl);
                                    myVideo.setVideoURI(uri);
                                    myVideo.start();
                                }
                            }).setNegativeButton("취소",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
//                                    myVideo.setVisibility(View.GONE);
//                                    imageView.setVisibility(View.VISIBLE);
//                                    imageView_play.setVisibility(View.VISIBLE);
                                    // 'No'
                                    return;
                                }
                            });
                    AlertDialog alert = alert_confirm.create();
                    alert.show();
                }
//                downloadVideo();
                break;
        }
    }

    private void downloadVideo(){
        if(new File(extStorageDirectory_, fileName_).exists()){
            Toast.makeText(this, "이미 해당 동영상을 다운받았습니다.", Toast.LENGTH_SHORT).show();
        }else {
            AlertDialog.Builder alert_confirm = new AlertDialog.Builder(VideoFullActivity.this);
            alert_confirm.setMessage("동영상을 다운로드 하겠습니까?").setCancelable(false).setPositiveButton("확인",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            myVideo.pause();

                            PostDB postDB = new PostDB(VideoFullActivity.this, "Download....");
                            postDB.downloadFile(videoUrl, extStorageDirectory_, fileName_.trim(), new OnFinishDBListener() {
                                @Override
                                public void onSuccess(String output) {
                                    if (output.equals("success")) { //다운로드 성공
                                        Toast.makeText(VideoFullActivity.this, "다운로드가 완료되었습니다.", Toast.LENGTH_LONG).show();
                                        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                                Uri.parse("file://" + extStorageDirectory_ + "/" + fileName_)));

                                        myVideo.setVisibility(View.VISIBLE);
                                        imageView.setVisibility(View.GONE);
                                        imageView_play.setVisibility(View.GONE);
                                        myVideo.setVideoPath(extStorageDirectory_ + "/" + fileName_);
                                        myVideo.start();
                                    } else { //다운로드 실패
                                        Toast.makeText(VideoFullActivity.this, "다운로드에 실패하였습니다.", Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                        }
                    }).setNegativeButton("취소",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // 'No'
                            return;
                        }
                    });
            AlertDialog alert = alert_confirm.create();
            alert.show();
        }
    }
}
