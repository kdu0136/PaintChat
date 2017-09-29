package com.example.kimdongun.paintchat.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.kimdongun.paintchat.Account;
import com.example.kimdongun.paintchat.Client;
import com.example.kimdongun.paintchat.DebugHandler;
import com.example.kimdongun.paintchat.HandlerMessageDecode;
import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.SQLiteHandler;
import com.example.kimdongun.paintchat.adapter.SocialListViewAdapter;
import com.example.kimdongun.paintchat.item.ChatRoomListViewItem;
import com.example.kimdongun.paintchat.service.SocketService;
import com.example.kimdongun.paintchat.service.SocketServiceExecutor;
import com.example.kimdongun.paintchat.toast.NormalChatToast;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class RecommendSocialActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, HandlerMessageDecode, SocketServiceExecutor {
    private Client client_; //계정 클라이언트

    private ListView listView; //추천 친구 리스트뷰
    private SocialListViewAdapter recommendSocialListViewAdapter; //추천 친구 리스트 뷰 어댑터

    private Handler handler_; //소켓 스레드랑 통신 할 핸들러
    private SocketService socketService_; //소켓 서비스
    private ServiceConnection serviceConnection_; //서비스 커넥션
    private boolean isLiveBinder; //서비스 바인드 연결 유무

    private NormalChatToast normalChatToast; //채팅 토스트 메세지
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommend_social);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //초기화 //초기화

        handler_ = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                String str = msg.obj.toString();
                if(str.equals("connectSuccess")){ //소켓 연결 성공
                    Toast.makeText(RecommendSocialActivity.this, "소켓 서버와 연결하였습니다.", Toast.LENGTH_SHORT).show();
                }else if(str.equals("connectFail")){ //소켓 연결 실패
                    Toast.makeText(RecommendSocialActivity.this, "소켓 서버와 연결을 실패하였습니다.", Toast.LENGTH_SHORT).show();
                }else {
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

        Intent serviceIntent = new Intent(RecommendSocialActivity.this, SocketService.class);
        bindService(serviceIntent, serviceConnection_, Context.BIND_AUTO_CREATE); //intent객체, 서비스와 연결에 대한 정의

        client_ = Client.getInstance(this);
        recommendSocialListViewAdapter = client_.recommendSocialListViewAdapter;

        listView = (ListView)findViewById(R.id.listView);
        listView.setAdapter(recommendSocialListViewAdapter);

        setTitle("추천친구 " + recommendSocialListViewAdapter.getCount() + " 명");

        listView.setOnItemClickListener(this);

        normalChatToast = new NormalChatToast(this); //채팅 토스트 메세지
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isLiveBinder){ //서비스랑 바인드 된 경우
            unbindService(serviceConnection_);
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(RecommendSocialActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home: //홈 버튼 클릭
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
        Account account = (Account)listView.getItemAtPosition(index);
        Intent intent = new Intent(RecommendSocialActivity.this, ProfileActivity.class);
        intent.putExtra("accountId", account.id_);
        intent.putExtra("type", "recommend");
        intent.putExtra("from", "recommend");
        startActivity(intent);
        finish();
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

        String cmd = (String)arrayList.get(0); //커맨드 값
        Object requestObj = arrayList.get(1); //명령 디테일 값

        switch (cmd){
            case "normalChat": //채팅 행동
                receiveNormalChat(requestObj);
                break;

            case "readChat": //채팅 읽음 처리 행동
                receiveReadChat(requestObj); //서비스에서 처리할 것 (SQLite에 해당 message 읽은 수 줄임)
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
