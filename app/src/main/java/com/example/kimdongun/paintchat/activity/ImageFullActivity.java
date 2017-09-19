package com.example.kimdongun.paintchat.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.server.OnFinishDBListener;
import com.example.kimdongun.paintchat.server.PostDB;

import java.io.File;

public class ImageFullActivity extends AppCompatActivity {
    private ImageView imageView; //이미지 프리뷰
    private String imageUrl; //이미지 Uri
    private String title; //액티비티 이름
    private String fileName_; //이미지 파일 이름
    private String extStorageDirectory_; //저장소

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_full);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        imageView = (ImageView)findViewById(R.id.imageView);

        Intent intent = getIntent();
        imageUrl = intent.getStringExtra("url");
        title = intent.getStringExtra("title");
        setTitle(title);

        String[] file = imageUrl.split("/");
        fileName_ = file[file.length-1];
        extStorageDirectory_ = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();

        Glide.with(this)
                .load(imageUrl)
                .thumbnail(0.1f)
                .placeholder(R.drawable.prepare_image)
                .into(imageView);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home: //홈 버튼 클릭
                onBackPressed();
                break;

            case R.id.action_save: //사진 저장 버튼
                if(new File(extStorageDirectory_, fileName_).exists()){
                    Toast.makeText(this, "이미 해당 사진을 다운 받았습니다.", Toast.LENGTH_SHORT).show();
                }else {
                    PostDB postDB = new PostDB(this, "Download....");
                    postDB.downloadFile(imageUrl, extStorageDirectory_, fileName_.trim(), new OnFinishDBListener() {
                        @Override
                        public void onSuccess(String output) {
                            if (output.equals("success")) { //다운로드 성공
                                Toast.makeText(ImageFullActivity.this, "다운로드가 완료되었습니다.", Toast.LENGTH_LONG).show();
                                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                        Uri.parse("file://" + extStorageDirectory_ + "/" + fileName_)));
                            } else { //다운로드 실패
                                Toast.makeText(ImageFullActivity.this, "다운로드에 실패하였습니다.", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_image_full, menu);
        return super.onCreateOptionsMenu(menu);
    }
}
