package com.example.kimdongun.paintchat.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.bumptech.glide.Glide;
import com.example.kimdongun.paintchat.Client;
import com.example.kimdongun.paintchat.DebugHandler;
import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.adapter.MaskListViewAdapter;
import com.example.kimdongun.paintchat.server.OnFinishDBListener;
import com.example.kimdongun.paintchat.server.PostDB;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener, AdapterView.OnItemClickListener {
    public static int REQ_CODE_CUSTOM_CAMERA = 50;
    private CameraBridgeViewBase mOpenCvCameraView; //카메라 뷰
    private Mat matInput; //카메라 기본 화면 mat
    private Mat matResult; //카메라 커스텀 화면 mat

    private Mat maskImg; //얼굴 마스크 저장 mat
    private boolean isMask = true; //마스크 유무
    private long cascadeClassifier_face = 0; //얼굴 인식 파일
    private int cameraIndex = 0; // 0 - 후면 카메라 1- 전면 카메라
    private int cameraResize = 640; //카메라 얼굴인식할 때 리사이징 할 사이즈
    //카메라 콜백 리스너
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    private ImageView imageView_maskConfig; //마스크 설정 보이게 하는 버튼
    private LinearLayout layout_maskConfig; //마스크 설정 레이아웃
    private ImageView button_shoot; //촬영 버튼
    private ImageView button_index; //카메라 변경 (셀프 <-> 후면)
    private SeekBar seekBar1; //마스크 크기 조절 (좌우)
    private SeekBar seekBar2; //마스크 크기 조절 (상하)
    private SeekBar seekBar3; //마스크 위치 조절 (좌우)
    private SeekBar seekBar4; //마스크 위치 조절 (상하)

    private View footer_; //리스트뷰 푸터(커스텀 마스크 추가 공간)
    private ImageView imageView_maskChoice; //마스크 선택 보이게 하는 버튼
    private LinearLayout layout_maskChoice; //마스크 선택 레이아웃
    private ListView listView_mask; //마스크 리스트
    private MaskListViewAdapter maskListViewAdapter; //마스크 리스트 어댑터

    public Client client_; //계정 클라이언트

    private int defaultSizeXY = 3;
    private int defaultPosXY = 20;

    private boolean isShots_ = false; //촬영 버튼 눌렀는가? (눌렀으면 화면 갱신 안하게 막기 위함)

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }

    public static native long loadCascade( String cascadeFileName );
    public static native void detect(long cascadeClassifier_face, long matAddrInput, long matAddrResult, long maskImg,
                                     int maskSizeX, int maskSizeY, int maskPosX, int maskPosY, boolean isMask, int cameraResize);
    public native void loadImage(String maskFileName, long maskImg);
    public native void saveImage(String saveFilePath, String saveFileName, long saveImg);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);

        client_ = Client.getInstance(this);

        //카메라 뷰 초기화
        mOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE); //카메라뷰 visible 설정
        mOpenCvCameraView.setCvCameraViewListener(this); //카메라뷰 리스너 설정 (onCameraViewStarted, onCameraViewStopped, onCameraFrame) call back 함수
        mOpenCvCameraView.setCameraIndex(cameraIndex); // front-camera(1),  back-camera(0)
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        //뷰 초기화
        imageView_maskConfig = (ImageView) findViewById(R.id.imageView_maskConfig); //마스크 설정 보이게 하는 버튼
        layout_maskConfig = (LinearLayout) findViewById(R.id.layout_maskConfig); //마스크 설정 레이아웃
        button_shoot = (ImageView)findViewById(R.id.button_shoot);
        button_index = (ImageView)findViewById(R.id.button_index);
        seekBar1 = (SeekBar)findViewById(R.id.seekBar1);
        seekBar2 = (SeekBar)findViewById(R.id.seekBar2);
        seekBar3 = (SeekBar)findViewById(R.id.seekBar3);
        seekBar4 = (SeekBar)findViewById(R.id.seekBar4);

        imageView_maskChoice = (ImageView) findViewById(R.id.imageView_maskChoice); //마스크 선택 보이게 하는 버튼
        layout_maskChoice = (LinearLayout) findViewById(R.id.layout_maskChoice); //마스크 선택 레이아웃
        listView_mask = (ListView) findViewById(R.id.listView_mask); //마스크 리스트
        maskListViewAdapter = new MaskListViewAdapter(this, Glide.with(this)); //마스크 리스트 어댑터
        listView_mask.setAdapter(maskListViewAdapter);

        footer_ = getLayoutInflater().inflate(R.layout.item_mask_listview, null, false);
        ImageView imageView_mask_footer= (ImageView)footer_.findViewById(R.id.imageView_mask);
        imageView_mask_footer.setBackground(getResources().getDrawable(R.drawable.mask_add));
        //리스트뷰 푸터 설정
        listView_mask.addFooterView(footer_);

        HashMap<String, String> postData = new HashMap<String, String>();
        postData.put("id", client_.account_.id_);

        PostDB postDB = new PostDB();
        postDB.putFileData("load_mask.php", postData, null, null, new OnFinishDBListener() {
            @Override
            public void onSuccess(String output) {
                try {
                    Log.d(getClass().getName(), "output: " + output);
                    JSONObject reader = new JSONObject(output);
                    if(reader.get("mask").equals(null)){ //마스크 없는 경우

                    }else{ //마스크 있는 경우
                        //json값 변환
                        JSONArray socialArray = reader.getJSONArray("mask");
                        for (int i = 0; i < socialArray.length(); i++) {
                            JSONObject object = socialArray.getJSONObject(i);
                            String name = object.getString("name"); //마스크 이름
                            maskListViewAdapter.addItem(name);
                        }
                        maskListViewAdapter.notifyDataSetChanged();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        //터치 이벤트
        imageView_maskConfig.setOnClickListener(this);
        imageView_maskChoice.setOnClickListener(this);
        button_shoot.setOnClickListener(this);
        button_index.setOnClickListener(this);
        listView_mask.setOnItemClickListener(this);

        read_cascade_file(); //얼굴인식 파일 추가
        maskImg = new Mat();
        read_mask_file(0); //마스크 이미지 불러오기
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            DebugHandler.log(getClass().getName(), "onResume :: Internal OpenCV library not found.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            DebugHandler.log(getClass().getName(), "onResum :: OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();

        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //출력할 화면 mat 초기화
        if(!isShots_) { //촬영버튼 누르기 전엔 화면 갱신\
            matInput = inputFrame.rgba();
            if(cameraIndex == 1)
                Core.flip(matInput, matInput, 1);
            if (matResult != null) matResult.release();
            matResult = new Mat(matInput.rows(), matInput.cols(), matInput.type());

            //얼굴 인식해서 마스크 씌우기
            detect(cascadeClassifier_face, matInput.getNativeObjAddr(), matResult.getNativeObjAddr(), maskImg.getNativeObjAddr(),
                    seekBar1.getProgress() - defaultSizeXY, seekBar2.getProgress() - defaultSizeXY,
                    seekBar3.getProgress() - defaultPosXY, seekBar4.getProgress() - defaultPosXY, isMask, cameraResize);
        }else{
            cameraShot();
        }
        return matResult;
    }

    private String copyFileFromAssets(String filename) {
        File storage = getCacheDir(); // 어플 캐시 경로
        File cacheFile = new File(storage,filename);
        try{
            if(cacheFile.exists()){
                DebugHandler.log(getClass().getName(), "경로에 파일 존재: "+ cacheFile.getAbsolutePath());
                return cacheFile.getAbsolutePath();   // 임시파일 저장경로를 리턴해주면 끝!
            }
            DebugHandler.log(getClass().getName(), "다음 경로로 파일복사 "+ cacheFile.getAbsolutePath());
            cacheFile.createNewFile();
            AssetManager assetManager = this.getAssets();
            InputStream inputStream = assetManager.open(filename); //에셋 파일 열기
            OutputStream outputStream = new FileOutputStream(cacheFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            inputStream.close();
            inputStream = null;
            outputStream.flush();
            outputStream.close();
            outputStream = null;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cacheFile.getAbsolutePath();   // 임시파일 저장경로를 리턴해주면 끝!
    }

    private void copyFileFromServer(final String filename) {
        File storage = getCacheDir(); // 어플 캐시 경로
        File cacheFile = new File(storage,filename);
        if(cacheFile.exists()){ //파일이 이미 존재하면 마스크 다운로드 X
            DebugHandler.log(getClass().getName(), "경로에 파일 이미 존재: "+ cacheFile.getAbsolutePath());
            maskImg = new Mat();
            loadImage(cacheFile.getAbsolutePath(), maskImg.getNativeObjAddr());//마스크 이미지 불러오기
            return;
        }
        PostDB postDB = new PostDB(this, "Mask Loading...");
        postDB.downloadFile("http://211.110.229.53/mask_image/" + filename, getCacheDir().getAbsolutePath(), filename, new OnFinishDBListener() {
            @Override
            public void onSuccess(String output) {
                if(output.equals("success")) { //다운로드 성공
                    // you can do something with loaded bitmap here
                    File file = new File(getCacheDir(), filename);
                    if(file.exists()){
                        DebugHandler.log(getClass().getName(), "경로에 파일 다운 완료: "+ file.getAbsolutePath());
                        maskImg = new Mat();
                        loadImage(file.getAbsolutePath(), maskImg.getNativeObjAddr());//마스크 이미지 불러오기
                    }
                }else{ //다운로드 실패
                }
            }
        });
    }

    private void read_cascade_file(){
        cascadeClassifier_face = loadCascade(copyFileFromAssets("haarcascade_frontalface_alt.xml"));
    }

    private void read_mask_file(int index){
        if(index < maskListViewAdapter.defulatMask.length){ //기본 마스크 (에셋에서 파일 가지고 와서 캐시로 복사)
            maskImg = new Mat();
            String maskPath = copyFileFromAssets((String)maskListViewAdapter.getItem(index));
            loadImage(maskPath, maskImg.getNativeObjAddr());//마스크 이미지 불러오기
        }else{ //커스텀 마스크 (서버에서 파일 가지고 와서 캐시로 복사)
            copyFileFromServer((String)maskListViewAdapter.getItem(index));//마스크 이미지 불러오기
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.imageView_maskConfig: //마스크 설정 버튼
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)layout_maskConfig.getLayoutParams();
                if(params.height == 0){ //설정 창이 안 보이면 보이게
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                }else{ //설정 창이 보이면 안 보이게
                    params.height = 0;
                }
                layout_maskConfig.setLayoutParams(params);
                break;

            case R.id.imageView_maskChoice: //마스크 선택 버튼
                RelativeLayout.LayoutParams params2 = (RelativeLayout.LayoutParams)layout_maskChoice.getLayoutParams();
                if(params2.width == 0){ //설정 창이 안 보이면 보이게
                    params2.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                }else{ //설정 창이 보이면 안 보이게
                    params2.width = 0;
                }
                layout_maskChoice.setLayoutParams(params2);
                break;

            case R.id.button_shoot: //촬영 버튼
                isShots_ = true; //화면 갱신 막고 촬영 시작
                break;

            case R.id.button_index: //카메라 회전 버튼
                if(cameraIndex == 0) { //후면이면 전면으로
                    cameraResize = 320;
                    cameraIndex = 1;
                }else{ //전면이면 후면으로
                    cameraIndex = 0;
                    cameraResize = 640;
                }
                mOpenCvCameraView.disableView();
                mOpenCvCameraView.setCameraIndex(cameraIndex); // front-camera(1),  back-camera(0)
                mOpenCvCameraView.enableView();
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
        DebugHandler.log("ListIndex", index + "");
        if(index < maskListViewAdapter.getCount() ) {
            read_mask_file(index);
        }
        else{ //커스텀 마스크 추가
            Intent intent = new Intent(CameraActivity.this, CanvasActivity.class);
            intent.putExtra("from", "camera");
            startActivityForResult(intent, REQ_CODE_CUSTOM_CAMERA);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        DebugHandler.log(getClass().getName(), "requestCode: " + requestCode);
        if(resultCode== Activity.RESULT_OK)
        {
            if(requestCode == REQ_CODE_CUSTOM_CAMERA) {
                String imagePath = data.getStringExtra("filePath");
                uploadMask(imagePath);
            }
        }
    }

    private void uploadMask(final String imagePath){
        DebugHandler.log(getClass().getName(), "RealPath: " + imagePath);
        long now = System.currentTimeMillis();
        final String fileName = client_.account_.id_ + "_" + now + ".png";
        HashMap<String, String> postData = new HashMap<>();
        postData.put("filePath", "/usr/share/nginx/html/mask_image/");
        PostDB postDB = new PostDB(this);
        postDB.putFileData("upload_file.php", postData, imagePath, fileName, new OnFinishDBListener() {
            @Override
            public void onSuccess(String output) {
                DebugHandler.log("MaskUploadResult", output);
                if(output.equals("success")){ //파일 업로드 성공했으면 db에 저장
                    HashMap<String, String> postData = new HashMap<String, String>();
                    postData.put("id", client_.account_.id_);
                    postData.put("name", fileName);

                    PostDB postDB = new PostDB();
                    postDB.putFileData("save_mask.php", postData, null, null, new OnFinishDBListener() {
                        @Override
                        public void onSuccess(String output) {
                            maskListViewAdapter.addItem(fileName);
                            maskListViewAdapter.notifyDataSetChanged();
                            //캔버스 캐시 파일 삭제
                            if(new File(imagePath).exists())
                                new File(imagePath).delete();
                        }
                    });
                }
            }
        });
    }

    private void cameraShot(){
        long now = System.currentTimeMillis();
        final String extStorageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString(); //저장 경로
        String saveFileName = client_.account_.id_ + "_" + now + ".png"; //저장 할 이름 (저장 경로 포함)
        saveFileName.trim();
        saveImage(extStorageDirectory, saveFileName, matResult.getNativeObjAddr());
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.parse("file://" + extStorageDirectory + "/" + saveFileName)));

        Intent returnIntent = getIntent();
        returnIntent.putExtra("filePath", extStorageDirectory + "/" + saveFileName);
        setResult(Activity.RESULT_OK,returnIntent);
        finish();
    }
}
