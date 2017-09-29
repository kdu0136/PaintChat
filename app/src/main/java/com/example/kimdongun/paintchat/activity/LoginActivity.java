package com.example.kimdongun.paintchat.activity;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kimdongun.paintchat.Account;
import com.example.kimdongun.paintchat.Client;
import com.example.kimdongun.paintchat.DebugHandler;
import com.example.kimdongun.paintchat.HandlerMessageDecode;
import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.SQLiteHandler;
import com.example.kimdongun.paintchat.item.ChatRoomListViewItem;
import com.example.kimdongun.paintchat.network.Network;
import com.example.kimdongun.paintchat.server.OnFinishDBListener;
import com.example.kimdongun.paintchat.server.PostDB;
import com.example.kimdongun.paintchat.service.SocketService;
import com.example.kimdongun.paintchat.service.SocketServiceExecutor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

//로그인 화면
public class LoginActivity extends AppCompatActivity implements View.OnClickListener, HandlerMessageDecode, SocketServiceExecutor {
    private AutoCompleteTextView editText_id; //입력 id
    private EditText editText_password; //입력 password
    private TextView textView_find_id; //아이디 찾기 버튼
    private TextView textView_find_password; //비밀번호 찾기 버튼
    private Button button_login; //로컬 로그인 버튼
    private Button button_login_google; //구글 로그인 버튼
    private Button button_login_facebook; //페이스북 로그인 버튼
    private Button button_registration; //회원가입 버튼
    private Client client_; //계정 클라이언트

    private Handler handler_; //소켓 스레드랑 통신 할 핸들러
    private SocketService socketService_; //소켓 서비스
    private ServiceConnection serviceConnection_; //서비스 커넥션
    private boolean isLiveBinder; //서비스 바인드 연결 유무

    private int fragId_ = 0; //메인화면 초기 프래그먼트 위치

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        Intent serviceIntent = new Intent(LoginActivity.this, SocketService.class);
//        stopService(serviceIntent);

        handler_ = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                String str = msg.obj.toString();
                if (str.equals("connectSuccess") || str.equals("connectFail")) { //소켓 연결 성공에 관련된 메세지 온 경우
                    if(str.equals("connectSuccess")){ //소켓 연결 성공
                        Toast.makeText(LoginActivity.this, "채팅 서버와 연결하였습니다.", Toast.LENGTH_SHORT).show();
                    }else{ //소켓 연결 실패
                        Toast.makeText(LoginActivity.this, "채팅 서버와 연결을 실패하였습니다.", Toast.LENGTH_SHORT).show();
                    }
                    Toast.makeText(getApplicationContext(), "'" + client_.account_.id_ + "'님 반갑습니다.", Toast.LENGTH_SHORT).show();

                    DebugHandler.log(getClass().getName(), "수동(소켓 서비스 메세지 받고) 로그인");
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.putExtra("fragId", fragId_);
                    startActivity(intent);
                    finish();
                }else{
                    DebugHandler.log(getClass().getName(), str);
                    doReceiveAction(str);
                }
            }
        };

        serviceConnection_ = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                try{
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

        Intent fragIntent = getIntent();
        fragId_ = fragIntent.getIntExtra("fragId", 0); //메인 엑티비티로 넘어갈 때 초기 프래그먼트 인덱스

        if(loadLoginRecord()){ //계정 정보가 저장되어 있으면 정보를 불러오고 메인으로 넘어가기
            DebugHandler.log(getClass().getName(), "자동 로그인");

            if(isMyServiceRunning(SocketService.class)){ //현재 소켓서비스가 실행 중이면 메인엑티비티로 이동
                Toast.makeText(getApplicationContext(), "'" + client_.account_.id_ + "'님 반갑습니다.", Toast.LENGTH_SHORT).show();
                DebugHandler.log("SocketService Live Check", "IsAlive");
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("fragId", fragId_);
                startActivity(intent);
                finish();
            }else{ //소켓 서비스가 죽어 있으면 서비스 실행 -> 서비스에서 메세지(소켓 연결 성공 유무) 받고 메인으로 넘어감
                DebugHandler.log("SocketService Live Check", "Dead");
                Intent serviceIntent = new Intent(LoginActivity.this, SocketService.class);
                startService(serviceIntent); //소켓 서비스 시작
                bindService(serviceIntent, serviceConnection_, Context.BIND_AUTO_CREATE); //intent객체, 서비스와 연결에 대한 정의
            }
        }

        setContentView(R.layout.activity_login);

        //초기화
        editText_id = (AutoCompleteTextView)findViewById(R.id.editText_id);
        editText_password = (EditText)findViewById(R.id.editText_password);
        textView_find_id = (TextView)findViewById(R.id.textView_find_id);
        textView_find_password = (TextView)findViewById(R.id.textView_find_password);
        button_login = (Button)findViewById(R.id.button_login);
        button_login_google = (Button)findViewById(R.id.button_login_google);
        button_login_facebook = (Button)findViewById(R.id.button_login_facebook);
        button_registration = (Button)findViewById(R.id.button_registration);

        //터치 이벤트
        textView_find_id.setOnClickListener(this);
        textView_find_password.setOnClickListener(this);
        button_login.setOnClickListener(this);
        button_login_google.setOnClickListener(this);
        button_login_facebook.setOnClickListener(this);
        button_registration.setOnClickListener(this);
    }

    //해당 서비스가 실행중인지 판별해주는 함수
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isLiveBinder){ //서비스랑 바인드 된 경우
            unbindService(serviceConnection_);
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.textView_find_id: //아이디 찾기
                Toast.makeText(getApplicationContext(), "아이디 찾기", Toast.LENGTH_SHORT).show();
                break;

            case R.id.textView_find_password: //패스워드 찾기
                Toast.makeText(getApplicationContext(), "패스워드 찾기", Toast.LENGTH_SHORT).show();
                break;

            case R.id.button_login: //로그인
                String id = editText_id.getText().toString();
                String pass = editText_password.getText().toString();
                if(id.length() < 1) { //ID 입력 안 했을 경우 메세지
                    Toast.makeText(getApplicationContext(), "ID를 입력하세요", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(pass.length() < 1) { //Password 입력 안 했을 경우 메세지
                    Toast.makeText(getApplicationContext(), "Password를 입력하세요", Toast.LENGTH_SHORT).show();
                    return;
                }
                requestLogin();
                break;

            case R.id.button_login_google: //구글 로그인
                Toast.makeText(getApplicationContext(), "구글 로그인", Toast.LENGTH_SHORT).show();
                break;

            case R.id.button_login_facebook: //페이스북 로그인
                Toast.makeText(getApplicationContext(), "페이스북 로그인", Toast.LENGTH_SHORT).show();
                break;

            case R.id.button_registration: //회원가입
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder alert_confirm = new AlertDialog.Builder(LoginActivity.this);
        alert_confirm.setMessage("어플을 종료 하시겠습니까?").setCancelable(false).setPositiveButton("확인",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LoginActivity.super.onBackPressed();
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

    /*****************************************************
     * requestLogin() - 계정 로그인 요청 함수
     *****************************************************/
    private void requestLogin(){
        String id = editText_id.getText().toString();
        String password = editText_password.getText().toString();

        //네트워크 연결 체크
        String netState = Network.getWhatKindOfNetwork(this);
        if(netState.equals(Network.NONE_STATE)){ //연결 안 되있는 경우
            Network.connectNetwork(this);
        }else { //연결 되있는 경우
            HashMap<String, String> postData = new HashMap<String, String>();
            postData.put("id", id);
            postData.put("password", password);
            postData.put("type", "local");

            PostDB postDB = new PostDB();
            postDB.putFileData("login.php", postData, null, null, new OnFinishDBListener() {
                @Override
                public void onSuccess(String output) {
                    if(output.equals("null")){ //잘못된 로그인 요청
                        Toast.makeText(getApplicationContext(), "아이디 또는 비밀번호를 확인하세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }else if(output.equals("other")){ //다른 기기에서 로그인 중 계정
                        Toast.makeText(getApplicationContext(), "다른 기기에서 접속 중인 계정입니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    //로그인 성공
                    successLogin(output);
                }
            });
        }
    }

    /*****************************************************
     * successLogin(String output) - 로그인 성공 후 작업
     * output - 계정 정보
     *****************************************************/
    private void successLogin(String output){
        try {
            DebugHandler.log(getClass().getName(), "output: " + output);
            JSONObject reader = new JSONObject(output);
            JSONArray objects = reader.getJSONArray("account");
            //json값 변환
            for (int i = 0; i < objects.length(); i++) {
                JSONObject object = objects.getJSONObject(i);
                String id = object.getString("id"); //계정 아이디
                String profile = object.getString("profile"); //계정 프로필
                String email = object.getString("email"); //계정 이메일
                String nick = object.getString("nick"); //계정 닉네임
                String type = object.getString("type"); //계정 유형
                int confirm = Integer.valueOf(object.getString("confirm")); // 1 이면 인증 완료 계정 0 이면 미 인증 계정
                int search = Integer.valueOf(object.getString("search")); // 1 이면 검색 허용 계정 0 이면 검색 거부
                int push = Integer.valueOf(object.getString("push")); // 1 이면 푸시 허용 계정 0 이면 미 푸시 거부

                //계정 초기화
                client_ = null;
                client_ = Client.getInstance(this);
                client_.loginClient(id, profile, email, nick, type, confirm, search, push);

                //SQLite에 계정 정보 입력
                SQLiteHandler sqLiteHandler = new SQLiteHandler(this, "project_one", null, 1);
                Object[] arrayList = {client_.account_.id_, client_.account_.profileUrl_, client_.account_.email_,
                        client_.account_.nick_, client_.account_.type_, client_.account_.confirm_, client_.account_.search_, client_.account_.push_};
                sqLiteHandler.insert("account_info", arrayList);

                Intent serviceIntent = new Intent(LoginActivity.this, SocketService.class);
                stopService(serviceIntent); //소켓 서비스 종료 후 시작하기 위함
                startService(serviceIntent); //소켓 서비스 시작
                bindService(serviceIntent, serviceConnection_, Context.BIND_AUTO_CREATE); //intent객체, 서비스와 연결에 대한 정의
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*****************************************************
     * loadLoginRecord() - 로그인 내역 있는지 확인 후 있으면 계정 정보 불러오기
     *****************************************************/
    private boolean loadLoginRecord(){
        SQLiteHandler sqLiteHandler = new SQLiteHandler(this, "project_one", null, 1);
        ArrayList<ArrayList<Object>> accountArray = sqLiteHandler.select("account_info");
        if(accountArray.size() > 0){ //로그인 기록 있음
            DebugHandler.log(getClass().getName(), "로그인 기록 있음");
            //계정 초기화
            client_ = Client.getInstance(this);

            String id = (String)accountArray.get(0).get(0); //계정 아이디
            String profile = (String)accountArray.get(0).get(1); //계정 프로필
            String email = (String)accountArray.get(0).get(2); //계정 이메일
            String nick = (String)accountArray.get(0).get(3); //계정 닉네임
            String type = (String)accountArray.get(0).get(4); //계정 유형
            int confirm = Integer.valueOf((String)accountArray.get(0).get(5)); // 1 이면 인증 완료 계정 0 이면 미 인증 계정
            int search = Integer.valueOf((String)accountArray.get(0).get(6)); // 1 이면 검색 허용 계정 0 이면 검색 거부
            int push = Integer.valueOf((String)accountArray.get(0).get(7)); // 1 이면 푸시 허용 계정 0 이면 미 푸시 거부

            client_.loginClient(id, profile, email, nick, type, confirm, search, push);

            return true;
        }else{
            return false;
        }
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
                    receiveNormalChat(requestObj); //엑티비티에서 처리할 것 (chatRoomAdapter 수정)
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
    }

    @Override
    public void receiveReadChat(Object jsonObj) {
    }

    @Override
    public void receiveExitServerResult() {
    }
}
