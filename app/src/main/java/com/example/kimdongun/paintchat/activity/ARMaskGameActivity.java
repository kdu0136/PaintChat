package com.example.kimdongun.paintchat.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.ColorInt;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
import com.example.kimdongun.paintchat.MaskGameCanvas;
import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.SQLiteHandler;
import com.example.kimdongun.paintchat.SensorARInventory;
import com.example.kimdongun.paintchat.item.CanvasRect;
import com.example.kimdongun.paintchat.item.ChatRoomListViewItem;
import com.example.kimdongun.paintchat.item.MaskItem;
import com.example.kimdongun.paintchat.server.OnFinishDBListener;
import com.example.kimdongun.paintchat.server.PostDB;
import com.example.kimdongun.paintchat.service.SocketService;
import com.example.kimdongun.paintchat.service.SocketServiceExecutor;
import com.example.kimdongun.paintchat.toast.NormalChatToast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class ARMaskGameActivity extends AppCompatActivity implements View.OnClickListener, HandlerMessageDecode, SocketServiceExecutor {
    private SensorARInventory sensorARInventory;
    private CameraPreview cameraPreview;
    private Camera camera;
    private RelativeLayout layout; //부모 레이아웃
    private boolean isVisibleMask;

    private LinearLayout layout_maskInfo; //마스크 정보 레이아웃
    private ImageView imageView_touchMask; //마스크 이미지
    private TextView textView_maskName; //마스크 이름
    private TextView textView_OldID; //마스크 원래 주인
    private TextView textView_Memo; //마스크 메모
    private TextView textView_Date; //마스크 습득 날짜

    private MaskGameCanvas maskGameCanvas; //게임 그림판
    private Handler canvasHandler_; //그림판 이벤트 받는 핸들러
    private Handler sensorHandler_; //센서 이벤트 받는 핸들러

    private TextView textView_count; //마스크 카운트
    private ImageView imageView_map; //맵으로 돌아가기

    // Camera.CameraInfo.CAMERA_FACING_FRONT or Camera.CameraInfo.CAMERA_FACING_BACK
    private final int cameraIndex = 0; // 0 - 후면 카메라 1- 전면 카메라

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
        setContentView(R.layout.activity_ar_mask_game);

        canvasHandler_ = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                @ColorInt int color = sensorARInventory.maskManager.printVertex((CanvasRect)msg.obj);
                String str;
                if(color == Color.RED){ //범위 밖
                    str = "마스크 범위를 벗어 났습니다.";
                }else if(color == Color.GREEN){ //범위 안
                    str = "마스크를 정확히 잡았습니다.";
                    maskGameCanvas.drawable_ = false;
                    sensorARInventory.isInsideSector = true;
                    sensorARInventory.canvasRect = (CanvasRect)msg.obj;
                    new Thread(new Runnable() { //3초 쓰레드 실행
                        @Override
                        public void run() {
                            for(int i = 0; i < 3 && sensorARInventory.isInsideSector; i++){
                                Message msg = handler.obtainMessage();
                                msg.what = 0;
                                msg.arg1 = 3-i;
                                handler.sendMessage(msg); //카운트 초 핸들러로 전송
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            if(sensorARInventory.isInsideSector) { //마스크 획득 성공
                                Message msg = handler.obtainMessage();
                                msg.what = 1;
                                msg.arg1 = 1;
                                handler.sendMessage(msg);
                            }else{ //마스크 획득 실패
                                Message msg = handler.obtainMessage();
                                msg.what = 1;
                                msg.arg1 = 0;
                                handler.sendMessage(msg);
                            }
                        }

                        //화면에 초 띄우고 마스크 획득 여부 UI관리하는 핸들러
                        Handler handler = new Handler(){
                            @Override
                            public void handleMessage(Message msg) {
                                super.handleMessage(msg);
                                if(msg.what == 0){ //초 변경
                                    textView_count.bringToFront();
                                    textView_count.setVisibility(View.VISIBLE);
                                    textView_count.setText(String.valueOf(msg.arg1));
                                }else{ //마스크 획득
                                    textView_count.setVisibility(View.INVISIBLE);
                                    sensorARInventory.isInsideSector = false;
                                    maskGameCanvas.drawable_ = true;
                                    if(msg.arg1 == 1) {
                                        Toast.makeText(ARMaskGameActivity.this, "마스크를 획득했습니다.", Toast.LENGTH_SHORT).show();
                                        maskGameCanvas.drawable_ = false;
                                        maskGameCanvas.setVisibility(View.GONE); //모두 지우기
                                        getMask();
                                    }
                                }
                            }
                        };
                    }).start();
                }else{ //너무 큰 범위
                    str = "범위가 너무 넓습니다.";
                }
                Toast.makeText(ARMaskGameActivity.this, str, Toast.LENGTH_SHORT).show();
                maskGameCanvas.drawRect(color);
            }
        };

        sensorHandler_ = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                @ColorInt int color = msg.arg1;
                if(color == Color.RED){ //범위 밖
                    Toast.makeText(ARMaskGameActivity.this, "마스크 범위를 벗어 났습니다.", Toast.LENGTH_SHORT).show();
                    sensorARInventory.isInsideSector = false;
                    textView_count.setVisibility(View.INVISIBLE);
                    maskGameCanvas.drawable_ = true;
                }
                maskGameCanvas.drawRect(color);
            }
        };

        handler_ = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                String str = msg.obj.toString();
                if(str.equals("connectSuccess")){ //소켓 연결 성공
                    Toast.makeText(ARMaskGameActivity.this, "소켓 서버와 연결하였습니다.", Toast.LENGTH_SHORT).show();
                }else if(str.equals("connectFail")){ //소켓 연결 실패
                    Toast.makeText(ARMaskGameActivity.this, "소켓 서버와 연결을 실패하였습니다.", Toast.LENGTH_SHORT).show();
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

        Intent serviceIntent = new Intent(ARMaskGameActivity.this, SocketService.class);
        bindService(serviceIntent, serviceConnection_, Context.BIND_AUTO_CREATE); //intent객체, 서비스와 연결에 대한 정의

        //이미지 뷰 초기화
        maskGameCanvas = (MaskGameCanvas) findViewById(R.id.canvas);
        maskGameCanvas.setHandler(canvasHandler_); //핸들러 설정

        client_ = Client.getInstance(this);

        layout = (RelativeLayout)findViewById(R.id.layout);
        isVisibleMask = true;

        textView_count = (TextView)findViewById(R.id.textView_count); //마스크 카운트
        imageView_map = (ImageView)findViewById(R.id.imageView_map); //맵으로 돌아가기

       sensorARInventory = new SensorARInventory(this,isVisibleMask);
        sensorARInventory.setHandler(sensorHandler_);

        //화면 크기
        WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        mDisplay = wm.getDefaultDisplay();

        //습득 할 마스크 이미지 추가
        Intent intent = getIntent();
        String name = intent.getStringExtra("name");
        float azimuth = intent.getFloatExtra("azimuth", 120);
        ImageView imageView = new ImageView(ARMaskGameActivity.this); //이미지 뷰 생성
        if(isVisibleMask)
            imageView.setVisibility(View.VISIBLE);
        else
            imageView.setVisibility(View.INVISIBLE);
        layout.addView(imageView); //이미지 뷰 레이아웃에 추가
        Glide.with(ARMaskGameActivity.this).load("http://211.110.229.53/mask_image/" + name).into(imageView);
        @IdRes int id = 0;
        imageView.setId(id); //아이디 설정
        float rate1 = imageView.getLayoutParams().height/imageView.getLayoutParams().width; //비율 계산
        imageView.getLayoutParams().width = mDisplay.getWidth()/3; //넓이 설정
        imageView.getLayoutParams().height = (int)(imageView.getLayoutParams().width * rate1); //높이 설정 (비율 이용)
        imageView.setX(mDisplay.getWidth()*1.5f);
        imageView.setY(mDisplay.getHeight()*1.5f);
        imageView.setBackground(getResources().getDrawable(R.drawable.mask_shape));
        sensorARInventory.maskManager.putMask(id, new MaskItem(name, "", "", "", imageView), azimuth);

        //마스크 정보 창 초기화
        layout_maskInfo = (LinearLayout)findViewById(R.id.layout_maskInfo); //마스크 정보 레이아웃
        imageView_touchMask = (ImageView)findViewById(R.id.imageView_touchMask); //마스크 이미지
        textView_maskName = (TextView)findViewById(R.id.textView_maskName); //마스크 이름
        textView_OldID = (TextView)findViewById(R.id.textView_OldID); //마스크 원래 주인
        textView_Memo = (TextView)findViewById(R.id.textView_Memo); //마스크 메모
        textView_Date = (TextView)findViewById(R.id.textView_Date); //마스크 습득 날짜
        layout_maskInfo.bringToFront(); //맨 앞으로
        layout_maskInfo.setVisibility(View.INVISIBLE); //정보 창 안보이게
        Glide.with(ARMaskGameActivity.this).load("http://211.110.229.53/mask_image/" + name).into(imageView_touchMask);
        textView_maskName.setText(name);
        //마스크 이미지 크기 설정
        float rate = imageView_touchMask.getLayoutParams().height/imageView_touchMask.getLayoutParams().width; //비율 계산
        imageView_touchMask.getLayoutParams().width = mDisplay.getWidth()*2/3; //넓이 설정
        imageView_touchMask.getLayoutParams().height = (int)(imageView_touchMask.getLayoutParams().width * rate); //높이 설정 (비율 이용)

        normalChatToast = new NormalChatToast(this); //채팅 토스트 메세지
        //터치 이벤트
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
//        DebugHandler.log(getClass().getName(), "H: " + sensorARInventory.cameraHorizontalAngle + "     V: " + sensorARInventory.cameraVerticalAngle);
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

        if(isLiveBinder){ //서비스랑 바인드 된 경우
            unbindService(serviceConnection_);
        }

        HashMap<String, String> postData = new HashMap<>();
        postData.put("name", textView_maskName.getText().toString());

        PostDB postDB = new PostDB();
        postDB.putFileData("init_throw_mask.php", postData, null, null, new OnFinishDBListener() {
            @Override
            public void onSuccess(String output) {

            }
        });

        //사용 한 객체 초기화
        sensorARInventory.maskManager.destroyManager();

        sensorARInventory = null;
        cameraPreview = null;
        camera = null;
        layout = null; //부모 레이아웃

        layout_maskInfo = null; //마스크 정보 레이아웃
        imageView_touchMask = null; //마스크 이미지
        textView_maskName = null; //마스크 이름
        textView_OldID = null; //마스크 원래 주인
        textView_Memo = null; //마스크 메모
        textView_Date = null; //마스크 습득 날짜

        maskGameCanvas = null; //게임 그림판
        canvasHandler_ = null; //그림판 이벤트 받는 핸들러
        sensorHandler_ = null; //센서 이벤트 받는 핸들러

        textView_count = null; //마스크 카운트
        imageView_map = null; //맵으로 돌아가기

        handler_ = null; //소켓 스레드랑 통신 할 핸들러
        socketService_ = null; //소켓 서비스
        serviceConnection_ = null; //서비스 커넥션

        normalChatToast = null; //채팅 토스트 메세지
        mDisplay = null; //디스플레이 정보
    }

    @Override
    public void onBackPressed() {
        HashMap<String, String> postData = new HashMap<>();
        postData.put("name", textView_maskName.getText().toString());

        PostDB postDB = new PostDB();
        postDB.putFileData("init_throw_mask.php", postData, null, null, new OnFinishDBListener() {
            @Override
            public void onSuccess(String output) {
                Intent intent = new Intent(ARMaskGameActivity.this, MapsActivity.class);
                startActivity(intent);
                finish();
            }
        });
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
                                                  int cameraId, Camera camera) {
        Camera.CameraInfo info =
                new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
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

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.imageView_map: //맵으로 돌아가기
                onBackPressed();
                break;
        }
    }

    //마스크 습득
    private void getMask(){
        sensorARInventory.maskManager.setVisibleMyMask(false, 0); //마스크 안보이게

        HashMap<String, String> postData = new HashMap<>();
        postData.put("id", client_.account_.id_);
        postData.put("name", textView_maskName.getText().toString());

        PostDB postDB = new PostDB(this);
        postDB.putFileData("get_mask.php", postData, null, null, new OnFinishDBListener() {
            @Override
            public void onSuccess(String output) {
                try {
                    JSONObject reader = new JSONObject(output);
                    if(reader.get("mask").equals(null)){ //마스크 없는 경우

                    }else{ //마스크 있는 경우
                        //json값 변환
                        JSONArray socialArray = reader.getJSONArray("mask");
                        for (int i = 0; i < socialArray.length(); i++) { //커스텀 마스크 추가
                            JSONObject object = socialArray.getJSONObject(i);
                            String oldID = object.getString("old_id"); //마스크 마스크 원래 주인
                            String memo = object.getString("memo"); //마스크 마스크 메모
                            String date = object.getString("date"); //마스크 습득 날짜

                            layout_maskInfo.bringToFront(); //맨 앞으로
                            layout_maskInfo.setVisibility(View.VISIBLE); //정보 창 보이게
                            textView_OldID.setText(oldID); //마스크 원래 주인
                            textView_Memo.setText(memo); //마스크 메모
                            textView_Date.setText(date); //마스크 습득 날짜
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
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

        normalChatToast.showToast(account.profileUrl_, account.nick_, (String)map.get("msg"));
    }

    @Override
    public void receiveReadChat(Object jsonObj) {
    }

    @Override
    public void receiveExitServerResult() {
    }
}
