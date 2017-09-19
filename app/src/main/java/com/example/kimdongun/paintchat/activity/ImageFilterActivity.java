package com.example.kimdongun.paintchat.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.example.kimdongun.paintchat.DebugHandler;
import com.example.kimdongun.paintchat.R;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

public class ImageFilterActivity extends AppCompatActivity implements View.OnClickListener {

    private Mat img_input; //원본 이미지
    private Bitmap bitmapOutput; //필터 적용한 비트맵

    private SeekBar seekBar; //필터 강도 조절
    private int nowFilterId; //현재 필터 아이디

    private String imagePath_; //불러 올 이미지 이름 (경로 포함)
    private ImageView imageView; //미리 보기 이미지 뷰
    private ImageView imageView_default; //기본 필터
    private ImageView imageView_gray; //흑백 필터
    private ImageView imageView_blur; //블러 필터
    private ImageView imageView_sketch; //스케치 필터
    private ImageView imageView_emboss; //엠보스 필터
    private ImageView imageView_watercolor; //수채화 필터

    private ImageProcessTask imageProcessTask;

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native void loadImage(String imageFileName, long img); //이미지 불러오기
    public native void imageFilterDefault(long inputImage, long outputImage); //기본 필터
    public native void imageFilterGray(long inputImage, long outputImage); //흑백 필터
    public native void imageFilterBlur(long inputImage, long outputImage, int filterScale); //블러 필터
    public native void imageFilterSketch(long inputImage, long outputImage, int filterScale); //스케치 필터
    public native void imageFilterEmboss(long inputImage, long outputImage, int filterScale); //엠보스 필터
    public native void imageFilterWaterColor(long inputImage, long outputImage); //수채화 필터
    public native void saveImage(String saveFilePath, String saveFileName, long maskImg); //이미지 저장

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_filter);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("이미지 필터");

        //초기화
        imageView = (ImageView)findViewById(R.id.imageView); //미리 보기 이미지 뷰
        imageView_default = (ImageView)findViewById(R.id.imageView_default); //기본 필터
        imageView_gray = (ImageView)findViewById(R.id.imageView_gray); //흑백 필터
        imageView_blur = (ImageView)findViewById(R.id.imageView_blur); //캐니 필터
        imageView_sketch = (ImageView)findViewById(R.id.imageView_sketch); //스케치 필터
        imageView_emboss = (ImageView)findViewById(R.id.imageView_emboss); //엠보스 필터
        imageView_watercolor = (ImageView)findViewById(R.id.imageView_watercolor); //수채화 필터
        seekBar = (SeekBar)findViewById(R.id.seekBar); //필터 강도 (1~20)
        seekBar.setVisibility(View.GONE);
        nowFilterId = R.id.imageView_default;

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //seekBar 게이지 0보다 무조건 크게
                if(progress <= 0)
                    progress = 1;
                seekBar.setProgress(progress);

                if(!imageProcessTask.isCancelled()){
                    imageProcessTask = new ImageProcessTask(ImageFilterActivity.this);
                    imageProcessTask.execute(nowFilterId, seekBar.getProgress());
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        Intent intent = getIntent();
        imagePath_ = intent.getStringExtra("imagePath");

        img_input = new Mat();

        //이미지 불러오기
        loadImage(imagePath_, img_input.getNativeObjAddr());
        imageProcessTask = new ImageProcessTask(this);
        imageProcessTask.execute(nowFilterId, seekBar.getProgress());

        //터치 이벤트
        imageView_default.setOnClickListener(this);
        imageView_gray.setOnClickListener(this);
        imageView_blur.setOnClickListener(this);
        imageView_sketch.setOnClickListener(this);
        imageView_emboss.setOnClickListener(this);
        imageView_watercolor.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_image_full, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home: //홈 버튼 클릭
                onBackPressed();
                break;

            case R.id.action_save: //필터 적용 버튼
//                File storage = getCacheDir(); // 어플 캐시 경로
//                String fileName = "profile.png";  // 파일이름
//                File cacheFile = new File(storage,fileName);
//                try{
//                    if(!cacheFile.exists())cacheFile.createNewFile();
//                    FileOutputStream out = new FileOutputStream(cacheFile);
//                    bitmapOutput.compress(Bitmap.CompressFormat.PNG, 100 , out);  // 넘거 받은 bitmap을 저장해줌
//                    out.close(); // 마무리로 닫아줍니다.
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }

                final String extStorageDirectory = getCacheDir().getAbsolutePath(); //저장 경로
                String saveFileName = "profile.png"; //저장 할 이름 (저장 경로 포함)
                saveFileName.trim();
                Mat img_output = new Mat();
                Utils.bitmapToMat(bitmapOutput, img_output);
                saveImage(extStorageDirectory, saveFileName, img_output.getNativeObjAddr());

                Intent returnIntent = getIntent();
                returnIntent.putExtra("filePath", extStorageDirectory + "/" + saveFileName);
                setResult(RESULT_OK, returnIntent);
                finish();

                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder alert_confirm = new AlertDialog.Builder(ImageFilterActivity.this);
        alert_confirm.setMessage("프로필 사진을 변경하지 않고 빠져 나가시겠습니까?").setCancelable(false).setPositiveButton("나가기",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //나가기
                        finish();
                    }
                }).setNegativeButton("취소",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                });
        AlertDialog alert = alert_confirm.create();
        alert.show();
    }

    @Override
    public void onClick(View v) {
        imageProcessTask = new ImageProcessTask(this);
        imageProcessTask.execute(v.getId(), seekBar.getProgress());
    }

    private class ImageProcessTask extends AsyncTask<Integer, Integer, Void> {
        private ProgressDialog progressDialog;
        private Context mContext;

        ImageProcessTask(Context context){
            this.mContext = context;
        }
        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(mContext);
            progressDialog.setCancelable(false);
            progressDialog.setMessage("필터 적용중...");
            progressDialog.show();
            super.onPreExecute();
        }//onPreExecute

        @Override
        protected void onPostExecute(Void aVoid) {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            imageView.setImageBitmap(bitmapOutput);
            DebugHandler.log("ImageFilter", "Done");
            super.onPostExecute(aVoid);
        }

        @Override
        protected Void doInBackground(Integer... integers) {
            nowFilterId = integers[0];
            publishProgress(nowFilterId);
            Mat img_output = new Mat();
            switch (nowFilterId){
                case R.id.imageView_default: //기본 필터
                    imageFilterDefault(img_input.getNativeObjAddr(), img_output.getNativeObjAddr());
                    break;

                case R.id.imageView_gray: //흑백 필터
                    imageFilterGray(img_input.getNativeObjAddr(), img_output.getNativeObjAddr());
                    break;

                case R.id.imageView_blur: //블러 필터
                    imageFilterBlur(img_input.getNativeObjAddr(), img_output.getNativeObjAddr(), integers[1] * 4);
                    break;

                case R.id.imageView_sketch: //스케치 필터
                    imageFilterSketch(img_input.getNativeObjAddr(), img_output.getNativeObjAddr(), integers[1] * 5);
                    break;

                case R.id.imageView_emboss: //엠보스 필터
                    imageFilterEmboss(img_input.getNativeObjAddr(), img_output.getNativeObjAddr(), integers[1] * 2);
                    break;

                case R.id.imageView_watercolor: //수채화 필터
                    imageFilterWaterColor(img_input.getNativeObjAddr(), img_output.getNativeObjAddr());
                    break;
            }

            bitmapOutput = Bitmap.createBitmap(img_output.cols(), img_output.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img_output, bitmapOutput);
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            switch (values[0]){
                case R.id.imageView_default: //기본 필터
                case R.id.imageView_watercolor: //수채화 필터
                case R.id.imageView_gray: //흑백 필터
                    seekBar.setVisibility(View.GONE);
                    break;

                case R.id.imageView_blur: //블러 필터
                case R.id.imageView_sketch: //스케치 필터
                case R.id.imageView_emboss: //엠보스 필터
                    seekBar.setVisibility(View.VISIBLE);
                    break;
            }
        }
    }
}
