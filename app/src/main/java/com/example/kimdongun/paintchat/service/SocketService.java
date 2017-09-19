package com.example.kimdongun.paintchat.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;

import com.example.kimdongun.paintchat.Account;
import com.example.kimdongun.paintchat.DebugHandler;
import com.example.kimdongun.paintchat.HandlerMessageDecode;
import com.example.kimdongun.paintchat.JsonEncode;
import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.SQLiteHandler;
import com.example.kimdongun.paintchat.activity.LoginActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by KimDongun on 2017-08-04.
 */

public class SocketService extends Service implements HandlerMessageDecode, SocketServiceExecutor  {
    // 서비스 종료시 재부팅 딜레이 시간, activity의 활성 시간을 벌어야 한다.
    private static final int REBOOT_DELAY_TIMER = 10 * 1000;

    private static final String IP = "211.110.229.53"; //소켓 서버 아이피
    private static final int PORT = 5000; //포트 번호

    private Account account_; //계정 정보
    private Socket socket_; //클라이언트 소켓
    private String socketStatus_; //소켓 연결 상태 (서비스, 앱)

    public SocketServiceThread socketServiceThread_; //계정 소켓 연결 스레드
    private ConnectThread connectThread_; //소켓 연결 시도 스레드
    private Handler socketHandler_; //스레드 핸들러
    private Handler connectHandler_; //소켓 연결 스레드 핸들러
    private Handler notificationHandler_; //푸시 알림을 위한 핸들러 (socketHandler 가 null 일때 동작)

    private final IBinder mBinder = new ServiceBinder();

    public boolean registerAlarm_; //소켓이 죽었을 때 다시 실행하는 알람 등록 여부

    //서비스 바인더 객체
    public class ServiceBinder extends Binder {
        public SocketService getService() { // 서비스 객체를 리턴
            if(socketServiceThread_ == null){ //소켓 스레드 비어있는 상태
                DebugHandler.log("SocketConnect", "시도");
                connectSocket(); //핸들러가 초기화 되어 있어야 함
            }else{ //소켓 스레드 할당 되어있는 상태
                if(!socketServiceThread_.aliveThread) { //소켓 연결 끊어진 상태
                    DebugHandler.log("SocketConnect", "시도");
                    connectSocket(); //핸들러가 초기화 되어 있어야 함
                }
            }
            return SocketService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        DebugHandler.log(getClass().getName(), "onBind()");

        return mBinder; // 서비스 객체를 리턴
    }

    @Override
    public void onCreate() {
        DebugHandler.log(getClass().getName(), "onCreate()");
        registerAlarm_ = true;
        socketStatus_ = "service";
        unregisterRestartAlarm();

        //초기화
        //소켓 커넥션 스레드전용 핸들러
        connectHandler_ = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                String result = msg.obj.toString();
                DebugHandler.log(getClass().getName(), "connectHandler: " + result);

                sendMessageActivity(result); //액티비티로 소켓 연결 성공 유무 전송

                if (result.equals("connectSuccess")) { //소켓 연결 성공
                    socketServiceThread_ = new SocketServiceThread();
                    socketServiceThread_.start();
                } else {
//                    registerAlarm_ = false;
//                    Client client = Client.getInstance(SocketService.this);
//                    client.logoutClient();
//                    stopSelf();
                }
                connectThread_ = null;
            }
        };

        notificationHandler_ = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                String msgSrt = msg.obj.toString();
                String[] nickMsg = msgSrt.split(",");

                Intent intent = new Intent(SocketService.this, LoginActivity.class);
                intent.putExtra("fragId", 2);
                PendingIntent pendingIntent = PendingIntent.getActivity(SocketService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
                Notification notification = new Notification.Builder(getApplicationContext())
                        .setContentTitle(nickMsg[0])
                        .setContentText(nickMsg[1])
                        .setSmallIcon(R.drawable.mask_btn_on)
                        .setTicker("알림!!")
                        .setContentIntent(pendingIntent)
                        .build();

                //소리 추가
                notification.defaults = Notification.DEFAULT_SOUND;
                //알림 소리를 한번만 내도록
                notification.flags = Notification.FLAG_ONLY_ALERT_ONCE;
                //확인하면 자동으로 알림이 제거 되도록
                notification.flags = Notification.FLAG_AUTO_CANCEL;
                notificationManager.notify(777, notification);
            }
        };

        if (loadAccountInfo()) { //계정 초기화 성공하면
            connectSocket(); //핸들러가 초기화 되어 있어야 함
        }
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        DebugHandler.log(getClass().getName(), "unbindService()");
    }

    @Override
    public void onRebind(Intent intent) {
        DebugHandler.log(getClass().getName(), "onRebind()");
    }

    @Override
    public void onDestroy() {
        DebugHandler.log(getClass().getName(), "onDestroy()");
        DebugHandler.log(getClass().getName(), "registerAlarm_registerAlarm_: " + registerAlarm_);

        if(registerAlarm_) {
            registerRestartAlarm();
        }

        if (socketServiceThread_ != null)
            socketServiceThread_.aliveThread = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
        /*
        START_NOT_STICKY
        system 이 service 를 죽였을 때, pending intent 가 없는 한 service 를 다시 실행시키지 않는다. 가장 safe 한 return type 이다.

        START_STICKY
        system 이 onStartCommand()가 return 한 후 service 를 죽였다면, service 를 다시 create 하면서 onStartCommand() 를 부른다.
        하지만 last intent 를 다시 전달하지는 않는다. pending intent 가 없다면 null intent 가 전달된다.
        MediaPlayer 와 같이 자기 일을 묵묵히 하고 request 가 간혹 들어오는 type 의 service 에 더욱 적합하다.

        START_REDELIVER_INTENT
        system 이 service 를 죽였을 때 서비스를 다시 만들고 onStartCommand() 를 부르는데,
         이 녀석은 last intent 를 전달받는다. 그 다음에 pending intent 가 있다면 추후에 전달된다.
         downloading 같은 즉각적인 resume 이 필요한 service 에 적합하다.
         */
    }

    public void setHandler(Handler handler) {
        socketHandler_ = handler;
        if (socketStatus_.equals("service")) { //액티비티와 핸들러 연결이 있으면
            String[] keys = {"status"};
            Object[] values = {"app"};
            String jsonStr = JsonEncode.getInstance().encodeCommandJson("statusChange", keys, values); //접속 상태를 service -> app 으로 전환
            DebugHandler.log(getClass().getName(), jsonStr);
            //서버로 전송
            sendMessage(jsonStr);
            socketStatus_ = "app";
        }
    }

    /**********************************
     * connectSocket() - 소켓 스레드를 시작하는 함수
     **********************************/
    public void connectSocket() {
        if(connectThread_ == null) {
            connectThread_ = new ConnectThread(connectHandler_);
            connectThread_.start();
        }
    }

    /**********************************
     * sendMessage(final String msg) - 소켓에 메세지 전송하는 함수
     * msg - 전송할 메세지
     **********************************/
    public void sendMessage(final String msg) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    if (socket_ == null) {
                        DebugHandler.log(getClass().getName(), "소켓 비어있음");
                        return;
                    }
                    if (socket_.isClosed()) {
                        DebugHandler.log(getClass().getName(), "소켓 닫힘");
                        return;
                    }
                    DebugHandler.log(getClass().getName(), "SendMessage: " + msg);
                    DataOutputStream dos = new DataOutputStream(socket_.getOutputStream());
                    dos.writeUTF(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    /*****************************************************
     * loadAccountInfo() - 계정 정보 SQLite에서 불러오기
     * 불러오기 실패하면 false 리턴
     *****************************************************/
    private boolean loadAccountInfo() {
        SQLiteHandler handler = new SQLiteHandler(this, "project_one", null, 1);
        ArrayList<ArrayList<Object>> accountArray = handler.select("account_info");
        if (accountArray.size() > 0) { //로그인 기록 있음
            DebugHandler.log(getClass().getName(), "로그인 기록 있음");
            //계정 초기화
            String id = (String) accountArray.get(0).get(0); //계정 아이디
            String profile = (String) accountArray.get(0).get(1); //계정 프로필
            String email = (String) accountArray.get(0).get(2); //계정 이메일
            String nick = (String) accountArray.get(0).get(3); //계정 닉네임
            String type = (String) accountArray.get(0).get(4); //계정 유형
            int confirm = Integer.valueOf((String) accountArray.get(0).get(5)); // 1 이면 인증 완료 계정 0 이면 미 인증 계정
            int search = Integer.valueOf((String) accountArray.get(0).get(6)); // 1 이면 검색 허용 계정 0 이면 검색 거부
            int push = Integer.valueOf((String) accountArray.get(0).get(7)); // 1 이면 푸시 허용 계정 0 이면 미 푸시 거부

            //int no, Drawable profile, String id, String email, String nick,String type, int confirm, int search, int push
            account_ = new Account(id, profile, email, nick, type, confirm, search, push); //계정 정보 초기화

            return true;
        } else {
            DebugHandler.log(getClass().getName(), "로그인 기록 없음");
            stopSelf();
            return false;
        }
    }
    /**
     * 서비스가 시스템에 의해서 또는 강제적으로 종료되었을 때 호출되어
     * 알람을 등록해서 10초 후에 서비스가 실행되도록 한다.
     */
    private void registerRestartAlarm() {
        DebugHandler.log(getClass().getName(), "registerRestartAlarm()");

        Intent intent = new Intent(SocketService.this, RestartServiceBroadCast.class);
        intent.setAction(RestartServiceBroadCast.ACTION_RESTART_SOCKET_SERVICE);

        PendingIntent sender = PendingIntent.getBroadcast(SocketService.this, 0, intent, 0); //브로드 케스트할 intent

        long firstTime = SystemClock.elapsedRealtime(); //현재 시간
        firstTime += REBOOT_DELAY_TIMER; // 10초 후에 알람이벤트 발생

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE); //알람 서비스 등록록
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, REBOOT_DELAY_TIMER, sender);
        /*
        RTC_WAKEUP: 알람 설정 시 지정된 시계 시간에 Intent를 생성하도록 장치를 깨움
        RTC : 명시적인 시간에 Intent를 발생시킬 것이지만, 장치를 깨우지는 않음
        ELAPSED_REALTIME : 장치가 부팅되고 난 이후로 경과된 시간의 양에 기반해 Intent를 발행.
                            장치를 깨우지는 않음. 절전상태도 포함
        ELAPSED_REALTIME_WAKEUP : 장치가 부팅된 이후로 지정된 길이의 시간이 지나면 Intent 발생 후 필요한 경우 장치를 깨움
         */
    }

    /**
     * 기존 등록되어있는 알람을 해제한다.
     */
    private void unregisterRestartAlarm() {
        DebugHandler.log(getClass().getName(), "unregisterRestartAlarm()");

        Intent intent = new Intent(SocketService.this, RestartServiceBroadCast.class);
        intent.setAction(RestartServiceBroadCast.ACTION_RESTART_SOCKET_SERVICE);

        PendingIntent sender = PendingIntent.getBroadcast(SocketService.this, 0, intent, 0);
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.cancel(sender);
    }

    //소켓 연결부분을 담당하는 스레드
    private class ConnectThread extends Thread {
        public ConnectThread(Handler handler) {
        }

        @Override
        public void run() {
            try {
                DebugHandler.log(getClass().getName(), account_.id_ + " 소켓 연결 시도");
                socket_ = new Socket();
                socket_.connect(new InetSocketAddress(IP, PORT), 2000); //연결 시도 timeout 2초 동안 연결 시도 기다림
                DebugHandler.log(getClass().getName(), account_.id_ + " 소켓 연결 성공");

                JSONObject jsonAccount = new JSONObject();
                try {
                    JSONObject accountJson = new JSONObject();
                    accountJson.put("id", account_.id_);
                    accountJson.put("email", account_.email_);
                    accountJson.put("nick", account_.nick_);
                    accountJson.put("type", account_.type_);
                    accountJson.put("status", socketStatus_); //계정 접속 상태 ex)service, app ->서비스에서 접속을 시도 한 건지 어플을 실행시켜서 접속을 시도 한 건지

                    jsonAccount.put("account", accountJson);

                    DebugHandler.log(getClass().getName(), "jsonTest: " + jsonAccount.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                sendMessage(jsonAccount.toString());
            } catch (IOException e) {
                DebugHandler.log(getClass().getName(), account_.id_ + " 소켓 연결 실패");
                Message msg = connectHandler_.obtainMessage(); // 메시지 얻어오기
                msg.obj = "connectFail";
                connectHandler_.sendMessage(msg); //핸들러로 메세지 전송
                e.printStackTrace();
                return;
            }
            Message msg = connectHandler_.obtainMessage(); // 메시지 얻어오기
            msg.obj = "connectSuccess";
            connectHandler_.sendMessage(msg); //핸들러로 메세지 전송
        }
    }

    //소켓에서 데이터를 받는 스레드
    public class SocketServiceThread extends Thread {
        public boolean aliveThread; //스레드 상태

        public SocketServiceThread() {
            aliveThread = true;
        }

        public void run() {
            DebugHandler.log(getClass().getName(), "Receive 스레드 시작");
            try {
                while (aliveThread) {
                    DataInputStream dis = new DataInputStream(socket_.getInputStream());
                    String msg_str = dis.readUTF();
//                    DebugHandler.log(getClass().getName(), "Receive Data: " + msg_str);

                    if(msg_str.equals("pingPongTest")){
                        //핑퐁 테스트 응답
                        String[] keys = {"ping"};
                        Object[] values = {"pong"};
                        String jsonStr = JsonEncode.getInstance().encodeCommandJson("pingPong", keys, values);
                        sendMessage(jsonStr);
                    }else {
                        doReceiveAction(msg_str);
                    }
                }
            } catch (IOException e) {
                aliveThread = false;
                e.printStackTrace();
            }
            DebugHandler.log(getClass().getName(), "Receive 스레드 종료");
//            stopSelf(); //소켓 서비스 종료
        }
    }

    @Override
    public void doReceiveAction(String request) {
//        DebugHandler.log(getClass().getName(), " JSON" + request);
        ArrayList<Object> arrayList = new ArrayList<Object>();
        try {
            JSONObject json = new JSONObject(request);
            String cmdStr = (String)json.get("cmd"); //명령 커맨드 값
            Object requestJson = json.get("request"); //명령 디테일 값ㅐ

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
                    receiveNormalChat(requestObj); //서비스에서 처리할 것 (SQLite에 해당 message 저장 해당 roomKey를 가진 chatRoom 없을 경우 SQLite에 생성)
                    sendMessageActivity(request); //각 엑티비티에서 처리할 것
                    break;

                case "readChat": //채팅 읽음 처리 행동
                    receiveReadChat(requestObj); //서비스에서 처리할 것 (SQLite에 해당 message 읽은 수 줄임)
                    sendMessageActivity(request); //각 엑티비티에서 처리할 것
                    break;

                case "exitServerResult": //서버 접속 종료 행동
                    receiveExitServerResult(); //서비스에서 처리할 것 (소켓 스레드 종료)
                    sendMessageActivity(request); //각 엑티비티에서 처리할 것
                    break;

                default: //이 외 명령 커맨드
                    sendMessageActivity(request); //각 엑티비티에서 처리할 것
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
            map.put("name", json.get("name")); //방 이름
            map.put("type", json.get("type")); //타입  ex)chat / file

            //SQLite에 받은 채팅 메세지 저장
            SQLiteHandler sqLiteHandler = new SQLiteHandler(SocketService.this, "project_one", null, 1);
            Object[] chatMessageValues = {this.account_.id_, map.get("roomKey"), map.get("id"), map.get("msg"),
                    map.get("date"), map.get("num"), 0, map.get("type")};
            sqLiteHandler.insert("chat_message", chatMessageValues);

            //SQLite에서 받은 채팅 메세지가 들어가야 되는 채팅 방 정보 가져옴
            String[] chatRoomColumns = {"my_id", "room_key"};
            Object[] chatRoomValues = {this.account_.id_, map.get("roomKey")};
            ArrayList<ArrayList<Object>> accountArray = sqLiteHandler.select("chat_room", chatRoomColumns, chatRoomValues, "and");

            if (accountArray.size() == 0) {  //해당 키값을 가진진 채팅 방 없음 -> 새로 생성해야됨
                DebugHandler.log(getClass().getName(),"해당 채팅 방 SQLite에 저장 되어 있지 않음");
                Drawable accountProfile = getResources().getDrawable(R.mipmap.ic_launcher);//account_.profile_;             ;

                String roomName = (String)map.get("roomKey");
                String[] accountIdArray = roomName.split(",");
                ArrayList<String> tempList = new ArrayList<>();
                //닉네임 중 자신을 지우기
                for(int i = 0; i < accountIdArray.length; i++) {
                    if(!accountIdArray[i].equals(this.account_.id_)) {
                        DebugHandler.log(getClass().getName(),"accountNickArray: " + accountIdArray[i]);
                        tempList.add(accountIdArray[i]);
                    }
                }
                //방 리스트에 뜨는 친구 닉네임 설정 (방 제목)
                String newRoomName = "";
                for(int i = 0; i < tempList.size(); i++){
                    newRoomName += tempList.get(i);
                    if (i < tempList.size() - 1)
                        newRoomName += ",";
                }

                int user_num = (int)map.get("num"); //채팅방있는 유저 수
                user_num += 1; //본인 추가

                //SQLite에 채팅 방 추가
                //SQLite에 채팅 방 추가
                Object[] arrayList2 = {this.account_.id_, map.get("roomKey"), newRoomName, (int)user_num};
                sqLiteHandler.insert("chat_room", arrayList2);
            }

            if (socketHandler_ == null) { //액티비티와 핸들러 연결이 없으면 메세지 푸시로 처리
                Message msg = notificationHandler_.obtainMessage(); // 메시지 얻어오기
                msg.obj = (String)map.get("nick") + "," + (String)map.get("msg"); // 메시지 정보 설정 (Object 형식)
                notificationHandler_.sendMessage(msg);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void receiveReadChat(Object jsonObj) {
        String receiveRoomKey = null; //채팅방 키값
        String dates = null;//읽은 채팅의 날짜들(인덱스)
        try {
            JSONObject json = (JSONObject)jsonObj;
            receiveRoomKey = json.getString("roomKey"); //채팅방 키값
            dates = json.getString("date"); //읽은 채팅의 날짜들(인덱스)
        } catch (JSONException e) {
            e.printStackTrace();
        }

        SQLiteHandler sqLiteHandler = new SQLiteHandler(this, "project_one", null, 1);

        String[] dateList = dates.split(","); //날짜를 , 로 구분지어서 분리
        for(int i = 0; i < dateList.length; i++){
            //SQLite에서 해당 채팅 불러와서 숫자 줄이기
            String[] whereNames = {"my_id", "room_key", "date"};
            Object[] whereValues = {this.account_.id_, receiveRoomKey, dateList[i]};
            ArrayList<ArrayList<Object>> chatArray = sqLiteHandler.select("chat_message", whereNames, whereValues, "and");
            if(chatArray.size() > 0){ //해당하는 채팅 데이터가 있는 경우
                long readNum = Long.valueOf((String)chatArray.get(0).get(5)); //읽은 수
                readNum -= 1; //읽은 수 하나 줄임

                //chat_message 테이블 업데이트 (읽음 수 변경)
                String[] updateColumnName = {"read_num"};
                String[] updateColumnValue = {String.valueOf(readNum)};
                String[] whereColumnName = {"my_id", "room_key", "date"};
                String[] whereColumnValue = {this.account_.id_, receiveRoomKey, dateList[i]};
                sqLiteHandler.update("chat_message", updateColumnName, updateColumnValue, whereColumnName, whereColumnValue, "and");
            }
        }
    }

    @Override
    public void receiveExitServerResult() {
        DebugHandler.log(getClass().getName(), "서버와 접속이 종료되었습니다.");
        socketServiceThread_.aliveThread = false;
    }

    /**********************************
     * sendMessageActivity(String request) - 각 엑티비티에서 처리해야 하는 명령 엑티비티로 전송
     **********************************/
    private void sendMessageActivity(String request){
        if (socketHandler_ != null) { //액티비티와 핸들러 연결이 있으면
            Message msg = socketHandler_.obtainMessage(); // 메시지 얻어오기
            msg.obj = request; // 메시지 정보 설정 (Object 형식)
            socketHandler_.sendMessage(msg); //핸들러로 메세지 전송
        }
    }
}