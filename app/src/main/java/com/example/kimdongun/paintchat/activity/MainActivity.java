package com.example.kimdongun.paintchat.activity;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.example.kimdongun.paintchat.Account;
import com.example.kimdongun.paintchat.Client;
import com.example.kimdongun.paintchat.DebugHandler;
import com.example.kimdongun.paintchat.HandlerMessageDecode;
import com.example.kimdongun.paintchat.JsonEncode;
import com.example.kimdongun.paintchat.Permission;
import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.adapter.ViewPagerAdapter;
import com.example.kimdongun.paintchat.dialog.AddSocialDialog;
import com.example.kimdongun.paintchat.dialog.CreateChatDialog;
import com.example.kimdongun.paintchat.dialog.CreateGameRoomDialog;
import com.example.kimdongun.paintchat.fragment.FragmentAccount;
import com.example.kimdongun.paintchat.fragment.FragmentChat;
import com.example.kimdongun.paintchat.fragment.FragmentGame;
import com.example.kimdongun.paintchat.fragment.FragmentSocial;
import com.example.kimdongun.paintchat.item.ChatRoomListViewItem;
import com.example.kimdongun.paintchat.item.GameRoomListViewItem;
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

import static com.example.kimdongun.paintchat.Permission.PERMISSION_REQUEST_CODE;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, HandlerMessageDecode, SocketServiceExecutor {
    public final static int REQ_CODE_SELECT_CAMERA=100; //카메라 액션 코드
    public final static int REQ_CODE_SELECT_IMAGE=150; //갤러리 액션 코드
    public final static int REQ_CODE_SELECT_CANVAS=200; //캔버스 그림 액션 코드
    public final static int REQ_CODE_PROFILE_FILTER=250; //필터 적용 액션 코드
    public final static int REQ_CODE_SELECT_VIDEO=300; //비디오 액션 코드

    private String[] PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};

    private ViewPager viewPager; //뷰 페이저
    public ViewPagerAdapter viewPagerAdapter_; //뷰 페이저 어댑터
    private TabLayout tabLayout; //텝 레이아웃
    private FloatingActionButton floating_btn; //floating 버튼
    public Client client_; //계정 클라이언트
    private CreateGameRoomDialog createGameRoomDialog_; //방 만들기 다이얼로그
    private AddSocialDialog addSocialDialog_; //친구 추가 다이얼로그
    private CreateChatDialog inviteChatDialog_; //채팅 친구 초대 다이얼 로그

    private Handler handler_; //소켓 스레드랑 통신 할 핸들러
    public SocketService socketService_; //소켓 서비스
    private ServiceConnection serviceConnection_; //서비스 커넥션
    public boolean isLiveBinder; //서비스 바인드 연결 유무

    private NormalChatToast normalChatToast; //채팅 토스트 메세지
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //초기화
        handler_ = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                String str = msg.obj.toString();
                if(str.equals("connectSuccess")){ //소켓 연결 성공
                    Toast.makeText(MainActivity.this, "소켓 서버와 연결하였습니다.", Toast.LENGTH_SHORT).show();

                    //게임 방 목록 전체 갱신
                    String[] keys1 = {""};
                    Object[] values1 = {""};
                    String jsonStr1 = JsonEncode.getInstance().encodeCommandJson("gameRoomList", keys1, values1);
                    //서버로 전송
                    socketService_.sendMessage(jsonStr1);
                }else if(str.equals("connectFail")){ //소켓 연결 실패
                    Toast.makeText(MainActivity.this, "소켓 서버와 연결을 실패하였습니다.", Toast.LENGTH_SHORT).show();
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

                    //게임 방 목록 전체 갱신
                    String[] keys1 = {""};
                    Object[] values1 = {""};
                    String jsonStr1 = JsonEncode.getInstance().encodeCommandJson("gameRoomList", keys1, values1);
                    //서버로 전송
                    socketService_.sendMessage(jsonStr1);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                isLiveBinder = false;
            }
        };

        Intent serviceIntent = new Intent(MainActivity.this, SocketService.class);
        bindService(serviceIntent, serviceConnection_, Context.BIND_AUTO_CREATE); //intent객체, 서비스와 연결에 대한 정의

        client_ = Client.getInstance(this);

        if (!Permission.hasPermissions(this, PERMISSIONS)) { //퍼미션 허가를 했었는지 여부를 확인
            Permission.requestNecessaryPermissions(this, PERMISSIONS);//퍼미션 허가안되어 있다면 사용자에게 요청
        } else {
            //이미 사용자에게 퍼미션 허가를 받음.
        }

        final String[] titleNames = {"친구", "게임", "채팅", "계정"};

        viewPager = (ViewPager) findViewById(R.id.view_pager);
        viewPagerAdapter_ = new ViewPagerAdapter(getSupportFragmentManager());
        viewPagerAdapter_.addFragment(new FragmentSocial(), "친구 목록");
        viewPagerAdapter_.addFragment(new FragmentGame(), "게임 목록");
        viewPagerAdapter_.addFragment(new FragmentChat(), "채팅 목록");
        viewPagerAdapter_.addFragment(new FragmentAccount(), "계정 설정");
        viewPager.setAdapter(viewPagerAdapter_);

        tabLayout = (TabLayout)findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        floating_btn = (FloatingActionButton)findViewById(R.id.floating_btn);

        Intent intent = getIntent();
        final int currentFragment = intent.getIntExtra("fragId", 0); //처음 프래그먼트

        //터치 이벤트
        floating_btn.setOnClickListener(this);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                setTitle(titleNames[position]);
                switch (position){
                    case 0: //소셜 화면
                        setTitle(titleNames[position] + " " + client_.socialListViewAdapter.getCount() + " 명");
                        floating_btn.setVisibility(View.VISIBLE);
                        floating_btn.setImageResource(R.drawable.ic_add);
                        break;
                    case 1: //게임 화면
                        floating_btn.setVisibility(View.VISIBLE);
                        floating_btn.setImageResource(R.drawable.ic_add);
                        break;
                    case 2: //채팅 화면
                        floating_btn.setVisibility(View.VISIBLE);
                        floating_btn.setImageResource(R.drawable.ic_add);
                        break;
                    case 3://계정 화면
                        floating_btn.setVisibility(View.INVISIBLE);
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        setTitle(titleNames[currentFragment]); //엑티비티 타이틀 수정
        if(currentFragment == 0){
            setTitle(titleNames[currentFragment] + " " + client_.socialListViewAdapter.getCount() + " 명");
        }
        viewPager.setCurrentItem(currentFragment, true); //뷰 페이저 currentFragment_ 번째로 설정

        normalChatToast = new NormalChatToast(this); //채팅 토스트 메세지
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DebugHandler.log(getClass().getName(), "onDestroy");
        if(isLiveBinder){ //서비스랑 바인드 된 경우
            unbindService(serviceConnection_);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        DebugHandler.log(getClass().getName(), "requestCode: " + requestCode);
        if(resultCode== Activity.RESULT_OK)
        {
            //프로필 사진 변경과 관련된 requestCode 받는 경우 계정 설정 프래그먼트로 작업을 넘김
            if(requestCode == REQ_CODE_SELECT_CANVAS || requestCode == REQ_CODE_SELECT_IMAGE ||
                    requestCode == REQ_CODE_SELECT_CAMERA || requestCode == REQ_CODE_PROFILE_FILTER) {
                FragmentAccount fragment = (FragmentAccount) viewPagerAdapter_.getItem(3);
                fragment.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    boolean readAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean cameraPermissionAccepted = grantResults[2] == PackageManager.PERMISSION_GRANTED;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                        if ( !readAccepted || !writeAccepted || !cameraPermissionAccepted)
                        {
                            showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
                            return;
                        }
                    }
                }
                break;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void showDialogForPermission(String msg) {
        final AlertDialog.Builder myDialog = new AlertDialog.Builder(MainActivity.this);
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
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.floating_btn:
                floatingButtonEvent();
                break;
        }
    }

    //각 프레그먼트 floatingButton 이벤트
    public void floatingButtonEvent(){
        switch (viewPager.getCurrentItem()){
            case 0: //소셜 화면
                addSocialDialog_ = new AddSocialDialog(this, client_);
                addSocialDialog_.show();
                break;

            case 1: //게임 화면
                createGameRoomDialog_ = new CreateGameRoomDialog(this, client_);
                createGameRoomDialog_.show();
                break;

            case 2: //채팅 화면
                inviteChatDialog_ = new CreateChatDialog(this, client_);
                inviteChatDialog_.show();
                break;

            case 4: //계정 화면
                break;

            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void doReceiveAction(String request) {
        DebugHandler.log(getClass().getName(), " JSON" + request);
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

        try {
            String cmdStr = (String) arrayList.get(0); //명령 커맨드 값
            Object requestObj = arrayList.get(1); //명령 디테일 값

            switch (cmdStr) {
                case "normalChat": //채팅 행동
                    receiveNormalChat(requestObj);
                    break;

                case "readChat": //채팅 읽음 처리 행동
                    receiveReadChat(requestObj); //서비스에서 처리할 것 (SQLite에 해당 message 읽은 수 줄임)
                    break;

                case "exitServerResult": //서버 접속 종료 행동
                    receiveExitServerResult();
                    break;

                case "createGameRoomResult": //방 생성 요청 후 결과
                    receiveCreateGameRoom(requestObj);
                    break;

                case "enterGameRoomResult": //방 입장 요청 후 결과
                    receiveEnterGameRoom(requestObj);
                    break;

                case "gameRoomListResult": //방 목록 갱신 요청 후 결과
                    receiveGameRoomList(requestObj);
                    break;

                case "createNewGameRoomInfo": //새로 생성된 방 목록 갱신
                    receiveCreateNewGameRoom(requestObj);
                    break;

                case "createChatRoomResult": //채팅방 생성 요청 결과
                    receiveCreateChatRoom(requestObj);
                    break;
            }
        }catch (IndexOutOfBoundsException e){
            e.printStackTrace();
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
            int user_num = (int)map.get("num"); //채팅방있는 유저 수
            user_num += 1; //본인 추가

            String[] accountNickArray = ((String)map.get("name")).split(",");
            //방 리스트에 뜨는 친구 닉네임 설정 (방 제목에서 자신의 닉네임은 안 보이도록)
            ArrayList<String> tempList = new ArrayList<>();
            //닉네임 중 자신을 지우기
            for(int i = 0; i < accountNickArray.length; i++) {
                if(!accountNickArray[i].equals(client_.account_.nick_)) {
                    DebugHandler.log(getClass().getName(),"accountNickArray: " + accountNickArray[i]);
                    tempList.add(accountNickArray[i]);
                }
            }
            //방 리스트에 뜨는 친구 닉네임 설정 (방 제목)
            //프로필 사진 url가져오기
            String newRoomName = "";
            final ArrayList<String> profileList = new ArrayList<>();
            for(int i = 0; i < tempList.size(); i++){
                Account account = client_.socialListViewAdapter.getItemByNick(tempList.get(i));
                if(account == null){
                    account = client_.recommendSocialListViewAdapter.getItemByNick(tempList.get(i));
                }
                if(account == null){
                    account = new Account("", "http://211.110.229.53/profile_image/default.png", null, tempList.get(i),
                            null, 1, 1, 1);
                }
                profileList.add(account.profileUrl_);
                newRoomName += tempList.get(i);
                if (i < tempList.size() - 1)
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
        //새로 방 생성
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

    //게임 방 생성 요청 후 행동
    private void receiveCreateGameRoom(Object jsonObj){
        HashMap<String, Object> map = new HashMap<String, Object>();
        try {
            JSONObject json = (JSONObject)jsonObj;
            map.put("result", json.get("result")); //결과
            map.put("maxNum", json.get("maxNum")); //최대 인원
            map.put("name", json.get("name")); //방 이름
            map.put("key", json.get("key")); //방 키값
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String result = (String)map.get("result");
        if(result.equals("success")){ //게임 방 만들기 성공
            Intent intent = new Intent(MainActivity.this, GameRoomActivity.class);
            intent.putExtra("drawable", true);
            intent.putExtra("maxNum", (int)map.get("maxNum"));
            intent.putExtra("name", (String)map.get("name"));
            intent.putExtra("key", (String)map.get("key"));
            intent.putExtra("isGameStart", false);
            intent.putExtra("host", client_.account_.nick_);
            intent.putExtra("quizHost", client_.account_.nick_);
            intent.putExtra("gameTime", 0);

            startActivity(intent);
            finish();
        }else{ //게임 방 만들기 실패
            Toast.makeText(MainActivity.this, "게임 방 생성에 실패하였습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    //게임 방 입장 요청 후  행동
    private void receiveEnterGameRoom(Object jsonObj){
        HashMap<String, Object> map = new HashMap<String, Object>();
        try {
            JSONObject json = (JSONObject)jsonObj;
            map.put("result",json.get("result")); //결과
            map.put("maxNum",json.get("maxNum")); //방 최대 인원
            map.put("name", json.get("name")); //방 이름
            map.put("key", json.get("key")); //방 키값
            map.put("host", json.get("host")); //방장 닉네임
            map.put("isGameStart", json.get("isGameStart")); //방 상태
            map.put("quizHost", json.get("quizHost")); //문제 출제자 닉네임
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String result = (String)map.get("result");
        switch(result){
            case "success": //게임 방 입장 성공
                Intent intent = new Intent(MainActivity.this, GameRoomActivity.class);
                intent.putExtra("drawable", !(boolean)map.get("isGameStart"));
                intent.putExtra("maxNum", (int)map.get("maxNum"));
                intent.putExtra("name", (String)map.get("name"));
                intent.putExtra("key", (String)map.get("key"));
                intent.putExtra("host", (String)map.get("host"));
                intent.putExtra("isGameStart", (boolean)map.get("isGameStart"));
                intent.putExtra("quizHost", (String)map.get("quizHost"));
                startActivity(intent);
                finish();
                break;

            case "wrongKey": //존재하지 않는 게임 방 입장
                Toast.makeText(MainActivity.this, "존재하지 않는 게임 방 입니다.", Toast.LENGTH_SHORT).show();
                break;

            case "fullRoom": //가득 찬 게임 방 입장
                Toast.makeText(MainActivity.this, "인원이 가득 찬 게임 방 입니다.", Toast.LENGTH_SHORT).show();
                break;

            case "wrongPassword": //비밀번호 오류 게임 방 입장
                Toast.makeText(MainActivity.this, "비밀번호가 틀렸습니다.", Toast.LENGTH_SHORT).show();
                break;

            case "fail": //게임 방 입장 중 오류
                Toast.makeText(MainActivity.this, "게임 방 입장에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    //게임 방 목록 요청 후  행동
    private void receiveGameRoomList(Object jsonObj){
        try {
            JSONArray jsonArray = (JSONArray)jsonObj;
            client_.gameRoomListViewAdapter.removeAll();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject object = jsonArray.getJSONObject(i);
                String key = object.getString("key"); //방방 고유 키값
                String name = object.getString("name"); //방 제목
                boolean isLock = Boolean.valueOf(object.getString("isLock")); //방 잠금 유무
                int num = Integer.valueOf(object.getString("num")); //방 현재 인원
                int maxNum = Integer.valueOf(object.getString("maxNum")); //방 최대 인원
                boolean isStart = Boolean.valueOf(object.getString("isStart")); //방 상태

                //int no, String name, boolean isLock, int num, int maxNum, boolean isStart
                GameRoomListViewItem item = new GameRoomListViewItem(key, name, isLock, num, maxNum, isStart);
                client_.gameRoomListViewAdapter.addItem(item);
            }
            client_.gameRoomListViewAdapter.notifyDataSetChanged();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //새로 생성된 방 목록 갱신
    private void receiveCreateNewGameRoom(Object jsonObj){
        try {
            JSONObject json = (JSONObject)jsonObj;
            String key = (String)json.get("key"); //방 키값
            String name = (String)json.get("name"); //방 제목
            boolean isLock = (Boolean)json.get("isLock"); //방 잠금 유무
            int num = (Integer)json.get("num"); //방 인원
            int maxNum = (Integer)json.get("maxNum"); //방 최대 인원
            boolean isStart = (Boolean)json.get("isStart"); //방 상태

            GameRoomListViewItem gameRoom = new GameRoomListViewItem(key, name, isLock, num, maxNum, isStart);
            client_.gameRoomListViewAdapter.addItem(gameRoom);
            client_.gameRoomListViewAdapter.notifyDataSetChanged();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //채팅 방 생성 요청 후 행동
    private void receiveCreateChatRoom(Object jsonObj){
        HashMap<String, Object> map = new HashMap<String, Object>();
        try {
            JSONObject json = (JSONObject)jsonObj;
            map.put("result", json.get("result")); //결과
            map.put("key", json.get("key")); //방 키값
            map.put("name", json.get("name")); //방 이름
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String result = (String)map.get("result");
        if(result.equals("success")){ //채팅 방 만들기 성공
            String roomKey = (String)map.get("key");
            String roomName = (String)map.get("name");

            //방 리스트에 뜨는 친구 닉네임 설정
            String accountNick = "";
            ChatRoomListViewItem chatRoomListViewItem = client_.chatRoomListViewAdapter.getItem(roomKey);
            if(chatRoomListViewItem == null){ //만들 채팅 방 키값을 가진 방이 없는 경우 (채팅방 리스트에 추가, 해당 채팅방에 채팅 데이터 입력)

                String[] accountNickArray = roomName.split(",");
                for(int i = 0; i < accountNickArray.length; i++){
                    if(!accountNickArray[i].equals(client_.account_.nick_)) {
                        accountNick += accountNickArray[i];
                        if (i < accountNickArray.length - 1)
                            accountNick += ",";
                    }
                }
            }
            Intent intent = new Intent(MainActivity.this, ChatRoomActivity.class);
            intent.putExtra("key", roomKey); //채팅방 키값 전송
            intent.putExtra("roomName", accountNick); //채팅방 이름
            startActivity(intent);
            finish();
        }else{ //채팅 방 만들기 실패
            Toast.makeText(MainActivity.this, "채팅 방 생성에 실패하였습니다.", Toast.LENGTH_SHORT).show();
        }
    }
};