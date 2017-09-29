package com.example.kimdongun.paintchat.activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.example.kimdongun.paintchat.Account;
import com.example.kimdongun.paintchat.Client;
import com.example.kimdongun.paintchat.DebugHandler;
import com.example.kimdongun.paintchat.HandlerMessageDecode;
import com.example.kimdongun.paintchat.Permission;
import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.SQLiteHandler;
import com.example.kimdongun.paintchat.SensorARMap;
import com.example.kimdongun.paintchat.item.ChatRoomListViewItem;
import com.example.kimdongun.paintchat.item.ThrowMaskItem;
import com.example.kimdongun.paintchat.server.OnFinishDBListener;
import com.example.kimdongun.paintchat.server.PostDB;
import com.example.kimdongun.paintchat.service.SocketService;
import com.example.kimdongun.paintchat.service.SocketServiceExecutor;
import com.example.kimdongun.paintchat.toast.NormalChatToast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import static com.example.kimdongun.paintchat.Permission.PERMISSION_REQUEST_CODE;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener, View.OnClickListener,
                                                                        HandlerMessageDecode, SocketServiceExecutor {
    private String[] PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    private ImageView imageView_location; //내 위치 버튼
    private ImageView imageView_inventory; //인벤토리 버튼

    private SensorARMap sensorARMap;

    private GoogleMap mMap;
    private LatLng latlng; //사용자 위치
    private Marker clientMarker; //사용자 마커
    private Circle clientCircle; //사용자 인식 범위
    private double circleRadius = 45; //원 반지름 (meter)
    private int cameraZoom = 18; //카메라 자동 줌
    private boolean followMyPos = true; //카메라를 내 위치로 고정 시킬 것인가?

    private ArrayList<Marker> maskMarker = new ArrayList<>(); //마스크 마커
    private ArrayList<ThrowMaskItem> throwMaskItems = new ArrayList<>(); //버려진 마스크 목록

    public LocationManager locationManager; //위치 수신하기 위함
    public LocationListener mLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            //여기서 위치값이 갱신되면 이벤트가 발생한다.
            //값은 Location 형태로 리턴되며 좌표 출력 방법은 다음과 같다.
            latlng = new LatLng(location.getLatitude(), location.getLongitude()); //위도, 경도
            double altitude = location.getAltitude();   //고도
            float accuracy = location.getAccuracy();    //정확도
            String provider = location.getProvider();   //위치제공자

            //Gps 위치제공자에 의한 위치변화. 오차범위가 좁다.
            //Network 위치제공자에 의한 위치변화
            //Network 위치는 Gps에 비해 정확도가 많이 떨어진다.
            if(mMap != null) {
                clientMarker.setPosition(latlng); //마커 위치 설정
                clientCircle.setCenter(latlng); //원 위치 설정
                if(followMyPos)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, cameraZoom)); //마커로 카메라 이동
            }
        }

        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    public Client client_; //계정 클라이언트
    private Handler handler_; //소켓 스레드랑 통신 할 핸들러
    public SocketService socketService_; //소켓 서비스
    private ServiceConnection serviceConnection_; //서비스 커넥션
    public boolean isLiveBinder; //서비스 바인드 연결 유무
    private NormalChatToast normalChatToast; //채팅 토스트 메세지

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        handler_ = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                String str = msg.obj.toString();
                if(str.equals("connectSuccess")){ //소켓 연결 성공
                    Toast.makeText(MapsActivity.this, "소켓 서버와 연결하였습니다.", Toast.LENGTH_SHORT).show();
                }else if(str.equals("connectFail")){ //소켓 연결 실패
                    Toast.makeText(MapsActivity.this, "소켓 서버와 연결을 실패하였습니다.", Toast.LENGTH_SHORT).show();
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

        Intent serviceIntent = new Intent(MapsActivity.this, SocketService.class);
        bindService(serviceIntent, serviceConnection_, Context.BIND_AUTO_CREATE); //intent객체, 서비스와 연결에 대한 정의

        client_ = Client.getInstance(this);

        imageView_location = (ImageView)findViewById(R.id.imageView_location);
        imageView_inventory = (ImageView)findViewById(R.id.imageView_inventory);

        if (!Permission.hasPermissions(this, PERMISSIONS)) { //퍼미션 허가를 했었는지 여부를 확인
            Permission.requestNecessaryPermissions(this, PERMISSIONS);//퍼미션 허가안되어 있다면 사용자에게 요청
        } else {
            // Location 제공자에서 정보를 얻어오기(GPS)
            // 1. Location을 사용하기 위한 권한을 얻어와야한다 AndroidManifest.xml
            //     ACCESS_FINE_LOCATION : NETWORK_PROVIDER, GPS_PROVIDER
            //     ACCESS_COARSE_LOCATION : NETWORK_PROVIDER
            // 2. LocationManager 를 통해서 원하는 제공자의 리스너 등록
            // 3. GPS 는 에뮬레이터에서는 기본적으로 동작하지 않는다
            // 4. 실내에서는 GPS_PROVIDER 를 요청해도 응답이 없다.  특별한 처리를 안하면 아무리 시간이 지나도
            //    응답이 없다.
            //    해결방법은
            //     ① 타이머를 설정하여 GPS_PROVIDER 에서 일정시간 응답이 없는 경우 NETWORK_PROVIDER로 전환
            //     ② 혹은, 둘다 한꺼번헤 호출하여 들어오는 값을 사용하는 방식.// LocationManager 객체를 얻어온다
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, // 등록할 위치제공자
                    100, // 통지사이의 최소 시간간격 (miliSecond)
                    1, // 통지사이의 최소 변경거리 (m)
                    mLocationListener);
//            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, // 등록할 위치제공자
//                    100, // 통지사이의 최소 시간간격 (miliSecond)
//                    1, // 통지사이의 최소 변경거리 (m)
//                    mLocationListener);
        }
        normalChatToast = new NormalChatToast(this); //채팅 토스트 메세지

        //터치 이벤트
        imageView_inventory.setOnClickListener(this);
        imageView_location.setOnClickListener(this);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(sensorARMap != null)
            sensorARMap.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(sensorARMap != null)
            sensorARMap.stop();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(sensorARMap != null)
            sensorARMap.stop();
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
        imageView_location = null; //내 위치 버튼
        imageView_inventory = null; //인벤토리 버튼

        sensorARMap = null;

        mMap = null;
        latlng = null; //사용자 위치
        clientMarker = null; //사용자 마커
        clientCircle = null; //사용자 인식 범위

        if(maskMarker != null)
            maskMarker.clear(); //마스크 마커
        maskMarker = null;

        if(throwMaskItems != null)
            throwMaskItems.clear();; //버려진 마스크 목록
        throwMaskItems = null;

        locationManager = null; //위치 수신하기 위함
        mLocationListener = null;

        normalChatToast = null; //채팅 토스트 메세지
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(MapsActivity.this, MainActivity.class);
        intent.putExtra("fragId", 3); //메인 화면 프래그먼트 index
        startActivity(intent);
        finish();
    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user  ll be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        DebugHandler.log(getClass().getName(), "onMapReady");
        mMap = googleMap;
        mMap.setMinZoomPreference(17f); //최소 줌
        mMap.setMaxZoomPreference(20f); //최대 줌

        mMap.getUiSettings().setMapToolbarEnabled(false); //지도 툴바 안 보이게
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);

        sensorARMap = new SensorARMap(MapsActivity.this);
        sensorARMap.start();

        loadThrowMask();
    }

    @Override
    public void onMapClick(LatLng latLng) {
        DebugHandler.log("onMapClick", "touch");
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        followMyPos = false; //카메라 이동 해제
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    boolean findLocation = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean accessCoarseLocation = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if ( !findLocation || !accessCoarseLocation)
                        {
                            showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
                            return;
                        }
                    }
                }
                break;
            }
        }
    }

    private void showDialogForPermission(String msg) {
        final AlertDialog.Builder myDialog = new AlertDialog.Builder(MapsActivity.this);
        myDialog.setTitle("알림");
        myDialog.setMessage(msg);
        myDialog.setCancelable(false);
        myDialog.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(PERMISSIONS, PERMISSION_REQUEST_CODE);
                }
            }
        });
        myDialog.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                finish();
            }
        });
        myDialog.show();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if(marker.getTag() != null) {
//            Toast.makeText(this, marker.getTag() + " 터치", Toast.LENGTH_SHORT).show();
            if(latlng != null) { //현재 위치 파악 된 경우
                Integer index = (Integer)marker.getTag();
                final ThrowMaskItem maskItem = throwMaskItems.get(index);
                float[] result = new float[3];
                Location.distanceBetween(latlng.latitude, latlng.longitude,
                        maskItem.latLng.latitude, maskItem.latLng.longitude, result);
                if (result[0] <= circleRadius) { //마스크가 범위 안에 있는 경우
                    HashMap<String, String> postData = new HashMap<>();
                    postData.put("name", maskItem.maskName);

                    PostDB postDB = new PostDB(this);
                    postDB.putFileData("select_mask.php", postData, null, null, new OnFinishDBListener() {
                        @Override
                        public void onSuccess(String output) {
                            if(output.equals("success")){
                                Intent intent = new Intent(MapsActivity.this, ARMaskGameActivity.class);
                                intent.putExtra("name", maskItem.maskName); //마스크 이름 전송
                                intent.putExtra("azimuth", sensorARMap.azimuthZ); //방위각 전송
                                startActivity(intent);
                                finish();
                            }else{
                                Toast.makeText(MapsActivity.this, "다른 사용자가 접근중인인 마스크 입니다.", Toast.LENGTH_SHORT).show();
                                loadThrowMask();
                            }
                        }
                    });
                } else { //마스크가 범위 밖에 있는 경우
                    Toast.makeText(this, "마스크가 너무 멀리 있습니다.", Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(this, "GPS 정보를 받고 있습니다.\n잠시후 다시 시도하세요.", Toast.LENGTH_SHORT).show();
            }
            followMyPos = false; //카메라 이동 해제
        }
        return false;
    }

    private void loadThrowMask(){
        maskMarker.clear(); //마스크 마커
        throwMaskItems.clear(); //버려진 마스크 목록
        mMap.clear(); //모든 마커 삭제

        //초기 위치
        if(latlng == null) {
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if(location != null) {
                DebugHandler.log("LastLocation", "lat: " + location.getLatitude() + "   lng: " + location.getLongitude());
                latlng = new LatLng(location.getLatitude(), location.getLongitude());
            }else
                latlng = new LatLng(37.483852, 126.972194); //(2사무실으로 해놨음)
        }

        clientMarker = mMap.addMarker(new MarkerOptions()
                .position(latlng)
                .anchor(0.5f, 0.5f)
                .flat(true)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.my_location))); //사용자 마커 추가
        CircleOptions circleOptions = new CircleOptions()
                .center(latlng)
                .radius(circleRadius)
                .strokeColor(getResources().getColor(R.color.colorBrightBlueAlpha))
                .fillColor(getResources().getColor(R.color.colorBrightBlueAlpha));
        clientCircle = mMap.addCircle(circleOptions); //사용자 원 추가

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, cameraZoom)); //카메라 이동
        sensorARMap.setClientMarker(clientMarker); //센서에 사용자 마커 연동

        PostDB postDB = new PostDB(this);
        postDB.putFileData("load_throw_mask.php", null, null, null, new OnFinishDBListener() {
            @Override
            public void onSuccess(String output) {
                try {
                    JSONObject reader = new JSONObject(output);
                    if(reader.get("mask").equals(null)){ //마스크 없는 경우

                    }else{ //마스크 있는 경우
                        //마커 배경 이미지
                        final Bitmap maskBack = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.marker_back), 120, 120, false);
                        //json값 변환
                        JSONArray socialArray = reader.getJSONArray("mask");
                        for (int i = 0; i < socialArray.length(); i++) { //커스텀 마스크 추가
                            JSONObject object = socialArray.getJSONObject(i);
                            final String name = object.getString("name"); //마스크 이름
                            final double lat = object.getDouble("lat"); //마스크 위도
                            final double lng = object.getDouble("lng"); //마스크 경도도

                            Glide.with(MapsActivity.this).load("http://211.110.229.53/mask_image/" + name)
                                    .asBitmap().into(new SimpleTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                                    ThrowMaskItem mask = new ThrowMaskItem(name, lat, lng);
                                    throwMaskItems.add(mask);

                                    //마커 이미지
                                    Bitmap maskImg = Bitmap.createScaledBitmap(resource, 100, 100, false);

                                    //배경과 이미지를 합친 bitmap => 최종 마커
                                    Bitmap bmOverlay = Bitmap.createBitmap(maskBack.getWidth(), maskBack.getHeight(), maskBack.getConfig());
                                    Canvas canvas = new Canvas(bmOverlay);
                                    canvas.drawBitmap(maskBack, new Matrix(), null);
                                    canvas.drawBitmap(maskImg, 10, 10, null);

                                    Marker marker = mMap.addMarker(new MarkerOptions()
                                            .position(mask.latLng)
                                            .anchor(0.5f, 0.5f)
                                            .flat(true)
                                            .icon(BitmapDescriptorFactory.fromBitmap(bmOverlay)));
                                    maskMarker.add(marker); //사용자 마커 추가
                                    maskMarker.get(maskMarker.size() - 1).setTag(maskMarker.size() - 1);
                                }
                            });
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.imageView_location: //내 위치 클릭
                if(mMap != null && latlng != null) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, cameraZoom)); //마커로 카메라 이동
                }else{
                    Toast.makeText(this, "GPS 정보를 받고 있습니다.\n잠시후 다시 시도하세요.", Toast.LENGTH_SHORT).show();
                }
                followMyPos = true;//카메라 이동 설정
                break;

            case R.id.imageView_inventory://인벤토리 클릭
                Intent intent = new Intent(MapsActivity.this, ARInventoryActivity.class);
                startActivity(intent);
                finish();
                break;
        }
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
