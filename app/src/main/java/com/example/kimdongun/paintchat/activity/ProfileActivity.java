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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.kimdongun.paintchat.Account;
import com.example.kimdongun.paintchat.Client;
import com.example.kimdongun.paintchat.CustomBitmapPool;
import com.example.kimdongun.paintchat.DebugHandler;
import com.example.kimdongun.paintchat.HandlerMessageDecode;
import com.example.kimdongun.paintchat.JsonEncode;
import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.SQLiteHandler;
import com.example.kimdongun.paintchat.item.ChatRoomListViewItem;
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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

import jp.wasabeef.glide.transformations.BlurTransformation;
import jp.wasabeef.glide.transformations.CropCircleTransformation;

public class ProfileActivity extends AppCompatActivity implements HandlerMessageDecode, View.OnClickListener, SocketServiceExecutor{
    private ImageView imageView_back; //배경 사진
    private ImageView imageView_profile; //프로필 사진
    private TextView textView_nick; //닉네임
    private TextView textView_id; //아이디
    private TextView textView_email; //이메일
    private LinearLayout layout_email; //이메일 레이아웃 (자신 프로필 화면에만 이메일 보이게 하기 위함)

    private LinearLayout layout_social; //친구 프로필 볼 때 보여지는 레이아웃
    private ImageView imageView_chat; //1:1 채팅 버튼

    private LinearLayout layout_none_social; //친구 아닌 사람 프로필 볼 떄 보여지는 레이아웃
    private ImageView imageView_add_social; //친구 추가 버튼

    private LinearLayout layout_mine; //자신 프로필 볼 때 보여지는 레이아웃
    private ImageView imageView_profile_fix; //프로필 수정 버튼

    private Client client_; //계정 클라이언트

    private String accountId_; //계정 아이디
    private Account account_; //프로필 볼 계정 정보

    private Handler handler_; //소켓 스레드랑 통신 할 핸들러
    private SocketService socketService_; //소켓 서비스
    private ServiceConnection serviceConnection_; //서비스 커넥션
    private boolean isLiveBinder; //서비스 바인드 연결 유무
    private NormalChatToast normalChatToast; //채팅 토스트 메세지

    private String fromActivity; //프로필을 오픈한 액티비티 (프로필 액티비티가 죽으면 보여줘야 할 액티비티)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("프로필");

        //초기화
        Intent intent = getIntent();
        accountId_ = intent.getStringExtra("accountId");
        final String socialType = intent.getStringExtra("type"); //계정 정보 유형 ex)mine, social, none (자신, 친구, 모르는 사람)
        fromActivity = intent.getStringExtra("from"); //프로필을 오픈한 액티비티 (프로필 액티비티가 죽으면 보여줘야 할 액티비티)

        //초기화
        handler_ = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                String str = msg.obj.toString();
                if(str.equals("connectSuccess")){ //소켓 연결 성공
                    Toast.makeText(ProfileActivity.this, "소켓 서버와 연결하였습니다.", Toast.LENGTH_SHORT).show();
                }else if(str.equals("connectFail")){ //소켓 연결 실패
                    Toast.makeText(ProfileActivity.this, "소켓 서버와 연결을 실패하였습니다.", Toast.LENGTH_SHORT).show();
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

        if(!fromActivity.equals("chat")) {
            Intent serviceIntent = new Intent(ProfileActivity.this, SocketService.class);
            bindService(serviceIntent, serviceConnection_, Context.BIND_AUTO_CREATE); //intent객체, 서비스와 연결에 대한 정의
        }

        client_ = Client.getInstance(this);

        imageView_back = (ImageView)findViewById(R.id.imageView_back);
        imageView_profile = (ImageView)findViewById(R.id.imageView_profile);
        textView_nick = (TextView)findViewById(R.id.textView_nick);
        textView_id = (TextView)findViewById(R.id.textView_id);
        textView_email = (TextView)findViewById(R.id.textView_email);
        layout_email = (LinearLayout)findViewById(R.id.layout_email);

        layout_social = (LinearLayout)findViewById(R.id.layout_social);
        imageView_chat = (ImageView)findViewById(R.id.imageView_chat);

        layout_none_social = (LinearLayout)findViewById(R.id.layout_none_social);
        imageView_add_social = (ImageView)findViewById(R.id.imageView_add_social);

        layout_mine = (LinearLayout)findViewById(R.id.layout_mine);
        imageView_profile_fix = (ImageView)findViewById(R.id.imageView_profile_fix);

        //배경 이미지 꽉 차게 설정
        imageView_back.setScaleType(ImageView.ScaleType.FIT_XY);
        //프로필 사진 창 크기 변경
        imageView_profile.getLayoutParams().height = this.getWindowManager().getDefaultDisplay().getWidth() / 4;
        imageView_profile.getLayoutParams().width = imageView_profile.getLayoutParams().height;

        //type에 따라 아래쪽 버튼 활성화 초기화
        if(socialType.equals("mine")){ //자신의 프로필 일떄 레이아웃 보이는거 설정
            layout_email.setVisibility(View.VISIBLE);
            layout_social.setVisibility(View.INVISIBLE);
            layout_none_social.setVisibility(View.INVISIBLE);
            layout_mine.setVisibility(View.VISIBLE);
        }else if(socialType.equals("social")){ //친구의 프로필 일떄 레이아웃 보이는거 설정
            layout_email.setVisibility(View.INVISIBLE);
            layout_social.setVisibility(View.VISIBLE);
            layout_none_social.setVisibility(View.INVISIBLE);
            layout_mine.setVisibility(View.INVISIBLE);
        }else if(socialType.equals("recommend")){ //추천 친구 사람 프로필 일떄 레이아웃 보이는거 설정
            layout_email.setVisibility(View.INVISIBLE);
            layout_social.setVisibility(View.INVISIBLE);
            layout_none_social.setVisibility(View.VISIBLE);
            layout_mine.setVisibility(View.INVISIBLE);
        }

        //채팅 화면에서 프로필 사진으로 넘어왔으면 채팅 관련 버튼 비활성화
        if(fromActivity.equals("chat")){
            layout_social.setVisibility(View.INVISIBLE);
        }

        //자신의 프로필 정보 볼 경우
        if(client_.account_.id_.equals(accountId_)) {
            Glide.with(this)
                    .load(client_.account_.profileUrl_ + "Thumb.png")
                    .error(R.mipmap.ic_launcher)
                    .centerCrop()
                    .bitmapTransform(new CropCircleTransformation(this))
                    .into(imageView_profile);

            Glide.with(this)
                    .load(client_.account_.profileUrl_ + "Thumb.png")
                    .thumbnail(0.1f)
                    .error(R.mipmap.ic_launcher)
                    .bitmapTransform(new BlurTransformation(ProfileActivity.this))
                    .into(imageView_back);

            textView_nick.setText(client_.account_.nick_);
            textView_id.setText(client_.account_.id_);
            textView_email.setText(client_.account_.email_);

        }else{ //다른사람 프로필 정보 볼 경우
            //계정 정보 서버로 받아옴
            HashMap<String, String> postData = new HashMap<String, String>();
            postData.put("search_id", accountId_);

            PostDB postDB = new PostDB();
            postDB.putFileData("select_social.php", postData, null, null, new OnFinishDBListener() {
                @Override
                public void onSuccess(String output) {
                    try {
                        DebugHandler.log(getClass().getName(), "output: " + output);
                        JSONObject reader = new JSONObject(output);
                        JSONArray socialArray = reader.getJSONArray("account");
                        //json값 변환
                        for (int i = 0; i < socialArray.length(); i++) {
                            JSONObject object = socialArray.getJSONObject(i);
                            String id = object.getString("id"); //계정 아이디
                            String profile = object.getString("profile"); //계정 프로필
                            String email = object.getString("email"); //계정 프로필
                            String nick = object.getString("nick"); //계정 닉네임
                            String type = object.getString("type"); //계정 유형

                            account_ = new Account(id, profile, email, nick, type, 1, 1, 1);

                            if(socialType.equals("social")){ //친구의 프로필
                                client_.socialListViewAdapter.getItemById(account_.id_).profileUrl_ = account_.profileUrl_;
                                client_.socialListViewAdapter.getItemById(account_.id_).nick_ = account_.nick_;
                            }else if(socialType.equals("recommend")){ //추천 친구 사람 프로필
                                client_.recommendSocialListViewAdapter.getItemById(account_.id_).profileUrl_ = account_.profileUrl_;
                                client_.recommendSocialListViewAdapter.getItemById(account_.id_).nick_ = account_.nick_;
                            }

                            Glide.with(ProfileActivity.this)
                                    .load(account_.profileUrl_ + "Thumb.png")
                                    .error(R.mipmap.ic_launcher)
                                    .centerCrop()
                                    .bitmapTransform(new CropCircleTransformation(new CustomBitmapPool()))
                                    .into(imageView_profile);

                            Glide.with(ProfileActivity.this)
                                    .load(account_.profileUrl_ + "Thumb.png")
                                    .thumbnail(0.1f)
                                    .error(R.mipmap.ic_launcher)
                                    .bitmapTransform(new BlurTransformation(ProfileActivity.this))
                                    .into(imageView_back);


                            textView_nick.setText(account_.nick_);
                            textView_id.setText(account_.id_);
                            textView_email.setText(account_.email_);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        normalChatToast = new NormalChatToast(this); //채팅 토스트 메세지

        //터치 이벤트
        imageView_profile.setOnClickListener(this);
        imageView_chat.setOnClickListener(this);
        imageView_add_social.setOnClickListener(this);
        imageView_profile_fix.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isLiveBinder){ //서비스랑 바인드 된 경우
            unbindService(serviceConnection_);
        }
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
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.imageView_profile://프로필 사진
                Intent intent0 = new Intent(this, ImageFullActivity.class);
                String profileUrl = "";
                if(client_.account_.id_.equals(accountId_)) {
                    profileUrl = client_.account_.profileUrl_;
                }else {
                    profileUrl = account_.profileUrl_;
                }
                intent0.putExtra("url", profileUrl);
                intent0.putExtra("title", "프로필");
                startActivity(intent0);
                break;

            case R.id.imageView_chat: //1:1채팅 버튼
                createChatRoom();
                break;

            case R.id.imageView_add_social: //친구 추가 버튼
                addSocial();
                break;

            case R.id.imageView_profile_fix: //프로필 수정 버튼
                Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                intent.putExtra("fragId", 3); //메인 화면 프래그먼트 index
                startActivity(intent);
                finish();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = null;
        if(fromActivity.equals("main"))
            intent = new Intent(ProfileActivity.this, MainActivity.class);
        else if(fromActivity.equals("recommend"))
            intent = new Intent(ProfileActivity.this, RecommendSocialActivity.class);
        else if(fromActivity.equals("chat")) {
            finish();
            return;
        }
        intent.putExtra("fragId", 0); //메인 화면 프래그먼트 index
        startActivity(intent);
        finish();
    }

    /**********************************
     * addSocial(Context context, Client client) - 검색한 계정 친구 추가
     **********************************/
    public void addSocial(){
        HashMap<String, String> postData = new HashMap<String, String>();
        postData.put("id", client_.account_.id_);
        postData.put("nick", client_.account_.nick_);
        postData.put("type", client_.account_.type_);

        postData.put("social_id", account_.id_);
        postData.put("social_nick", account_.nick_);
        postData.put("social_type", account_.type_);

        PostDB postDB = new PostDB();
        postDB.putFileData("add_social.php", postData, null, null, new OnFinishDBListener() {
            @Override
            public void onSuccess(String output) {
                if (output.equals("success")) { //친구 추가 성공 -> 1:1 채팅 버튼 활성화
                    Toast.makeText(ProfileActivity.this, account_.id_ + "님을 친구로 등록 했습니다.", Toast.LENGTH_SHORT).show();
                    //친구일때 보이는 레이아웃으로 전환
                    layout_social.setVisibility(View.VISIBLE);
                    layout_none_social.setVisibility(View.INVISIBLE);
                    layout_mine.setVisibility(View.INVISIBLE);

                    //친구 목록에 계정 추가
                    client_.socialListViewAdapter.addItem(account_);
                    client_.socialListViewAdapter.notifyDataSetChanged();
                    client_.recommendSocialListViewAdapter.removeItem(account_);
                    client_.recommendSocialListViewAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(ProfileActivity.this, "친구 등록에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**********************************
     * createChatRoom() - 해당 유저와 1:1 채팅방 개설
     **********************************/
    public void createChatRoom(){
        ArrayList<String> listID = new ArrayList<String>();
        listID.add(client_.account_.id_);
        listID.add(account_.id_);

        ArrayList<String> listNick = new ArrayList<String>();
        listNick.add(client_.account_.nick_);
        listNick.add(account_.nick_);

        Collections.sort(listID);
        Collections.sort(listNick);

        //방 키값 생성
        String roomKey = "";
        //방 이름 생성
        String roomName = "";
        for(int i = 0; i < listID.size(); i++){
            roomKey += listID.get(i);
            roomName += listNick.get(i);
            if(i < listID.size() - 1){
                roomKey += ",";
                roomName += ",";
            }
        }
        roomKey.trim();
        roomName.trim();

        JSONArray jsonArr = new JSONArray();
        try {
            JSONObject keyJson = new JSONObject();
            keyJson.put("key", roomKey); //채팅방 키값
            keyJson.put("name", roomName); //방 이름
            jsonArr.put(keyJson); //roomKey, roomNick JsonArray에 추가
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //본인 제외 채팅에 참여하는 아이디 보냄
        try {
            JSONObject idJson = new JSONObject();
            idJson.put("id", account_.id_); //채팅방에 입장 할 유저 아이디
            jsonArr.put(idJson); //유저 아이디를 JsonArray에 추가
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //정보를 json으로 변환
        String jsonStr = JsonEncode.getInstance().encodeCommandJson("createChatRoom", jsonArr);
        if(isLiveBinder)
            socketService_.sendMessage(jsonStr);
    }

    @Override
    public void doReceiveAction(String request) {
        DebugHandler.log(getClass().getName(), "JSON " + request);
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

            case "createChatRoomResult": //1:1 채팅방 생성 요청 결과
                receiveCreateChatRoom(requestObj);
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

            ChatRoomListViewItem chatRoomListViewItem = client_.chatRoomListViewAdapter.getItem(roomKey);

            Intent intent = new Intent(ProfileActivity.this, ChatRoomActivity.class);
            intent.putExtra("key", roomKey); //채팅방 키값 전송
            intent.putExtra("roomName", account_.nick_); //채팅방 이름
            startActivity(intent);
            finish();
        }else{ //채팅 방 만들기 실패
            Toast.makeText(ProfileActivity.this, "채팅 방 생성에 실패하였습니다.", Toast.LENGTH_SHORT).show();
        }
    }
}
