package com.example.kimdongun.paintchat.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.kimdongun.paintchat.Account;
import com.example.kimdongun.paintchat.CameraPreview;
import com.example.kimdongun.paintchat.Client;
import com.example.kimdongun.paintchat.DebugHandler;
import com.example.kimdongun.paintchat.HandlerMessageDecode;
import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.SQLiteHandler;
import com.example.kimdongun.paintchat.SensorARInventory;
import com.example.kimdongun.paintchat.item.ChatRoomListViewItem;
import com.example.kimdongun.paintchat.item.MaskItem;
import com.example.kimdongun.paintchat.server.OnFinishDBListener;
import com.example.kimdongun.paintchat.server.PostDB;
import com.example.kimdongun.paintchat.service.SocketService;
import com.example.kimdongun.paintchat.service.SocketServiceExecutor;
import com.example.kimdongun.paintchat.toast.NormalChatToast;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class ARInventoryActivity extends AppCompatActivity implements View.OnClickListener, HandlerMessageDecode, SocketServiceExecutor {
    private SensorARInventory sensorARInventory;
    private CameraPreview cameraPreview;
    private Camera camera;
    private RelativeLayout layout; //부모 레이아웃
    private ImageView imageView_myMask; //내 마스크 보기 버튼
    private boolean isVisibleMask;

    private LinearLayout layout_maskInfo; //마스크 정보 레이아웃
    private ImageView imageView_touchMask; //마스크 이미지
    private TextView textView_maskName; //마스크 이름
    private TextView textView_OldID; //마스크 원래 주인
    private TextView textView_Memo; //마스크 메모 수정 불가
    private LinearLayout layout_memo; //수정 가능 메모 레아이웃
    private EditText editText_Memo; //마스크 메모 수정 가능
    private Button button_ok; //수정 확인
    private TextView textView_Date; //마스크 습득 날짜
    private Button button_throw; //마스크 버리기

    private ImageView imageView_map; //맵으로 돌아가기

    private LatLng latlng; //사용자 위치
    public LocationManager locationManager; //위치 수신하기 위함
    public LocationListener mLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            //여기서 위치값이 갱신되면 이벤트가 발생한다.
            //값은 Location 형태로 리턴되며 좌표 출력 방법은 다음과 같다.
            Log.d("LOCATION", "onLocationChanged, location:" + location);
            latlng = new LatLng(location.getLatitude(), location.getLongitude()); //위도, 경도
        }

        public void onProviderDisabled(String provider) {
            // Disabled시
            Log.d("test", "onProviderDisabled, provider:" + provider);
        }

        public void onProviderEnabled(String provider) {
            // Enabled시
            Log.d("test", "onProviderEnabled, provider:" + provider);
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            // 변경시
            Log.d("test", "onStatusChanged, provider:" + provider + ", status:" + status + " ,Bundle:" + extras);
        }
    };

    // Camera.CameraInfo.CAMERA_FACING_FRONT or Camera.CameraInfo.CAMERA_FACING_BACK
    private final int cameraIndex = 0; // 0 - 후면 카메라 1- 전면 카메라

    private final String[] defulatMask = {"spiderman.png", "ironman.png", "betman.png", "jigsaw.png"};

    public Client client_; //계정 클라이언트
    private Handler handler_; //소켓 스레드랑 통신 할 핸들러
    public SocketService socketService_; //소켓 서비스
    private ServiceConnection serviceConnection_; //서비스 커넥션
    public boolean isLiveBinder; //서비스 바인드 연결 유무

    private NormalChatToast normalChatToast; //채팅 토스트 메세지
    private Display mDisplay; //디스플레이 정보
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //상태바 없애기
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_ar_inventory);
        handler_ = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                String str = msg.obj.toString();
                if(str.equals("connectSuccess")){ //소켓 연결 성공
                    Toast.makeText(ARInventoryActivity.this, "소켓 서버와 연결하였습니다.", Toast.LENGTH_SHORT).show();
                }else if(str.equals("connectFail")){ //소켓 연결 실패
                    Toast.makeText(ARInventoryActivity.this, "소켓 서버와 연결을 실패하였습니다.", Toast.LENGTH_SHORT).show();
                }else {
                    doReceiveAction(str);
                }
            }
        };

        serviceConnection_ = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                try{
                    DebugHandler.log(getClass().getName(), "Service Connected");
                    SocketService.ServiceBinder myBinder_ = (SocketService.ServiceBinder) service;
                    socketService_ = myBinder_.getService();
                    socketService_.setHandler(handler_);
                    isLiveBinder = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onServiceDisconnected(ComponentName name) {
                isLiveBinder = false;
            }
        };

        Intent serviceIntent = new Intent(ARInventoryActivity.this, SocketService.class);
        bindService(serviceIntent, serviceConnection_, Context.BIND_AUTO_CREATE); //intent객체, 서비스와 연결에 대한 정의

        client_ = Client.getInstance(this);

        layout = (RelativeLayout)findViewById(R.id.layout);
        imageView_myMask = (ImageView)findViewById(R.id.imageView_myMask);
        isVisibleMask = false;

        layout_maskInfo = (LinearLayout)findViewById(R.id.layout_maskInfo); //마스크 정보 레이아웃
        imageView_touchMask = (ImageView)findViewById(R.id.imageView_touchMask); //마스크 이미지
        textView_maskName = (TextView)findViewById(R.id.textView_maskName); //마스크 이름
        textView_OldID = (TextView)findViewById(R.id.textView_OldID); //마스크 원래 주인
        textView_Memo = (TextView)findViewById(R.id.textView_Memo); //마스크 메모 수정 불가능
        layout_memo = (LinearLayout)findViewById(R.id.layout_memo); //수정 가능 메모 레이아웃
        editText_Memo = (EditText)findViewById(R.id.editText_Memo); //마스크 메모 수정 가능
        button_ok = (Button)findViewById(R.id.button_ok); //수정 확인
        textView_Date = (TextView)findViewById(R.id.textView_Date); //마스크 습득 날짜
        button_throw = (Button)findViewById(R.id.button_throw); //마스크 버리기

        imageView_map = (ImageView)findViewById(R.id.imageView_map); //맵으로 돌아가기

        layout_maskInfo.setVisibility(View.INVISIBLE); //처음에는 마스크 정보 안보이게

        sensorARInventory = new SensorARInventory(this, isVisibleMask);

        //화면 크기
        WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        mDisplay = wm.getDefaultDisplay();

        //가지고 있는 마스크 불러오기
        HashMap<String, String> postData = new HashMap<String, String>();
        postData.put("id", client_.account_.id_);

        PostDB postDB = new PostDB();
        postDB.putFileData("load_mask.php", postData, null, null, new OnFinishDBListener() {
            @Override
            public void onSuccess(String output) {
                try {
                    //기본 마스크 추가
                    for (int i = 0; i < defulatMask.length; i++) {
                        ImageView imageView = new ImageView(ARInventoryActivity.this); //이미지 뷰 생성
                        if(isVisibleMask)
                            imageView.setVisibility(View.VISIBLE);
                        else
                            imageView.setVisibility(View.INVISIBLE);
                        layout.addView(imageView); //이미지 뷰 레이아웃에 추가
                        Glide.with(ARInventoryActivity.this).load("http://211.110.229.53/mask_image/" + defulatMask[i]).into(imageView);
                        @IdRes int id1 = i;
                        imageView.setId(id1); //아이디 설정
                        float rate1 = imageView.getLayoutParams().height/imageView.getLayoutParams().width; //비율 계산
                        imageView.getLayoutParams().width = mDisplay.getWidth()/3; //넓이 설정
                        imageView.getLayoutParams().height = (int)(imageView.getLayoutParams().width * rate1); //높이 설정 (비율 이용)
                        imageView.setX(mDisplay.getWidth()*1.5f);
                        imageView.setY(mDisplay.getHeight()*1.5f);
                        imageView.setBackground(getResources().getDrawable(R.drawable.mask_shape));

                        sensorARInventory.maskManager.putMask(id1, new MaskItem(defulatMask[i], "Admin", "기본 마스크", "--", imageView));
                        imageView.setOnClickListener(ARInventoryActivity.this); //터치 이벤트 설정
                    }
                    Log.d(getClass().getName(), "output: " + output);
                    JSONObject reader = new JSONObject(output);
                    if(reader.get("mask").equals(null)){ //마스크 없는 경우

                    }else{ //마스크 있는 경우
                        //json값 변환
                        JSONArray socialArray = reader.getJSONArray("mask");
                        for (int i = 0; i < socialArray.length(); i++) { //커스텀 마스크 추가
                            JSONObject object = socialArray.getJSONObject(i);
                            String name = object.getString("name"); //마스크 이름
                            String oldID = object.getString("old_id"); //마스크 마스크 원래 주인
                            String memo = object.getString("memo"); //마스크 마스크 메모
                            String date = object.getString("date"); //마스크 습득 날짜

                            ImageView imageView = new ImageView(ARInventoryActivity.this); //이미지 뷰 생성
                            if(isVisibleMask)
                                imageView.setVisibility(View.VISIBLE);
                            else
                                imageView.setVisibility(View.INVISIBLE);
                            layout.addView(imageView); //이미지 뷰 레이아웃에 추가
                            Glide.with(ARInventoryActivity.this).load("http://211.110.229.53/mask_image/" + name).into(imageView);
                            @IdRes int id1 = i+defulatMask.length;
                            imageView.setId(id1); //아이디 설정
                            float rate1 = imageView.getLayoutParams().height/imageView.getLayoutParams().width; //비율 계산
                            imageView.getLayoutParams().width = mDisplay.getWidth()/3; //넓이 설정
                            imageView.getLayoutParams().height = (int)(imageView.getLayoutParams().width * rate1); //높이 설정 (비율 이용)
                            imageView.setX(mDisplay.getWidth()*1.5f);
                            imageView.setY(mDisplay.getHeight()*1.5f);
                            imageView.setBackground(getResources().getDrawable(R.drawable.mask_shape));

                            sensorARInventory.maskManager.putMask(id1, new MaskItem(name, oldID, memo, date, imageView));
                            imageView.setOnClickListener(ARInventoryActivity.this); //터치 이벤트 설정
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, // 등록할 위치제공자
                100, // 통지사이의 최소 시간간격 (miliSecond)
                1, // 통지사이의 최소 변경거리 (m)
                mLocationListener);
//        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, // 등록할 위치제공자
//                100, // 통지사이의 최소 시간간격 (miliSecond)
//                1, // 통지사이의 최소 변경거리 (m)
//                mLocationListener);

        normalChatToast = new NormalChatToast(this); //채팅 토스트 메세지

        //터치 이벤트
        imageView_myMask.setOnClickListener(this);
        imageView_touchMask.setOnClickListener(this);
        button_ok.setOnClickListener(this);
        button_throw.setOnClickListener(this);
        imageView_map.setOnClickListener(this);
    }
    @Override
    protected void onStart() {
        super.onStart();
        if(sensorARInventory != null)
            sensorARInventory.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCamera();
        //카메라 시야각 초기화
        sensorARInventory.cameraHorizontalAngle = cameraPreview.horizontalAngle;
        sensorARInventory.cameraVerticalAngle = cameraPreview.verticalAngle;
        DebugHandler.log("CameraAngle", "H: " + sensorARInventory.cameraHorizontalAngle + "     V: " + sensorARInventory.cameraVerticalAngle);
        if(sensorARInventory != null)
            sensorARInventory.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(sensorARInventory != null)
            sensorARInventory.stop();

        // Surface will be destroyed when we return, so stop the preview.
        if(camera != null) {
            // Call stopPreview() to stop updating the preview surface
            camera.stopPreview();
            cameraPreview.setCamera(null);
            camera.release();
            camera = null;
        }

        ((RelativeLayout) findViewById(R.id.layout)).removeView(cameraPreview);
        cameraPreview = null;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(sensorARInventory != null)
            sensorARInventory.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(locationManager != null)
            locationManager.removeUpdates(mLocationListener);  //  미수신할때는 반드시 자원해체를 해주어야 한다.

        if(isLiveBinder){ //서비스랑 바인드 된 경우
            unbindService(serviceConnection_);
        }

        //사용 한 객체 초기화
        sensorARInventory.maskManager.destroyManager();

        sensorARInventory = null;
        cameraPreview = null;
        camera = null;
        layout = null; //부모 레이아웃
        imageView_myMask = null; //내 마스크 보기 버튼

        layout_maskInfo = null; //마스크 정보 레이아웃
        imageView_touchMask = null; //마스크 이미지
        textView_maskName = null; //마스크 이름
        textView_OldID = null; //마스크 원래 주인
        textView_Memo = null; //마스크 메모 수정 불가
        layout_memo = null; //수정 가능 메모 레아이웃
        editText_Memo = null; //마스크 메모 수정 가능
        button_ok = null; //수정 확인
        textView_Date = null; //마스크 습득 날짜
        button_throw = null; //마스크 버리기

        imageView_map = null; //맵으로 돌아가기

        latlng = null; //사용자 위치
        locationManager = null; //위치 수신하기 위함
        mLocationListener = null;

        normalChatToast = null; //채팅 토스트 메세지
        mDisplay = null; //디스플레이 정보

        handler_ = null; //소켓 스레드랑 통신 할 핸들러
        socketService_ = null; //소켓 서비스
        serviceConnection_ = null; //서비스 커넥션
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(ARInventoryActivity.this, MapsActivity.class);
        startActivity(intent);
        finish();
    }

    //카메라 초기화
    public void startCamera() {
        if ( cameraPreview == null ) {
            cameraPreview = new CameraPreview(this, (SurfaceView) findViewById(R.id.cameraPreview));
            cameraPreview.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            ((RelativeLayout) findViewById(R.id.layout)).addView(cameraPreview);
            cameraPreview.setKeepScreenOn(true);
        }

        cameraPreview.setCamera(null);
        if (camera != null) {
            camera.release();
            camera = null;
        }

        int numCams = Camera.getNumberOfCameras();
        if (numCams > 0) {
            try {
                camera = Camera.open(cameraIndex);
                // camera orientation
                camera.setDisplayOrientation(setCameraDisplayOrientation(this, cameraIndex, camera));
                // get Camera parameters
                Camera.Parameters params = camera.getParameters();
                // picture image orientation
                params.setRotation(setCameraDisplayOrientation(this, cameraIndex, camera));
                camera.startPreview();

            } catch (RuntimeException ex) {
                DebugHandler.logE(getClass().getName(), "camera_not_found " + ex.getMessage().toString());
            }
        }

        cameraPreview.setCamera(camera);
//        sensorAR.imageView.getLayoutParams().width = 30;
//        sensorAR.imageView.getLayoutParams().height = 30;
    }

    /**
     *
     * @param activity
     * @param cameraId  Camera.CameraInfo.CAMERA_FACING_FRONT,
     *                    Camera.CameraInfo.CAMERA_FACING_BACK
     * @param camera
     *
     * Camera Orientation
     * reference by https://developer.android.com/reference/android/hardware/Camera.html
     */
    public int setCameraDisplayOrientation(Activity activity,
                                                  int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;
    }

    private int touchID = 0; //터치한 마스크 아이디
    @Override
    public void onClick(View v) {
        MaskItem mask = sensorARInventory.maskManager.getMask(v.getId());
        if(mask != null) { //마스크 터치
            Toast.makeText(this, "ID: " + v.getId() + "  Name: " + mask.maskName + "  OldID: " + mask.maskOldID +
                    "  Memo: " + mask.maskMemo + "  Date: " + mask.maskDate + " 터치 됨 ", Toast.LENGTH_SHORT).show();
            sensorARInventory.maskManager.setMaskTouch(true);
            Glide.with(ARInventoryActivity.this).load("http://211.110.229.53/mask_image/" + mask.maskName).into(imageView_touchMask);
            float rate = imageView_touchMask.getLayoutParams().height/imageView_touchMask.getLayoutParams().width; //비율 계산
            imageView_touchMask.getLayoutParams().width = mDisplay.getWidth()*2/3; //넓이 설정
            imageView_touchMask.getLayoutParams().height = (int)(imageView_touchMask.getLayoutParams().width * rate); //높이 설정 (비율 이용)
            textView_maskName.setText(mask.maskName); //마스크 이름
            textView_OldID.setText(mask.maskOldID); //마스크 원래 주인
            textView_Memo.setText(mask.maskMemo); //마스크 메모 수정 불가능
            editText_Memo.setText(mask.maskMemo); //마스크 메모 수정 가능
            if(client_.account_.id_.equals(mask.maskOldID)){ //마스크 메모 수정 가능
                textView_Memo.setVisibility(View.INVISIBLE);
                layout_memo.setVisibility(View.VISIBLE);
            }else{ //수정 불가능
                textView_Memo.setVisibility(View.VISIBLE);
                layout_memo.setVisibility(View.INVISIBLE);

            }
            textView_Date.setText(mask.maskDate); //마스크 습득 날짜
            if(v.getId() < defulatMask.length) //기본 마스크
                button_throw.setVisibility(View.GONE);
            else
                button_throw.setVisibility(View.VISIBLE);
            layout_maskInfo.setVisibility(View.VISIBLE);
            touchID = v.getId();
        }else{
            switch (v.getId()){
                case R.id.imageView_myMask: //내 마스크 보이기
                    isVisibleMask = !isVisibleMask;
                    sensorARInventory.setVisibleMyMask(isVisibleMask);
                    if(!isVisibleMask){ //마스크 안보이게 하면 마스크 정보 창도 안 보이게 설정
                        sensorARInventory.maskManager.setMaskTouch(false);
                        layout_maskInfo.setVisibility(View.INVISIBLE);
                        hideSoftKeyboard();
                        imageView_myMask.setBackground(getResources().getDrawable(R.drawable.mask_btn)); //마스크 안 보고 있는 이미지
                    }else{
                        imageView_myMask.setBackground(getResources().getDrawable(R.drawable.mask_btn_on)); //마스크 보고 있는 이미지
                    }
                    break;

                case R.id.imageView_touchMask: //마스크 정보에서 띄워지는 이미지
                    sensorARInventory.maskManager.setMaskTouch(false);
                    layout_maskInfo.setVisibility(View.INVISIBLE);
                    hideSoftKeyboard();
                    break;

                case R.id.button_ok:
                    final String str = editText_Memo.getText().toString();
                    if(str.length() < 1){
                        Toast.makeText(this, "메모를 입력하세요", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    HashMap<String, String> postData = new HashMap<String, String>();
                    postData.put("id", String.valueOf(client_.account_.id_));
                    postData.put("name", textView_maskName.getText().toString().trim());
                    postData.put("memo", str.trim());

                    PostDB postDB = new PostDB();
                    postDB.putFileData("update_mask.php", postData, null, null, new OnFinishDBListener() {
                        @Override
                        public void onSuccess(String output) {
                            Toast.makeText(ARInventoryActivity.this, "메모 수정을 완료했습니다.", Toast.LENGTH_SHORT).show();
                            sensorARInventory.maskManager.getMask(touchID).maskMemo = str;
                            hideSoftKeyboard();
                        }
                    });
                    break;

                case R.id.button_throw: //마스크 버리기
                    if(latlng == null){
                        Toast.makeText(this, "GPS 정보를 받고 있습니다.\n잠시후 다시 시도하세요.", Toast.LENGTH_SHORT).show();
                    }else {
                        AlertDialog.Builder alert_confirm = new AlertDialog.Builder(this);
                        alert_confirm.setMessage("마스크를 버리시겠습니까?").setCancelable(false).setPositiveButton("확인",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        HashMap<String, String> postData = new HashMap<String, String>();
                                        postData.put("id", String.valueOf(client_.account_.id_));
                                        postData.put("name", textView_maskName.getText().toString().trim());
                                        postData.put("lat", String.valueOf(latlng.latitude));
                                        postData.put("lng", String.valueOf(latlng.longitude));

                                        PostDB postDB = new PostDB();
                                        postDB.putFileData("throw_mask.php", postData, null, null, new OnFinishDBListener() {
                                            @Override
                                            public void onSuccess(String output) {
                                                Toast.makeText(ARInventoryActivity.this, "마스크를 버렸습니다.", Toast.LENGTH_SHORT).show();
                                                sensorARInventory.maskManager.removeMask(touchID);
                                                sensorARInventory.maskManager.setMaskTouch(false);
                                                layout_maskInfo.setVisibility(View.INVISIBLE);
                                                hideSoftKeyboard();
                                            }
                                        });
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
                    break;

                case R.id.imageView_map: //맵으로 돌아가기
                    onBackPressed();
                    break;
            }
        }
    }

    private void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText_Memo.getWindowToken(), 0);
    }

    @Override
    public void doReceiveAction(String request) {
        DebugHandler.log(getClass().getName() + " JSON", request);
        ArrayList<Object> arrayList = new ArrayList<Object>();
        try {
            JSONObject json = new JSONObject(request);
            String cmdStr = (String)json.get("cmd"); //명령 커맨드 값
            Object requestJson = json.get("request"); //명령 디테일 값

            arrayList.add(cmdStr); //명령 커맨드
            arrayList.add(requestJson); //명령 디테일
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String cmd = (String)arrayList.get(0); //커맨드 값
        Object requestObj = arrayList.get(1); //명령 디테일 값

        switch (cmd){
            case "normalChat": //채팅 행동
                receiveNormalChat(requestObj);
                break;

            case "readChat": //채팅 읽음 처리 행동
                receiveReadChat(requestObj);
                break;

            case "exitServerResult": //서버 접속 종료 행동
                receiveExitServerResult();
                break;
        }
    }

    @Override
    public void receiveNormalChat(Object jsonObj) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        try {
            JSONObject json = (JSONObject)jsonObj;
            map.put("roomKey", json.get("roomKey")); //채팅방 키값
            map.put("id", json.get("id")); //채팅 메세지 보낸 유저 아이디
            map.put("nick", json.get("nick")); //채팅 메세지 보낸 유저 닉네임
            map.put("msg", json.get("msg")); //채팅 메세지
            map.put("date", json.get("date")); //채팅 메세지 보낸 시간
            map.put("num", json.get("num")); //채팅 메세지 읽음 수
            map.put("name", json.get("name")); //채팅 방 이름
            map.put("type", json.get("type")); //타입  ex)chat / image / video / invite
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Date date = new Date((long)map.get("date"));
        SimpleDateFormat sdfTime = new SimpleDateFormat("aa hh:mm");
        String strTime = sdfTime.format(date);

        ChatRoomListViewItem chatRoomListViewItem = client_.chatRoomListViewAdapter.getItem((String)map.get("roomKey"));
        if(chatRoomListViewItem == null){ //받은 메세지의 방 키값을 가진 방이 없는 경우 (채팅방 리스트에 해당 채팅 방 추가)
            //SQLite에서 받은 채팅 메세지가 들어가야 되는 채팅 방 정보 가져옴
            SQLiteHandler sqLiteHandler = new SQLiteHandler(this, "project_one", null, 1);
            String[] chatRoomColumns = {"my_id", "room_key"};
            Object[] chatRoomValues = {client_.account_.id_, map.get("roomKey")};
            ArrayList<ArrayList<Object>> chatRoomArray = sqLiteHandler.select("chat_room", chatRoomColumns, chatRoomValues, "and");

            int user_num = (int)map.get("num"); //채팅방있는 유저 수
            user_num += 1; //본인 추가

            String[] accountIdkArray = ((String)chatRoomArray.get(0).get(2)).split(",");//((String)map.get("name")).split(",");

            //방 리스트에 뜨는 친구 닉네임 설정 (방 제목에서 자신의 닉네임은 안 보이도록)
            String newRoomName = "";
            //프로필 사진 url가져오기
            final ArrayList<String> profileList = new ArrayList<>();
            for(int ii = 0; ii < accountIdkArray.length; ii++){
                Account account = client_.socialListViewAdapter.getItemById(accountIdkArray[ii]);
                if(account == null){
                    account = client_.recommendSocialListViewAdapter.getItemById(accountIdkArray[ii]);
                }
                if(account == null){
                    account = new Account(accountIdkArray[ii], "http://211.110.229.53/profile_image/default.png", null, "???",
                            null, 1, 1, 1);
                }
                profileList.add(account.profileUrl_);
                newRoomName += account.nick_;
                if (ii < accountIdkArray.length - 1)
                    newRoomName += ",";
            }

            chatRoomListViewItem = new ChatRoomListViewItem((String)map.get("roomKey"), profileList, newRoomName, (int)user_num,
                    (String)map.get("msg"), strTime, 1);

        }else{ //받은 메세지의 방 키값을 가진 방이 있는 경우 (채팅방 리스트에 있는 아이템 수정)
            //채팅방 리스트뷰에 보이는 정보 수정 (마지막 메세지, 날짜, 안 읽은 메세지 수)
            chatRoomListViewItem.msgNum_ += 1;
            chatRoomListViewItem.msg_ = (String)map.get("msg");
            chatRoomListViewItem.time_ = strTime;
            client_.chatRoomListViewAdapter.removeItem(chatRoomListViewItem.roomKey_);
        }
        chatRoomListViewItem.type_ = (String)map.get("type");
        client_.chatRoomListViewAdapter.addTopItem(chatRoomListViewItem);
        client_.chatRoomListViewAdapter.notifyDataSetChanged();

        Account account = client_.socialListViewAdapter.getItemById((String)map.get("id"));
        if(account == null){
            account = client_.recommendSocialListViewAdapter.getItemById((String)map.get("id"));
        }
        if(account == null){
            account = new Account((String)map.get("id"), "http://211.110.229.53/profile_image/default.png", null, (String)map.get("nick"),
                    null, 1, 1, 1);
        }

        normalChatToast.showToast(account.profileUrl_, account.nick_, (String)map.get("msg"), (String)map.get("type"));
    }

    @Override
    public void receiveReadChat(Object jsonObj) {

    }

    @Override
    public void receiveExitServerResult() {

    }
}
