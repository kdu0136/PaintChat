package com.example.kimdongun.paintchat.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.kimdongun.paintchat.Account;
import com.example.kimdongun.paintchat.Client;
import com.example.kimdongun.paintchat.DebugHandler;
import com.example.kimdongun.paintchat.HandlerMessageDecode;
import com.example.kimdongun.paintchat.JsonEncode;
import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.SQLiteHandler;
import com.example.kimdongun.paintchat.adapter.NormalChatListViewAdapter;
import com.example.kimdongun.paintchat.dialog.FilePickerDialog;
import com.example.kimdongun.paintchat.item.ChatRoomListViewItem;
import com.example.kimdongun.paintchat.item.NormalChatListViewItem;
import com.example.kimdongun.paintchat.server.OnFinishDBListener;
import com.example.kimdongun.paintchat.server.PostDB;
import com.example.kimdongun.paintchat.service.SocketService;
import com.example.kimdongun.paintchat.service.SocketServiceExecutor;
import com.example.kimdongun.paintchat.toast.NormalChatToast;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import static com.example.kimdongun.paintchat.activity.MainActivity.REQ_CODE_SELECT_CAMERA;
import static com.example.kimdongun.paintchat.activity.MainActivity.REQ_CODE_SELECT_CANVAS;
import static com.example.kimdongun.paintchat.activity.MainActivity.REQ_CODE_SELECT_IMAGE;
import static com.example.kimdongun.paintchat.activity.MainActivity.REQ_CODE_SELECT_VIDEO;

public class ChatRoomActivity extends AppCompatActivity implements View.OnClickListener, HandlerMessageDecode, SocketServiceExecutor, AdapterView.OnItemClickListener {
    private String roomKey; //룸 키값
    private String roomName; //채팅방 이름 (유저 닉네임)

    private ListView listView; //채팅 리스트 뷰
    private NormalChatListViewAdapter adapter; //채팅 리스트뷰 어댑터

    private Button button_file; //파일 보내기 버튼
    private Button button_send; //채팅 보내기 버튼
    private EditText editText_msg; //채팅 메세지

    private FilePickerDialog filePickerDialog_; //파일 선택 다이얼로그

    private Client client_; //클라이언트
    private Handler handler_; //소켓 스레드랑 통신 할 핸들러
    public SocketService socketService_; //소켓 서비스
    private ServiceConnection serviceConnection_; //서비스 커넥션
    public boolean isLiveBinder; //서비스 바인드 연결 유무
    private NormalChatToast normalChatToast; //채팅 토스트 메세지

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native void loadImage(String imageFileName, long img, int resizing); //이미지 불러오기
    public native void saveImage(String saveFilePath, String saveFileName, long maskImg); //이미지 저장

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        roomKey = intent.getStringExtra("key"); //룸 키값
        roomName = intent.getStringExtra("roomName"); //채팅방 이름
        DebugHandler.log(getClass().getName(), "RoomKey: " + roomKey + " RoomName: " + roomName);
        setTitle(roomName);

        //초기화
        handler_ = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                String str = msg.obj.toString();
                if(str.equals("connectSuccess")){ //소켓 연결 성공
                    Toast.makeText(ChatRoomActivity.this, "소켓 서버와 연결하였습니다.", Toast.LENGTH_SHORT).show();

                    loadChat(); //채팅 메세지 SQLite에서 불러오고 서버로 읽은 메세지 전송
                }else if(str.equals("connectFail")){ //소켓 연결 실패
                    Toast.makeText(ChatRoomActivity.this, "소켓 서버와 연결을 실패하였습니다.", Toast.LENGTH_SHORT).show();
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

                    loadChat(); //채팅 메세지 SQLite에서 불러오고 서버로 읽은 메세지 전송

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                isLiveBinder = false;
            }
        };

        Intent serviceIntent = new Intent(ChatRoomActivity.this, SocketService.class);
        bindService(serviceIntent, serviceConnection_, Context.BIND_AUTO_CREATE); //intent객체, 서비스와 연결에 대한 정의

        client_ = Client.getInstance(this);

        button_file = (Button)findViewById(R.id.button_file);
        button_send = (Button)findViewById(R.id.button_send);
        editText_msg = (EditText)findViewById(R.id.editText_msg);
        listView = (ListView)findViewById(R.id.listView);
        adapter = new NormalChatListViewAdapter(this, Glide.with(this));

        listView.setAdapter(adapter);
        listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL); //리스트뷰 새 아이템 추가하면 자동 스크롤 설정

        //새로운 메세지 추가할 때, 마지막 채팅 아래 띄우기
        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(adapter.getCount() - 1);
            }
        });


        normalChatToast = new NormalChatToast(this); //채팅 토스트 메세지
        //터치 이벤트
        button_file.setOnClickListener(this);
        button_send.setOnClickListener(this);
        listView.setOnItemClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isLiveBinder){ //서비스랑 바인드 된 경우
            unbindService(serviceConnection_);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu_add_social, menu);
        return super.onCreateOptionsMenu(menu);
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
    public void onBackPressed() {
        //채팅방 리스트뷰에 보이는 정보 수정 (마지막 메세지, 날짜, 안 읽은 메세지 수)
        if(adapter.getCount() > 0) {
            ChatRoomListViewItem chatRoomListViewItem = client_.chatRoomListViewAdapter.getItem(roomKey);
            chatRoomListViewItem.msgNum_ = 0;
            NormalChatListViewItem normalChatListViewItem = (NormalChatListViewItem) adapter.getItem(adapter.getCount() - 1);
            chatRoomListViewItem.msg_ = normalChatListViewItem.msg_;
            chatRoomListViewItem.time_ = normalChatListViewItem.time_;
            client_.chatRoomListViewAdapter.notifyDataSetChanged();
        }

        Intent intent =  new Intent(ChatRoomActivity.this, MainActivity.class);
        intent.putExtra("fragId", 2); //메인 화면 프래그먼트 index
        startActivity(intent);
        finish();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button_file: //파일 전송
                filePickerDialog_ = new FilePickerDialog(this);
                filePickerDialog_.show();
                break;

            case R.id.button_send: //채팅 전송
                String msg = editText_msg.getText().toString();

                if(msg.length() < 1){
                    return;
                }
                //채팅 정보를 json으로 변환
                String[] keys = {"roomKey", "id", "nick", "msg", "type"};
                Object[] values = {roomKey, client_.account_.id_, client_.account_.nick_, msg, "chat"};
                String jsonStr = JsonEncode.getInstance().encodeCommandJson("normalChat", keys, values);
                if(isLiveBinder)
                    socketService_.sendMessage(jsonStr);

                editText_msg.setText("");

                break;
        }
    }

    @Override
    public void onActivityResult(final int requestCode, int resultCode, Intent data) {
        DebugHandler.log(getClass().getName(), "requestCode: " + requestCode);
        if(resultCode== Activity.RESULT_OK)
        {
            String filePath = ""; // path 경로
            if(requestCode == REQ_CODE_SELECT_IMAGE || requestCode == REQ_CODE_SELECT_VIDEO) { //갤러리에서 액션 받은 경우
                Uri galleryImgUri = data.getData();
                filePath = getRealPathFromURI(galleryImgUri); // path 경로
            }else if(requestCode == REQ_CODE_SELECT_CANVAS || requestCode == REQ_CODE_SELECT_CAMERA) { //캔버스 또는 카메라 에서 액션 받은 경우
                filePath = data.getStringExtra("filePath");
            }
            if(filePath.equals("")){ //그냥 종료
                return;
            }

            filePickerDialog_.dismiss();
            uploadFile(filePath, requestCode); //파일 전송
        }
        DebugHandler.log(getClass().getName(), "onActivityResult End");
    }

    private void uploadFile(final String filePath, final int requestCode){
        String extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new File(filePath)).toString()); //파일 확장자명

        DebugHandler.log(getClass().getName(), "RealPath: " + filePath);
        DebugHandler.log(getClass().getName(), "extension: " + extension);

        long now = System.currentTimeMillis();
//        final String fileName = client_.account_.id_ + "_" + now + ".png"; //현재 시간을 받아서 파일명  생성
        final String fileName = client_.account_.id_ + "_" + now + "." + extension; //현재 시간을 받아서 파일명  생성
        HashMap<String, String> postData = new HashMap<>();
        postData.put("filePath", "/usr/share/nginx/html/chat_file/");
        PostDB postDB = new PostDB(this, "업로드 중입니다...");
        postDB.putFileData("upload_file.php", postData, filePath, fileName, new OnFinishDBListener() {
            @Override
            public void onSuccess(String output) {
                DebugHandler.log("ProfileUploadResult", output);
                if(output.equals("success")){ //파일 업로드 성공했으면 채팅으로 파일 url전송
                    makeAndUploadThumbnail(filePath, fileName, requestCode); //썸네일 생성

                    //캐시 파일 삭제
                    if(requestCode == REQ_CODE_SELECT_CANVAS) {
                        if (new File(filePath).exists()) {
                            DebugHandler.log(getClass().getName(), "DeleteFile: " + filePath);
                            new File(filePath).delete();
                        }
                    }
                }
            }
        });
    }

    private void makeAndUploadThumbnail(String filePath, final String uploadFileName, final int requestCode){
        Mat img_input = new Mat(); //이미지 파일 저장할 Mat
        final String extStorageDirectory = getCacheDir().getAbsolutePath(); //저장 경로 (캐시)
        final String saveFileName = "thumbnail.png"; //저장 할 이름
        if(requestCode == REQ_CODE_SELECT_VIDEO){ //비디오 파일 썸네일
            Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(filePath, MediaStore.Video.Thumbnails.FULL_SCREEN_KIND); //썸네일 생성
            Utils.bitmapToMat(bitmap, img_input); //생성한 썸네일 Mat 타입으로 변경

            saveImage(extStorageDirectory, saveFileName, img_input.getNativeObjAddr()); //동영상 썸네일 이미지 캐시에 저장
            String tempFilePath = extStorageDirectory + "/" + saveFileName;

            loadImage(tempFilePath, img_input.getNativeObjAddr(), 300); //이미지 캐시에서 불러와서 리사이징
        }else{ //이미지 파일 썸네일
            loadImage(filePath, img_input.getNativeObjAddr(), 300); //이미지 불러오기
        }
        saveImage(extStorageDirectory, saveFileName, img_input.getNativeObjAddr()); //이미지 캐시에 저장
        final String uploadFilePath = extStorageDirectory + "/" + saveFileName;

        HashMap<String, String> postData = new HashMap<>();
        postData.put("filePath", "/usr/share/nginx/html/chat_file/");
        PostDB postDB = new PostDB();
        //썸네일 이미지 서버로 전송
        postDB.putFileData("upload_file.php", postData, uploadFilePath, uploadFileName + "Thumb.png", new OnFinishDBListener() {
            @Override
            public void onSuccess(String output) {
                DebugHandler.log("ThumbnailUploadResult", output);
                if(output.equals("success")) { //파일 업로드 성공

                    //채팅 정보를 json으로 변환
                    String[] keys = {"roomKey", "id", "nick", "msg", "type"};
                    String type = "image";
                    if(requestCode == REQ_CODE_SELECT_VIDEO)
                        type = "video";
//                    String type = "file";
                    Object[] values = {roomKey, client_.account_.id_, client_.account_.nick_, "http://211.110.229.53/chat_file/" + uploadFileName, type};
                    String jsonStr = JsonEncode.getInstance().encodeCommandJson("normalChat", keys, values);
                    if (isLiveBinder)
                        socketService_.sendMessage(jsonStr);

                    if (new File(uploadFilePath).exists()){ //캐시에서 썸네일 삭제
                        DebugHandler.log(getClass().getName(), "DeleteFile: " + uploadFilePath);
                        new File(uploadFilePath).delete();
                    }
                }
            }
        });
    }

    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    public void loadChat(){
        SQLiteHandler sqLiteHandler = new SQLiteHandler(this, "project_one", null, 1);
        String[] whereNames = {"room_key", "my_id"};
        Object[] whereValues = {roomKey, client_.account_.id_};
        ArrayList<ArrayList<Object>> accountArray = sqLiteHandler.select("chat_message", whereNames, whereValues, "and");

        String nonReadChatDate = ""; //읽지 않은 메세지의 날짜 받아 놓을 리스트 (날짜가 인덱스로 사용)
        for(int i = 0; i < accountArray.size(); i++){
            String id = (String)accountArray.get(i).get(2); //계정 아이디
            String msg = (String)accountArray.get(i).get(3); //채팅 내용
            long now = Long.valueOf((String)accountArray.get(i).get(4)); //채팅 날짜,시간
            long readNum = Long.valueOf((String)accountArray.get(i).get(5)); //읽은 수
            long isRead = Long.valueOf((String)accountArray.get(i).get(6)); //읽은 메세지인가 0-읽지 않음 1-읽음
            String type = (String)accountArray.get(i).get(7); //타입  ex)chat / file
            if(isRead == 0 && !client_.account_.id_.equals(id)){
                nonReadChatDate += now + ","; //읽지 않은 메세지 시간(인덱스) 저장
            }

            boolean myMsg = client_.account_.id_.equals(id); //메시지 주인이 자기면 true 아니면 false
            Date date = new Date(now);
            SimpleDateFormat sdfDate = new SimpleDateFormat("------- yyyy년 MM월 dd일 E요일 -------");
            SimpleDateFormat sdfTime = new SimpleDateFormat("aa hh:mm");
            String strDate = sdfDate.format(date);
            String strTime = sdfTime.format(date);

            Account account = client_.socialListViewAdapter.getItemById(id);

//        Drawable accountProfile, String accountId, String accountNick,
//                String msg, String date, String time, int num, boolean myMsg
            NormalChatListViewItem item = new NormalChatListViewItem(account, msg, strDate, strTime, now, (int)readNum, myMsg, type);

            adapter.addItem(item);
        }
        adapter.notifyDataSetChanged();

        //읽지 않은 메세지 있는 경우 해당 메세지 날짜(인덱스) 서버에 전송
        if(!nonReadChatDate.equals("")){
            String[] keys = {"roomKey", "date"};
            Object[] values = {roomKey, nonReadChatDate};
            String jsonStr = JsonEncode.getInstance().encodeCommandJson("readChat", keys, values);
            if(isLiveBinder)
                socketService_.sendMessage(jsonStr);
        }

        if(socketService_.socketServiceThread_ != null
                && socketService_.socketServiceThread_.aliveThread) { //소켓 연결이 재대로 되어 있을 경우
            //채팅방 리스트뷰에 보이는 정보 수정 (마지막 메세지, 날짜, 안 읽은 메세지 수)
            if (adapter.getCount() > 0) {
                ChatRoomListViewItem chatRoomListViewItem = client_.chatRoomListViewAdapter.getItem(roomKey);
                chatRoomListViewItem.msgNum_ = 0;
                NormalChatListViewItem normalChatListViewItem = (NormalChatListViewItem) adapter.getItem(adapter.getCount() - 1);
                chatRoomListViewItem.msg_ = normalChatListViewItem.msg_;
                chatRoomListViewItem.time_ = normalChatListViewItem.time_;
                client_.chatRoomListViewAdapter.notifyDataSetChanged();
            }

            //SQLite에 메세지 읽음으로 변경
            String[] updateColumnName = {"is_read"};
            String[] updateColumnValue = {String.valueOf(1)};
            String[] whereColumnName = {"my_id", "room_key"};
            String[] whereColumnValue = {client_.account_.id_, roomKey};
            sqLiteHandler.update("chat_message", updateColumnName, updateColumnValue, whereColumnName, whereColumnValue, "and");
        }
    }

    @Override
    public void doReceiveAction(String request){
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
            map.put("type", json.get("type")); //타입  ex)chat / file
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String receiveRoomKey = (String)map.get("roomKey");
        Date date = new Date((long)map.get("date"));
        SimpleDateFormat sdfDate = new SimpleDateFormat("------- yyyy년 MM월 dd일 E요일 -------");
        SimpleDateFormat sdfTime = new SimpleDateFormat("aa hh:mm");
        String strDate = sdfDate.format(date);
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
        client_.chatRoomListViewAdapter.addTopItem(chatRoomListViewItem);
        client_.chatRoomListViewAdapter.notifyDataSetChanged();

        if(!this.roomKey.equals(receiveRoomKey)) { //받은 메세지가 해당 채팅방 메세지가 아닌 경우 메세지를 해당 방 체팅 리스트에 추가 안함 -> 토스트 메세지 띄움
            Account account = client_.socialListViewAdapter.getItemById((String)map.get("id"));
            if(account == null){
                account = client_.recommendSocialListViewAdapter.getItemById((String)map.get("id"));
            }
            if(account == null){
                account = new Account((String)map.get("id"), "http://211.110.229.53/profile_image/default.png", null, (String)map.get("nick"),
                        null, 1, 1, 1);
            }

            normalChatToast.showToast(account.profileUrl_, account.nick_, (String)map.get("msg"));

            return;
        }

        if(!client_.account_.id_.equals((String)map.get("id"))){ //자신의 채팅 메세지 아닌 경우 서버로 해당 메세지 읽었다고 알려주고 해당 메세지 읽음으로 변경
            String[] keys = {"roomKey", "date"};
            Object[] values = {roomKey, map.get("date")};
            String jsonStr = JsonEncode.getInstance().encodeCommandJson("readChat", keys, values);
            if(isLiveBinder)
                socketService_.sendMessage(jsonStr);
        }

        boolean myMsg = client_.account_.id_.equals((String)map.get("id")); //메시지 주인이 자기면 true 아니면 false

//        Drawable accountProfile, String accountId, String accountNick,
//                String msg, String date, String time, int num, boolean myMsg
        Account account = client_.socialListViewAdapter.getItemById((String)map.get("id"));
        NormalChatListViewItem item = new NormalChatListViewItem(account,
                (String)map.get("msg"), strDate, strTime, (long)map.get("date"), (int)map.get("num"), myMsg, (String)map.get("type"));

        adapter.addItem(item);
        adapter.notifyDataSetChanged();

        //chat_message 테이블 업데이트 (읽음 으로 변경)
        SQLiteHandler sqLiteHandler = new SQLiteHandler(this, "project_one", null, 1);
        String[] updateColumnName = {"is_read"};
        String[] updateColumnValue = {String.valueOf(1)};
        String[] whereColumnName = {"my_id", "room_key", "date"};
        String[] whereColumnValue = {client_.account_.id_, roomKey, String.valueOf(map.get("date"))};
        sqLiteHandler.update("chat_message", updateColumnName, updateColumnValue, whereColumnName, whereColumnValue, "and");
    }

    @Override
    public void receiveReadChat(Object jsonObj) {
        String dates = null;//읽은 채팅의 날짜들(인덱스)
        try {
            JSONObject json = (JSONObject)jsonObj;
            dates = json.getString("date"); //읽은 채팅의 날짜들(인덱스)
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String[] dateList = dates.split(","); //날짜를 , 로 구분지어서 분리
        for(int i = 0; i < dateList.length; i++){
            NormalChatListViewItem item = adapter.getItem(Long.valueOf(dateList[i]));
            if(item != null)
                item.num_ -= 1; //읽음 숫자 -1 해줌
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void receiveExitServerResult() {
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
        NormalChatListViewItem item = (NormalChatListViewItem)adapter.getItem(index);
        if(item.type_.equals("image")){ //사진 파일이면 크게보기
            Intent intent0 = new Intent(this, ImageFullActivity.class);
            intent0.putExtra("url", item.msg_);
            intent0.putExtra("title", "사진");
            startActivity(intent0);
        }else if(item.type_.equals("video")) { //비디오 파일이면 비디오 재생 화면으로
            Intent intent0 = new Intent(this, VideoFullActivity.class);
            intent0.putExtra("url", item.msg_);
            startActivity(intent0);
        }else{ //메세지면 프로필 화면으로
            if(item.myMsg_) //자신의 메세지 터치하면 아무일도 일어나지 않음
                return;
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.putExtra("accountId", item.account_.id_);
            intent.putExtra("type", "social");
            intent.putExtra("from", "chat");
            startActivity(intent);
        }
    }
}
