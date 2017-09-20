package com.example.kimdongun.paintchat.activity;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kimdongun.paintchat.Account;
import com.example.kimdongun.paintchat.Client;
import com.example.kimdongun.paintchat.DebugHandler;
import com.example.kimdongun.paintchat.GameCanvas;
import com.example.kimdongun.paintchat.HandlerMessageDecode;
import com.example.kimdongun.paintchat.JsonEncode;
import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.adapter.GameChatListViewAdapter;
import com.example.kimdongun.paintchat.adapter.GameClientListViewAdapter;
import com.example.kimdongun.paintchat.dialog.ColorPickerDialog;
import com.example.kimdongun.paintchat.dialog.PaintListDialog;
import com.example.kimdongun.paintchat.item.ChatRoomListViewItem;
import com.example.kimdongun.paintchat.item.GameChatListViewItem;
import com.example.kimdongun.paintchat.item.GameClientListViewItem;
import com.example.kimdongun.paintchat.server.OnFinishDBListener;
import com.example.kimdongun.paintchat.server.PostDB;
import com.example.kimdongun.paintchat.service.SocketService;
import com.example.kimdongun.paintchat.service.SocketServiceExecutor;
import com.example.kimdongun.paintchat.toast.GameRoomToast;
import com.example.kimdongun.paintchat.toast.NormalChatToast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import yuku.ambilwarna.AmbilWarnaDialog;

public class GameRoomActivity extends AppCompatActivity implements View.OnClickListener, HandlerMessageDecode, SocketServiceExecutor,
                                                                    DialogInterface.OnDismissListener, AdapterView.OnItemClickListener {
    private String gameRoomKey_; //방 키값
    private String gameHostNick_; //방장 닉네임
    private String quizHostNick_; //문제 출제자 닉네임

    private ListView listView_nick; //입장 유저 목록 리스트뷰
    private ListView listView_chat; //유저 채팅 리스트뷰
    private GameChatListViewAdapter gameChatListViewAdapter; //유저 채팅 어댑터
    private GameClientListViewAdapter gameClientListViewAdapter; //유저 목록 어댑터
    final static int CHAT_COLOR = Color.argb(0xFF, 0xA1, 0xA1,0xA1);
    final static int ALARM_COLOR = Color.argb(0xFF, 0xD8, 0xD0,0x00);
    final static int ALARM_COLOR2 = Color.argb(0xFF, 0x00, 0xD9,0xD6);

    private final int gameTime = 180; //게임 초
    private ProgressBar progressBar; //퀴즈 프로그레스 바
    private TextView textView_quiz; //퀴즈 정답
    private TextView textView_all_ready; //모두 레디 텍스트
    private Button button_ready; //레디 버튼 (방장 제외)
    private Button button_start; //시작 버튼 (방장 전용)
    private boolean isReady = false; //자신이 레디 상태인가

    private LinearLayout layout_tool; //canvas 툴을 담고 있는 레이아웃

    private ImageView imageView_arrow_left; //펜 두깨 내리는 이미지
    private ImageView imageView_arrow_right; //펜 두깨 올라는 이미지
    private View imageView_pen_width; //펜 두깨 이미지
    private ImageView imageView_select_pen; //펜 선택 박스
    private ImageView imageView_select_eraser; //지우개 선택 박스
    private ImageView imageView_pen; //펜 이미지
    private ImageView imageView_eraser; //지우개 이미지
    private ImageView imageView_palette; //팔레트 이미지
    private ImageView imageView_restore; //되돌리기 이미지
    private ImageView imageView_clear; //clear 이미지

    private GameCanvas gameCanvas; //게임 그림판
    private Handler canvasHandler_; //그림판 이벤트 받는 핸들러

    private Client client_; //클라이언트

    private Button button_send; //채팅 보내기 버튼
    private EditText editText_msg; //채팅 메세지

    private ColorPickerDialog colorPickerDialog_; //펜 or 캔버스 선택 다이얼 로그

    private Handler handler_; //소켓 스레드랑 통신 할 핸들러
    public SocketService socketService_; //소켓 서비스
    private ServiceConnection serviceConnection_; //서비스 커넥션
    public boolean isLiveBinder; //서비스 바인드 연결 유무
    private NormalChatToast normalChatToast; //채팅 토스트 메세지

    public boolean isGameStart_; //게임이 시작 되었는가?
    private int maxNum_; //게임방 최대 인원

    private int initPenWidth;

    private ArrayList<String> fileNameArray = new ArrayList<>(); //그림 파일 이름
    private ArrayList<String> quizNameArray = new ArrayList<>(); //그림 퀴즈 정답

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_room);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //초기화
        button_send = (Button)findViewById(R.id.button_send);
        editText_msg = (EditText)findViewById(R.id.editText_msg);

        client_ = Client.getInstance(this);

        handler_ = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                String str = msg.obj.toString();
                if(str.equals("connectSuccess")){ //소켓 연결 성공
                    Toast.makeText(GameRoomActivity.this, "소켓 서버와 연결하였습니다.", Toast.LENGTH_SHORT).show();

                    String[] keys = {""};
                    Object[] values = {""};
                    String jsonStr = JsonEncode.getInstance().encodeCommandJson("clientList", keys, values);
                    //서버로 전송
                    if (isLiveBinder)
                        socketService_.sendMessage(jsonStr);

                    if(isGameStart_) {
                        //이때까지 그린 그림 정보 가져오기 요청
                        String jsonStr2 = JsonEncode.getInstance().encodeCommandJson("loadCanvas", keys, values);
                        //서버로 전송
                        if (isLiveBinder)
                            socketService_.sendMessage(jsonStr2);
                    }
                }else if(str.equals("connectFail")){ //소켓 연결 실패
                    Toast.makeText(GameRoomActivity.this, "소켓 서버와 연결을 실패하였습니다.", Toast.LENGTH_SHORT).show();
                }else {
                    doReceiveAction(str);
                }
            }
        };

        canvasHandler_ = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(isGameStart_) { //게임이 시작 된 경우에만 서버로 Paint 전송
                    if (isLiveBinder)
                        socketService_.sendMessage(msg.obj.toString());
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

                    String[] keys = {""};
                    Object[] values = {""};
                    String jsonStr = JsonEncode.getInstance().encodeCommandJson("clientList", keys, values);
                    //서버로 전송
                    socketService_.sendMessage(jsonStr);

                    if(isGameStart_) {
                        //이때까지 그린 그림 정보 가져오기 요청
                        String jsonStr2 = JsonEncode.getInstance().encodeCommandJson("loadCanvas", keys, values);
                        //서버로 전송
                        socketService_.sendMessage(jsonStr2);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                isLiveBinder = false;
            }
        };

        Intent serviceIntent = new Intent(GameRoomActivity.this, SocketService.class);
        bindService(serviceIntent, serviceConnection_, Context.BIND_AUTO_CREATE); //intent객체, 서비스와 연결에 대한 정의

        listView_chat = (ListView)findViewById(R.id.listView_chat);
        gameChatListViewAdapter = new GameChatListViewAdapter(this);
        listView_chat.setAdapter(gameChatListViewAdapter);
        listView_chat.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL); //리스트뷰 새 아이템 추가하면 자동 스크롤 설정
        //새로운 메세지 추가할 때, 마지막 채팅 아래 띄우기
        gameChatListViewAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView_chat.setSelection(gameChatListViewAdapter.getCount() - 1);
            }
        });

        listView_nick = (ListView)findViewById(R.id.listView_nick);
        gameClientListViewAdapter = new GameClientListViewAdapter(this);
        listView_nick.setAdapter(gameClientListViewAdapter);
        //새로운 아이템 추가할 때, 마지막 아래 띄우기
        gameClientListViewAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView_nick.setSelection(gameChatListViewAdapter.getCount() - 1);
            }
        });

        progressBar = (ProgressBar)findViewById(R.id.progressBar); //퀴즈 프로그레스 바
        progressBar.setScaleY(3f);
        progressBar.setMax(gameTime);//게임 초 입력
        textView_quiz = (TextView)findViewById(R.id.textView_quiz); //퀴즈 정답
        textView_all_ready = (TextView)findViewById(R.id.textView_all_ready); //모두 레디 텍스트
        button_ready = (Button)findViewById(R.id.button_ready); //레디 버튼
        button_start = (Button)findViewById(R.id.button_start); //시작 버튼

        //이미지 뷰 초기화
        layout_tool = (LinearLayout)findViewById(R.id.layout_tool); //툴을 담고 있는 레이아웃
        imageView_arrow_left = (ImageView)findViewById(R.id.imageView_arrow_left); //펜 두깨 내리는 이미지
        imageView_arrow_right = (ImageView)findViewById(R.id.imageView_arrow_right); //펜 두깨 올라는 이미지
        imageView_pen_width = (View)findViewById(R.id.imageView_pen_width); //펜 두깨 이미지
        imageView_select_pen = (ImageView)findViewById(R.id.imageView_select_pen); //펜 선택 박스
        imageView_select_eraser = (ImageView)findViewById(R.id.imageView_select_eraser); //지우개 선택 박스
        imageView_pen = (ImageView)findViewById(R.id.imageView_pen); //펜 이미지
        imageView_eraser = (ImageView)findViewById(R.id.imageView_eraser); //지우개 이미지
        imageView_palette = (ImageView)findViewById(R.id.imageView_palette); //팔레트 이미지
        imageView_restore = (ImageView)findViewById(R.id.imageView_restore); //되돌리기 이미지
        imageView_clear = (ImageView)findViewById(R.id.imageView_clear); //clear 이미지
        gameCanvas = (GameCanvas)findViewById(R.id.canvas); //그림판
        gameCanvas.setHandler(canvasHandler_); //그림판 핸들러 설정

        ViewGroup.LayoutParams pen_width_params = imageView_pen_width.getLayoutParams();
        initPenWidth = pen_width_params.height;

        Intent intent = getIntent();
        gameCanvas.drawable_ = intent.getBooleanExtra("drawable", true);
        isGameStart_ = intent.getBooleanExtra("isGameStart", false);
        maxNum_ = intent.getIntExtra("maxNum", 2); //방 최대 인원 (X표기 위함)
        setTitle(intent.getStringExtra("name")); //방 제목
        gameRoomKey_ = intent.getStringExtra("key");
        gameHostNick_ = intent.getStringExtra("host"); //방장 닉네임
        quizHostNick_ = intent.getStringExtra("quizHost"); //문제 출제자 닉네임
        //캔버스 툴 visible 설정
        if(gameCanvas.drawable_){
            layout_tool.setVisibility(View.VISIBLE);
        }else{
            layout_tool.setVisibility(View.INVISIBLE);
        }

        if(isGameStart_){ //게임이 시작 되었으면 start / ready 버튼 안보이게
            button_ready.setVisibility(View.INVISIBLE);
            button_start.setVisibility(View.INVISIBLE);
        }else { //게임 시작 전이면 방장/유저 별로 start / ready 버튼 설정
            if (gameHostNick_.equals(client_.account_.nick_)) { //자신이 방장일 경우
                button_ready.setVisibility(View.INVISIBLE);
                button_start.setVisibility(View.VISIBLE);
            } else { //자신이 방장이 아닌 경우
                button_ready.setVisibility(View.VISIBLE);
                button_start.setVisibility(View.INVISIBLE);
            }
        }

        normalChatToast = new NormalChatToast(this); //채팅 토스트 메세지

        //터치 이벤트
        button_ready.setOnClickListener(this);
        button_start.setOnClickListener(this);
        button_send.setOnClickListener(this);
        imageView_arrow_left.setOnClickListener(this);
        imageView_arrow_right.setOnClickListener(this);
        imageView_pen.setOnClickListener(this);
        imageView_eraser.setOnClickListener(this);
        imageView_palette.setOnClickListener(this);
        imageView_restore.setOnClickListener(this);
        imageView_clear.setOnClickListener(this);
        listView_nick.setOnItemClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(progressThread != null)
            progressThread.endThread = true; //현재 실행 중 스레드 종료
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
    public void onBackPressed() {
        AlertDialog.Builder alert_confirm = new AlertDialog.Builder(GameRoomActivity.this);
        alert_confirm.setMessage("방에서 퇴장하겠습니까?").setCancelable(false).setPositiveButton("퇴장",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String[] keys = {""};
                        Object[] values = {""};
                        String jsonStr = JsonEncode.getInstance().encodeCommandJson("exitGameRoom", keys, values);
                        if(isLiveBinder)
                            socketService_.sendMessage(jsonStr);
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

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button_ready: //레디
                isReady = !isReady;
                String[] keys0 = {"ready"};
                Object[] values0 = {isReady};
                String jsonStr0 = JsonEncode.getInstance().encodeCommandJson("readyGameRoom", keys0, values0);
                if(isLiveBinder)
                    socketService_.sendMessage(jsonStr0);
                break;

            case R.id.button_start: //시작
                String[] keys1 = {""};
                Object[] values1 = {""};
                String jsonStr1 = JsonEncode.getInstance().encodeCommandJson("startGameRoom", keys1, values1);
                if(isLiveBinder)
                    socketService_.sendMessage(jsonStr1);
                break;

            case R.id.button_send: //채팅 전송
                String msg = editText_msg.getText().toString();
                if(msg.length() > 0) {
                    //채팅 정보를 json으로 변환
                    String[] keys = {"msg"};
                    Object[] values = {msg};
                    String jsonStr = JsonEncode.getInstance().encodeCommandJson("gameChat", keys, values);
                    if (isLiveBinder)
                        socketService_.sendMessage(jsonStr);

                    editText_msg.setText("");
                }
                break;

            case R.id.imageView_arrow_left: //펜 두깨 내림
                if(gameCanvas.decreasePenWidth()) {
                    DebugHandler.log(getClass().getName(), "펜 두깨 내림");
                    ViewGroup.LayoutParams pen_width_params = imageView_pen_width.getLayoutParams();
                    pen_width_params.height--;
                    imageView_pen_width.setLayoutParams(pen_width_params);
                    DebugHandler.log(getClass().getName(), "펜 UI 두깨: " +  pen_width_params.height);
                }
                break;

            case R.id.imageView_arrow_right: //펜 두깨 올림
                if(gameCanvas.increasePenWidth()) {
                    DebugHandler.log(getClass().getName(), "펜 두깨 올림");
                    ViewGroup.LayoutParams pen_width_params = imageView_pen_width.getLayoutParams();
                    pen_width_params.height++;
                    imageView_pen_width.setLayoutParams(pen_width_params);
                    DebugHandler.log(getClass().getName(), "펜 UI 두깨: " +  pen_width_params.height);
                }
                break;

            case R.id.imageView_pen: //펜 선택
                imageView_select_pen.setVisibility(View.VISIBLE);
                imageView_select_eraser.setVisibility(View.INVISIBLE);
                gameCanvas.setToPen();
                imageView_pen_width.setBackgroundColor(gameCanvas.getPenColor()); //선 두깨 색상 펜 색상으로
                if(isGameStart_) { //게임이 시작 된 경우에만 서버로 Paint 전송
                    String[] keys = {"type"};
                    Object[] values = {"toPen"};
                    String jsonStr = JsonEncode.getInstance().encodeCommandJson("gameCanvas", keys, values);
                    if (isLiveBinder)
                        socketService_.sendMessage(jsonStr);
                }
                break;

            case R.id.imageView_eraser: //지우개 선택
                imageView_select_pen.setVisibility(View.INVISIBLE);
                imageView_select_eraser.setVisibility(View.VISIBLE);
                gameCanvas.setToEraser();
                imageView_pen_width.setBackgroundColor(Color.BLACK); //선 두깨 색상 검정색으로
                if(isGameStart_) { //게임이 시작 된 경우에만 서버로 Paint 전송
                    String[] keys2 = {"type"};
                    Object[] values2 = {"toEraser"};
                    String jsonStr2 = JsonEncode.getInstance().encodeCommandJson("gameCanvas", keys2, values2);
                    if (isLiveBinder)
                        socketService_.sendMessage(jsonStr2);
                }
                break;

            case R.id.imageView_palette: //팔레트 선택
                colorPickerDialog_ = new ColorPickerDialog(this);
                colorPickerDialog_.setOnDismissListener(this);
                colorPickerDialog_.show();
                break;

            case R.id.imageView_restore: //되돌리기 선택
                gameCanvas.restore();
                if(isGameStart_) { //게임이 시작 된 경우에만 서버로 Paint 전송
                    String[] keys3 = {"type"};
                    Object[] values3 = {"undo"};
                    String jsonStr3 = JsonEncode.getInstance().encodeCommandJson("gameCanvas", keys3, values3);
                    if (isLiveBinder)
                        socketService_.sendMessage(jsonStr3);
                }
                break;

            case R.id.imageView_clear: //clear 선택
                gameCanvas.clear();
                //펜 선택으로 변경
                imageView_select_pen.setVisibility(View.VISIBLE);
                imageView_select_eraser.setVisibility(View.INVISIBLE);
                gameCanvas.setToPen();
                imageView_pen_width.setBackgroundColor(gameCanvas.getPenColor()); //선 두깨 색상 펜 색상으로

                if(isGameStart_) { //게임이 시작 된 경우에만 서버로 Paint 전송
                    String[] keys4 = {"type"};
                    Object[] values4 = {"clear"};
                    String jsonStr4 = JsonEncode.getInstance().encodeCommandJson("gameCanvas", keys4, values4);
                    if (isLiveBinder)
                        socketService_.sendMessage(jsonStr4);
                }
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
        if(gameHostNick_.equals(client_.account_.nick_) && !isGameStart_){ //자신이 방장인 경우에 유저 리스트 터치하면 강퇴 다이얼로그 띄움 && 게임 중 아닐 때
            final GameClientListViewItem item = (GameClientListViewItem)gameClientListViewAdapter.getItem(index);
            if(!item.nick_.equals(client_.account_.nick_)) { //자신은 강퇴 못하게 막기
                AlertDialog.Builder alert_confirm = new AlertDialog.Builder(GameRoomActivity.this);
                alert_confirm.setMessage("'" + item.nick_ + "'님을 강제퇴장 시키겠습니까?").setCancelable(false).setPositiveButton("강제 퇴장",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String[] keys = {"id"};
                                Object[] values = {item.id_};
                                String jsonStr = JsonEncode.getInstance().encodeCommandJson("kickClient", keys, values);
                                //서버로 전송
                                socketService_.sendMessage(jsonStr);
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
        final Object requestObj = arrayList.get(1); //명령 디테일 값

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

            case "gameChat": //채팅 행동
                receiveGameChat(requestObj);
                break;

            case "gameCanvas": //Canvas 행동
                receiveGameCanvas(requestObj);
                break;

            case "noticeEnter": //방에 클라이언트 입장을 알리는 행동
                receiveNoticeEnter(requestObj);
                break;

            case "noticeExit": //방에 클라이언트 퇴장을 알리는 행동
                receiveNoticeExit(requestObj);
                break;

            case "noticeKick": //방에 클라이언트 강제 퇴장을 알리는 행동
                receiveNoticeKick(requestObj);
                break;

            case "noticeChangeHost": //방에 방장 변경을 알리는 행동
                receiveNoticeChangeHost(requestObj);
                break;

            case "exitGameRoomResult": //방 퇴장 요청 후 행동
                receiveExitChangeHost(requestObj);
                break;

            case "clientListResult": //방에 클라이언트 리스트 요청 후 결과
                receiveClientList(requestObj);
                break;

            case "readyGameRoom": //유저의 ready 상태 변함 행동
                receiveReady(requestObj);
                break;

            case "allReadyGameRoom": //유저가 모두 Ready 상태를 알림 받은 후 행동
                receiveAllReady(requestObj);
                break;

            case "startGameRoom": //게임 Start 요청 후 행동
                receiveStartGame(requestObj);
                break;

            case "quizCorrect": //게임 정답자가 나온 후 행동
                receiveChangeQuizHost(requestObj, 1);
                break;

            case "timeOutGameRoom": //타임 아웃으로 새 게임 시작 알림 행동
                receiveChangeQuizHost(requestObj, 2);
                break;

            case "exitQuizHostGameRoom": //출제자가 방을 나가서 변경 된 경우
                receiveChangeQuizHost(requestObj, 3);
                break;

            case "endGameRoom": //게임 끝 알림 행동
                receiveEndGame(requestObj);
                break;

            case "progressGameRoom": //새로 방에 들어왔을 때 게임 진행 프로그래스 바 갱신 행동
                receiveProgressGame(requestObj);
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

    //받은 커맨드가 채팅일 경우 행동
    private void receiveGameChat(Object jsonObj){
        HashMap<String, Object> map = new HashMap<String, Object>();
        try {
            JSONObject json = (JSONObject)jsonObj;
            map.put("nick", json.get("nick")); //채팅을 보낸 유저 닉네임
            map.put("msg", json.get("msg")); //채팅 메세지
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String msg = "[" + (String)map.get("nick") + "] : " + (String)map.get("msg");
        GameChatListViewItem item = new GameChatListViewItem(msg, CHAT_COLOR);
        gameChatListViewAdapter.addItem(item);
        gameChatListViewAdapter.notifyDataSetChanged();
    }

    //받은 커맨드가 Canvas 작업일 경우 행동
    private void receiveGameCanvas(Object jsonObj){
        HashMap<String, Object> map = new HashMap<String, Object>();
        try {
            JSONObject json = (JSONObject)jsonObj;
            switch (((String)json.get("type"))){ //canvas type: pen, eraser, canvas
                case "pen": //pen 정보
                    double x = 0;//x 좌표
                    double y = 0;//y 좌표
                    try{
                        x = json.getDouble("x"); //x 좌표
                        y = json.getDouble("y"); //y 좌표
                    }catch (ClassCastException e){
                        e.printStackTrace();
                    }
                    switch ((String)json.get("motion")){
                        case "start": //터치 시작 했을 때 path 정보
                            gameCanvas.touchStart((float)x, (float)y);
                            break;

                        case "move": //터치 중일 때 path 정보
                            gameCanvas.touchMove((float)x, (float)y);
                            break;

                        case "end": //터치 마칠 했을 때 path 정보
                            gameCanvas.touchEnd((float)x, (float)y);
                            break;
                    }
                    break;

                case "toPen": //pen으로 변경
                    gameCanvas.setToPen();
                    break;

                case "toEraser": //eraser으로 변경
                    gameCanvas.setToEraser();
                    break;

                case "canvasColor": //canvas 색상 변경
                    int canvasColor = (int)json.get("color");//변경 할 캔버스 색상
                    gameCanvas.setCanvasColor(canvasColor);
                    break;

                case "penColor": //펜 색상 변경
                    int penColor = (int)json.get("color");//펜 색상
                    gameCanvas.setPenColor(penColor);
                    break;

                case "penWidth": //펜 두깨 변경
                    double penWidth = json.getDouble("width");//펜 두깨
                    gameCanvas.setPenWidth((float)penWidth);
                    break;

                case "undo": //되돌리기
                    gameCanvas.restore();
                    break;

                case "clear": //지우기
                    gameCanvas.clear();
                    gameCanvas.setToPen();
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //받은 커맨드가 방에 클라이언트 입장을 알리는 행동
    private void receiveNoticeEnter(Object jsonObj){
        final HashMap<String, Object> map = new HashMap<String, Object>();
        try {
            JSONObject json = (JSONObject)jsonObj;
            map.put("id", json.get("id")); //방에 들어온 유저 아이디
            map.put("nick", json.get("nick")); //방에 들어온 유저 닉네임

            GameClientListViewItem item = new GameClientListViewItem((String)map.get("id"), (String)map.get("nick"), false, false);
            gameClientListViewAdapter.addItem(item);
            gameClientListViewAdapter.notifyDataSetChanged();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String msg = "[알림]-[" + (String)map.get("nick") + "] : 님이 방에 입장하였습니다.";
        GameChatListViewItem item = new GameChatListViewItem(msg, ALARM_COLOR);
        gameChatListViewAdapter.addItem(item);
        gameChatListViewAdapter.notifyDataSetChanged();

        if(isGameStart_ && gameCanvas.drawable_) { //게임 중일 때 누가 입장했을 경우 현재 진행 시간을 보내줌 (그리는 사람이)
            String[] keys = {"progressBar"};
            Object[] values = {progressBar.getProgress()};
            String jsonStr = JsonEncode.getInstance().encodeCommandJson("progressGameRoom", keys, values);
            if (isLiveBinder)
                socketService_.sendMessage(jsonStr);
        }
    }

    //받은 커맨드가 방에 클라이언트 퇴장을 알리는 행동
    private void receiveNoticeExit(Object jsonObj){
        HashMap<String, Object> map = new HashMap<String, Object>();
        try {
            JSONObject json = (JSONObject)jsonObj;
            map.put("id", json.get("id")); //방에서 나간 유저 아이디
            map.put("nick", json.get("nick")); //방에서 나간 유저 닉네임
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //방에서 나간 클라이언트 정보 비워주기
        gameClientListViewAdapter.removeItem((String)map.get("id"));
        gameClientListViewAdapter.notifyDataSetChanged();

        String msg = "[알림]-[" + (String)map.get("nick") + "] : 님이 방에서 퇴장하였습니다.";
        GameChatListViewItem item = new GameChatListViewItem(msg, ALARM_COLOR);
        gameChatListViewAdapter.addItem(item);
        gameChatListViewAdapter.notifyDataSetChanged();
    }

    //받은 커맨드가 방에 클라이언트 강제 퇴장을 알리는 행동
    private void receiveNoticeKick(Object jsonObj){
        HashMap<String, Object> map = new HashMap<String, Object>();
        try {
            JSONObject json = (JSONObject)jsonObj;
            map.put("id", json.get("id")); //방에서 강제 퇴장 된 유저 아이디
            map.put("nick", json.get("nick")); //방에서 강제 퇴장 된 유저 닉네임
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //방에서 나간 클라이언트 정보 비워주기
        gameClientListViewAdapter.removeItem((String)map.get("id"));
        gameClientListViewAdapter.notifyDataSetChanged();

        String msg = "[알림]-[" + (String)map.get("nick") + "] : 님이 방에서 강제퇴장 되었습니다.";
        GameChatListViewItem item = new GameChatListViewItem(msg, ALARM_COLOR);
        gameChatListViewAdapter.addItem(item);
        gameChatListViewAdapter.notifyDataSetChanged();

        if(client_.account_.id_.equals((String)map.get("id"))){ //본인이 강제 퇴장 당한 경우 메인 엑티비티로 화면 이동
            AlertDialog.Builder alert_confirm = new AlertDialog.Builder(GameRoomActivity.this);
            alert_confirm.setMessage("방에서 강제퇴장 되었습니다.").setCancelable(false).setPositiveButton("확인",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(GameRoomActivity.this, MainActivity.class);
                            intent.putExtra("fragId", 1);
                            startActivity(intent);
                            finish();
                        }
                    });
            AlertDialog alert = alert_confirm.create();
            alert.show();
        }
    }

    //받은 커맨드가 방에 클라이언트 입장을 알리는 행동
    private void receiveNoticeChangeHost(Object jsonObj){
        String hostId = "";
        try {
            JSONObject json = (JSONObject)jsonObj;
            hostId = json.getString("id"); //새로운 방장 유저 아이디
            gameHostNick_ = json.getString("nick"); //새로운 방장 유저 닉네임
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(!isGameStart_) { //게임 중이 아닐 경우만 방장 변경 UI 활성화화
            //방장으로 된 인원은 ready 버튼 비활성화 start 버튼 활성화
            if (client_.account_.nick_.equals(gameHostNick_)) {
                button_ready.setVisibility(View.INVISIBLE);
                button_start.setVisibility(View.VISIBLE);
            }

            String msg = "[알림]-[" + gameHostNick_ + "] : 님이 새로운 방장이 되었습니다.";
            gameClientListViewAdapter.getItem(hostId).isHost_ = true;
            gameClientListViewAdapter.getItem(hostId).isReady_ = false;
            gameClientListViewAdapter.notifyDataSetChanged();

            GameChatListViewItem item = new GameChatListViewItem(msg, ALARM_COLOR2);
            gameChatListViewAdapter.addItem(item);
            gameChatListViewAdapter.notifyDataSetChanged();
        }
    }

    //받은 커맨드가 방 퇴장 요청 후 행동
    private void receiveExitChangeHost(Object jsonObj){
        HashMap<String, Object> map = new HashMap<String, Object>();
        try {
            JSONObject json = (JSONObject)jsonObj;
            map.put("result", json.get("result")); //결과
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String result = (String)map.get("result");
        if(result.equals("success")){ //게임 방 퇴장 성공
            Intent intent = new Intent(GameRoomActivity.this, MainActivity.class);
            intent.putExtra("fragId", 1);
            startActivity(intent);
            finish();
        }else{ //게임 방 퇴장 실패
            Toast.makeText(GameRoomActivity.this, "게임 방 퇴장에 실패하였습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    //방에 클라이언트 리스트 요청 후 결과
    private void receiveClientList(Object jsonObj){
        gameClientListViewAdapter.removeAll();
        GameClientListViewItem myItem;
        if(client_.account_.nick_.equals(gameHostNick_)){
            myItem = new GameClientListViewItem(client_.account_.id_, client_.account_.nick_, false, true);
        }else{
            myItem = new GameClientListViewItem(client_.account_.id_, client_.account_.nick_, false, false);
        }
        gameClientListViewAdapter.addItem(myItem);//본인 추가
        try {
            JSONArray jsonArray = (JSONArray)jsonObj;
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject object = jsonArray.getJSONObject(i);
                String nick = object.getString("nick");
                GameClientListViewItem item;

                if(nick.equals(gameHostNick_)){
                    item = new GameClientListViewItem(object.getString("id"), nick, object.getBoolean("ready"), true);
                }else{
                    item = new GameClientListViewItem(object.getString("id"), nick, object.getBoolean("ready"), false);
                }
                gameClientListViewAdapter.addItem(item);//추가
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        gameClientListViewAdapter.notifyDataSetChanged();

        if(isGameStart_){//게임 시작 아닐 경우만 ready 혹은 방장 UI 띄움, 시작일 경우에는 문제 출제자 테두리 설정
            textView_all_ready.setVisibility(View.INVISIBLE);
            button_ready.setVisibility(View.INVISIBLE);
            button_start.setVisibility(View.INVISIBLE);
            layout_tool.setVisibility(View.INVISIBLE);
            textView_quiz.setVisibility(View.INVISIBLE);
        }
    }

    //유저의 ready 상태 변함 행동
    private void receiveReady(Object jsonObj){
        HashMap<String, Object> map = new HashMap<String, Object>();
        try {
            JSONObject json = (JSONObject)jsonObj;
            map.put("id", json.get("id")); //ready 변경 한 유저
            map.put("ready", json.get("ready")); //ready 상태
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //ready 상태 변경 한 클라이언트 정보를 보여주고 있는 layout 찾아서 바꿔주기
        gameClientListViewAdapter.getItem((String)map.get("id")).isReady_ = (boolean)map.get("ready");
        gameClientListViewAdapter.notifyDataSetChanged();
    }

    //유저가 모두 Ready 상태를 알림 받은 후 행동
    private void receiveAllReady(Object jsonObj){
        HashMap<String, Object> map = new HashMap<String, Object>();
        try {
            JSONObject json = (JSONObject)jsonObj;
            map.put("allReady", json.get("allReady")); //전부 ready 상태?
        } catch (JSONException e) {
            e.printStackTrace();
        }

        boolean allReady = (boolean)map.get("allReady");
        if(allReady){ //전부 레디 상태
            textView_all_ready.setVisibility(View.VISIBLE);
            String msg = "[알림]-모든 인원이 준비 하였습니다. START 버튼을 눌러주세요";

            GameChatListViewItem item = new GameChatListViewItem(msg, ALARM_COLOR2);
            gameChatListViewAdapter.addItem(item);
            gameChatListViewAdapter.notifyDataSetChanged();
        }else{
            textView_all_ready.setVisibility(View.INVISIBLE);
        }
    }

    //방장이 Start 요청 후 행동
    private void receiveStartGame(Object jsonObj){
        HashMap<String, Object> map = new HashMap<String, Object>();
        try {
            JSONObject json = (JSONObject)jsonObj;
            map.put("isGameStart", json.get("isGameStart")); //게임 시작 상태
            map.put("nick", json.get("nick")); //출제자 닉네임
            map.put("quiz", json.get("quiz")); //출제 문제
            map.put("maxQuiz", json.get("maxQuiz")); //최대 문제 수
        } catch (JSONException e) {
            e.printStackTrace();
        }

        isGameStart_ = (boolean)map.get("isGameStart");

        if(!isGameStart_){ //게임 시작 요청 실패
            Toast.makeText(GameRoomActivity.this, "게임을 시작 할 수 없습니다.", Toast.LENGTH_SHORT).show();
        }else{
            //ready / start 버튼 / ready 텍스트, 방장 텍스트 비활성화
            isReady = false;
            textView_all_ready.setVisibility(View.INVISIBLE);
            button_ready.setVisibility(View.INVISIBLE);
            button_start.setVisibility(View.INVISIBLE);

            GameRoomToast toast = new GameRoomToast(this);
            String msg = "게임이 시작 되었습니다.\n" + "'" + (String)map.get("nick") +
                    "' 님이 그림을 그려주세요.[" + 1 + "/" + map.get("maxQuiz").toString() + "]";
            toast.showToast(msg);

            GameChatListViewItem item = new GameChatListViewItem("[알림]-" + msg, ALARM_COLOR2);
            gameChatListViewAdapter.addItem(item);
            gameChatListViewAdapter.notifyDataSetChanged();

            initGame((String)map.get("nick"), (String)map.get("quiz")); //게임 초기화
        }
    }

    //게임 출제자 변경 후 행동
    private void receiveChangeQuizHost(Object jsonObj, int type){
        HashMap<String, Object> map = new HashMap<String, Object>();
        try {
            JSONObject json = (JSONObject)jsonObj;
            map.put("nick", json.get("nick")); //정답자 닉네임
            map.put("quiz", json.get("quiz")); //다음 문제
            map.put("numQuiz", json.get("numQuiz")); //진행 한 문제 수
            map.put("maxQuiz", json.get("maxQuiz")); //최대 문제 수
            map.put("totalNum", json.get("totalNum")); //게임방에서 진행 된 총 문제 수
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if(progressThread != null)
            progressThread.endThread = true; //현재 실행 중 스레드 종료

        GameRoomToast toast = new GameRoomToast(this);
        GameChatListViewItem item = null;
        String msg = null;
        switch (type){
            case 1: //정답을 맞춰서 변경 된 경우
                msg = "[" + (String)map.get("nick") + "] 님이 [" + textView_quiz.getText().toString() + "]을 맞췄습니다.\n" +
                        "[" + (String)map.get("nick") + "] 님이 그림을 그려주세요.[" + map.get("numQuiz").toString() + "/" + map.get("maxQuiz").toString() + "]";

                item = new GameChatListViewItem("[정답]-" + msg, getResources().getColor(R.color.colorAccent));
                break;

            case 2: //타임 아웃으로 변경 된 경우
                msg = "TIME OUT\n[" + (String)map.get("nick") + "] 님이 그림을 그려주세요.[" +
                        map.get("numQuiz").toString() + "/" + map.get("maxQuiz").toString() + "]";
                item = new GameChatListViewItem("[알림]-" + msg, ALARM_COLOR2);
                break;

            case 3: //출제자가 방을 나가서 변경 된 경우
                msg = "그림 그리는 사람이 방을 나갔습니다.\n" +
                        "[" + (String)map.get("nick") + "] 님이 그림을 그려주세요.[" + map.get("numQuiz").toString() + "/" + map.get("maxQuiz").toString() + "]";
                item = new GameChatListViewItem("[알림]-" + msg, ALARM_COLOR2);
                break;
        }

        toast.showToast(msg);

        gameChatListViewAdapter.addItem(item);
        gameChatListViewAdapter.notifyDataSetChanged();

        //그림 파일 캐시에 저장
        String fileName = gameRoomKey_ + map.get("totalNum").toString() + ".png";
        fileNameArray.add(fileName);
        quizNameArray.add(textView_quiz.getText().toString());
        //서버로 그린 그림 전송(그림을 그린 사람만 & 방에서 나감으로 출제자 변경 제외)
        if(gameCanvas.drawable_){
            if(type != 3){
                String filePath = gameCanvas.saveBitmapOnCache(fileName, this);
                uploadPaintImage(fileName, filePath);
            }
        }

        initGame((String)map.get("nick"), (String)map.get("quiz")); //게임 초기화
    }

    //게임 끝 알림 행동
    private void receiveEndGame(Object jsonObj){
        HashMap<String, Object> map = new HashMap<String, Object>();
        try {
            JSONObject json = (JSONObject)jsonObj;
            map.put("totalNum", json.get("totalNum")); //게임방에서 진행 된 총 문제 수
            map.put("type", json.get("type")); //어떤 형태로 게임이 끝났는가
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //그림 파일 캐시에 저장
        String fileName = gameRoomKey_ + map.get("totalNum").toString() + ".png";
        fileNameArray.add(fileName);
        quizNameArray.add(textView_quiz.getText().toString());
        //서버로 그린 그림 전송(그림을 그린 사람만 & 방에서 나감으로 출제자 변경 제외)
        if(gameCanvas.drawable_){
            if(!((String)map.get("type")).equals("exit")){
                String filePath = gameCanvas.saveBitmapOnCache(fileName, this);
                uploadPaintImage(fileName, filePath);
            }
        }

        progressThread.endThread = true; //현재 실행 중 스레드 종료
        GameRoomToast toast = new GameRoomToast(this);
        String msg = "모든 퀴즈가 끝났습니다.";
        toast.showToast(msg);

        GameChatListViewItem item = new GameChatListViewItem("[알림]-" + msg, ALARM_COLOR2);
        gameChatListViewAdapter.addItem(item);
        gameChatListViewAdapter.notifyDataSetChanged();

        //게임 중 아님
        isGameStart_ = false;
        //모두 그릴 수 있는 모드로
        gameCanvas.drawable_ = true;
        layout_tool.setVisibility(View.VISIBLE);
        textView_quiz.setVisibility(View.VISIBLE); //퀴즈 정답 보이게
        textView_quiz.setText("정답"); //정답 초기화

        //캔버스 초기화
        gameCanvas.setPenWidth(5);
        ViewGroup.LayoutParams pen_width_params = imageView_pen_width.getLayoutParams();
        pen_width_params.height = initPenWidth;
        imageView_pen_width.setLayoutParams(pen_width_params);
        gameCanvas.setCanvasColor(Color.WHITE);
        gameCanvas.setPenColor(Color.BLACK);
        imageView_select_pen.setVisibility(View.VISIBLE);
        imageView_select_eraser.setVisibility(View.INVISIBLE);
        gameCanvas.setToPen();
        gameCanvas.clear();

        //ready / start 초기화
        if (gameHostNick_.equals(client_.account_.nick_)) { //자신이 방장일 경우
            button_ready.setVisibility(View.INVISIBLE);
            button_start.setVisibility(View.VISIBLE);
        } else { //자신이 방장이 아닌 경우
            button_ready.setVisibility(View.VISIBLE);
            button_start.setVisibility(View.INVISIBLE);
        }

        String[] keys = {""};
        Object[] values = {""};
        String jsonStr = JsonEncode.getInstance().encodeCommandJson("clientList", keys, values);
        DebugHandler.log(getClass().getName(), jsonStr);
        //서버로 전송
        if (isLiveBinder)
            socketService_.sendMessage(jsonStr);

        PaintListDialog paintListDialog = new PaintListDialog();
        paintListDialog.setDialogSize(getWindow().getWindowManager().getDefaultDisplay().getWidth()*3/4
                , getWindow().getWindowManager().getDefaultDisplay().getHeight()*2/3);
        paintListDialog.setDrawableList(fileNameArray, quizNameArray);
        paintListDialog.show(getSupportFragmentManager(), "paintListDialog");
        fileNameArray.clear();
        quizNameArray.clear();
    }

    //새로 방에들어왔을 때 게임 진행 프로그래스 바 갱신 행동
    private void receiveProgressGame(Object jsonObj){
        HashMap<String, Object> map = new HashMap<String, Object>();
        try {
            JSONObject json = (JSONObject)jsonObj;
            map.put("progressBar", json.get("progressBar")); //프로그레스 바 게이지
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //시간 프로그래스 바 초기화
        int progressInt = (int)map.get("progressBar") + 1;
        progressThread = new ProgressThread(progressHandler);
        progressThread.time = progressInt;
        progressThread.start();
    }

    //게임을 초기화 해주는 함수
    //String nick - 문제 출제자 닉네임, String quiz - 문제
    private void initGame(String nick, String quiz){
        //퀴즈 출제자 태두리 변경
        gameCanvas.drawable_ = false; // 자신이 출제자 아니면 -> 그릴 수 없게 변경
        if (client_.account_.nick_.equals(nick)) {
            gameCanvas.drawable_ = true; // 자신이 출제자 이면 -> 그릴 수 있게 변경
        }
        //Ready 초기화
        for(int i = 0; i < gameClientListViewAdapter.getCount(); i++){
            ((GameClientListViewItem)gameClientListViewAdapter.getItem(i)).isReady_ = false;
        }
        gameClientListViewAdapter.notifyDataSetChanged();

        //캔버스 툴 visible 설정
        textView_quiz.setText(quiz); //퀴즈 입력
        if (gameCanvas.drawable_) { //문제 출제자
            initCanvas();
            textView_quiz.setVisibility(View.VISIBLE); //퀴즈 정답 보이게
            layout_tool.setVisibility(View.VISIBLE); //그림 툴 보이게
        } else {
            textView_quiz.setVisibility(View.INVISIBLE); //퀴즈 정답 안 보이게
            layout_tool.setVisibility(View.INVISIBLE); //그림 툴 안보이게
        }

        //시간 프로그래스 바 초기화
        progressThread = new ProgressThread(progressHandler);
        progressThread.start();
    }

    //방 유저 모두의 캔버스를 초기화 해주는 명령 (새로운 문제 시작 or 게임 시작 시 실행)
    private void initCanvas(){
        //펜 두깨 전송
        gameCanvas.setPenWidth(0.01f);
        String[] keys0 = {"type", "width"};
        Object[] values0 = {"penWidth", 0.01};
        String jsonStr0 = JsonEncode.getInstance().encodeCommandJson("gameCanvas", keys0, values0);
        if (isLiveBinder)
            socketService_.sendMessage(jsonStr0);

        ViewGroup.LayoutParams pen_width_params = imageView_pen_width.getLayoutParams();
        pen_width_params.height = initPenWidth;
        imageView_pen_width.setLayoutParams(pen_width_params);

        //캔버스 색상 전송
        gameCanvas.setCanvasColor(Color.WHITE);
        String[] keys = {"type", "color"};
        Object[] values = {"canvasColor", Color.WHITE};
        String jsonStr = JsonEncode.getInstance().encodeCommandJson("gameCanvas", keys, values);
        if (isLiveBinder)
            socketService_.sendMessage(jsonStr);

        //펜 색상 전송
        gameCanvas.setPenColor(Color.BLACK);
        imageView_pen_width.setBackgroundColor(Color.BLACK);
        String[] keys1 = {"type", "color"};
        Object[] values1 = {"penColor", Color.BLACK};
        String jsonStr1 = JsonEncode.getInstance().encodeCommandJson("gameCanvas", keys1, values1);
        if (isLiveBinder)
            socketService_.sendMessage(jsonStr1);

        //캔버스 지우기
        gameCanvas.clear();
        String[] keys2 = {"type"};
        Object[] values2 = {"clear"};
        String jsonStr2 = JsonEncode.getInstance().encodeCommandJson("gameCanvas", keys2, values2);
        if (isLiveBinder)
            socketService_.sendMessage(jsonStr2);

        //펜 선택으로 변경
        imageView_select_pen.setVisibility(View.VISIBLE);
        imageView_select_eraser.setVisibility(View.INVISIBLE);
        gameCanvas.setToPen();

    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if(dialog.equals(colorPickerDialog_)){
            //펜 선택으로 변경
            imageView_select_pen.setVisibility(View.VISIBLE);
            imageView_select_eraser.setVisibility(View.INVISIBLE);
            gameCanvas.setToPen();
            imageView_pen_width.setBackgroundColor(gameCanvas.getPenColor()); //선 두깨 색상 펜 색상으로
            if(isGameStart_) { //게임이 시작 된 경우에만 서버로 Paint 전송
                String[] keys = {"type"};
                Object[] values = {"toPen"};
                String jsonStr = JsonEncode.getInstance().encodeCommandJson("gameCanvas", keys, values);
                if (isLiveBinder)
                    socketService_.sendMessage(jsonStr);
            }

            if(colorPickerDialog_.choice.equals("pen")){
                // initialColor is the initially-selected color to be shown in the rectangle on the left of the arrow.
                // for example, 0xff000000 is black, 0xff0000ff is blue. Please be aware of the initial 0xff which is the alpha.
                AmbilWarnaDialog colorPickerDialog = new AmbilWarnaDialog(this, gameCanvas.getPenColor(), new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onOk(AmbilWarnaDialog dialog, int color) {
                        // color is the color selected by the user.
                        gameCanvas.setPenColor(color);
                        imageView_pen_width.setBackgroundColor(color);
                        if(isGameStart_) { //게임이 시작 된 경우에만 서버로 Paint 전송
                            String[] keys = {"type", "color"};
                            Object[] values = {"penColor", color};
                            String jsonStr = JsonEncode.getInstance().encodeCommandJson("gameCanvas", keys, values);
                            if (isLiveBinder)
                                socketService_.sendMessage(jsonStr);
                        }
                    }

                    @Override
                    public void onCancel(AmbilWarnaDialog dialog) {
                        // cancel was selected by the user
                    }
                });
                colorPickerDialog.show();
            }else if(colorPickerDialog_.choice.equals("canvas")){
                // initialColor is the initially-selected color to be shown in the rectangle on the left of the arrow.
                // for example, 0xff000000 is black, 0xff0000ff is blue. Please be aware of the initial 0xff which is the alpha.
                AmbilWarnaDialog colorPickerDialog = new AmbilWarnaDialog(this, gameCanvas.getCanvasColor(), new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onOk(AmbilWarnaDialog dialog, int color) {
                        // color is the color selected by the user.
                        gameCanvas.setCanvasColor(color);
                        if(isGameStart_) { //게임이 시작 된 경우에만 서버로 Paint 전송
                            String[] keys = {"type", "color"};
                            Object[] values = {"canvasColor", color};
                            String jsonStr = JsonEncode.getInstance().encodeCommandJson("gameCanvas", keys, values);
                            if (isLiveBinder)
                                socketService_.sendMessage(jsonStr);
                        }
                    }

                    @Override
                    public void onCancel(AmbilWarnaDialog dialog) {
                        // cancel was selected by the user
                    }
                });
                colorPickerDialog.show();
            }
        }
    }

    private ProgressThread progressThread; //progress bar 스레드

    private class ProgressThread extends Thread{ //progress bar 스레드
        public int time = 1; //초
        Handler progressHandler_; //핸들러
        boolean endThread = false;
        ProgressThread(Handler progressHandler){
            progressHandler_ = progressHandler;
        }
        @Override
        public void run() {
            while(time <= gameTime){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
                if(endThread) {
                    progressBar.setProgress(0);
                    return;
                }
                Message msg = progressHandler.obtainMessage();
                msg.arg1 = time++;
                progressHandler.sendMessage(msg);
            }
            if(gameCanvas.drawable_){ //자신이 문제 출제자인 경우 타임 아웃 됬을 때 타임아웃 서버로 전송
                String[] keys = {""};
                Object[] values = {""};
                String jsonStr = JsonEncode.getInstance().encodeCommandJson("timeOutGameRoom", keys, values);
                if (isLiveBinder)
                    socketService_.sendMessage(jsonStr);
            }
            progressBar.setProgress(0);
        }
    }
    //progress bar 핸들러
    private Handler progressHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            int time = msg.arg1;
            progressBar.setProgress(time);
        }
    };

    private void uploadPaintImage(final String fileName, final String filePath){
        HashMap<String, String> postData = new HashMap<>();
        postData.put("filePath", "/usr/share/nginx/html/paint_image/");
        PostDB postDB = new PostDB();
        postDB.putFileData("upload_file.php", postData, filePath, fileName,new OnFinishDBListener() {
            @Override
            public void onSuccess(String output) {
                DebugHandler.log("PaintUploadResult", output);
                if(output.equals("success")){ //파일 업로드 성공했으면 db에 저장
                    HashMap<String, String> postData = new HashMap<String, String>();
                    postData.put("id", client_.account_.id_);
                    postData.put("name", fileName);

                    PostDB postDB = new PostDB();
                    postDB.putFileData("save_paint.php", postData, null, null, new OnFinishDBListener() {
                        @Override
                        public void onSuccess(String output) {
                            //캔버스 캐시 파일 삭제
                            if(new File(filePath).exists())
                                new File(filePath).delete();
                        }
                    });
                }
            }
        });
    }
}
