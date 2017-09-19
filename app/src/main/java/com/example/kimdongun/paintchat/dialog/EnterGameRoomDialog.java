package com.example.kimdongun.paintchat.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kimdongun.paintchat.Client;
import com.example.kimdongun.paintchat.DebugHandler;
import com.example.kimdongun.paintchat.JsonEncode;
import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.activity.MainActivity;

/**
 * Created by KimDongun on 2017-07-27.
 */

//생성 할 게임 방 설정을 하는 다이얼 로그
public class EnterGameRoomDialog extends Dialog implements View.OnClickListener{
    private Context context_; //다이얼로그 만들어질 때 context
    private String roomKey_; //방 키값
    private String roomName_; //방 제목
    private String password_; //방 비밀번호

    private TextView textView_name; //방 제목
    private EditText editText_password; //비밀번호
    private Button button_enter; //방 입장 버튼

    private Client client_; //클라이언트 정보

    /**********************************
     * CreateGameRoomDialog(Context context, String name) - 각 변수들을 초기화
     * account - 방을 만드는 유저의 정보
     **********************************/
    public EnterGameRoomDialog(Context context, Client client, String roomKey, String roomName) {
        super(context);
        this.context_ = context;
        this.client_ = client;
        this.roomKey_ = roomKey;
        this.roomName_ = roomName;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //setCaneledOnTouchOutside(false);
        setContentView(R.layout.dialog_enter_game_room);

        //초기화
        textView_name = (TextView)findViewById(R.id.textView_name);
        editText_password = (EditText)findViewById(R.id.editText_password);
        button_enter = (Button)findViewById(R.id.button_enter);

        textView_name.setText(roomName_);

        //터치 이벤트
        button_enter.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button_enter: //방 입장 버튼
                password_ = editText_password.getText().toString();
                if(password_.length() < 1){ //비밀번호 입력 안한 경우
                    Toast.makeText(context_, "비밀번호를 입력하세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                EnterGameRoom();
                break;
        }
    }

    /**********************************
     * EnterGameRoom() - 방 입장하는 명령을 내림
     **********************************/
    private void EnterGameRoom(){
        String[] keys = {"key", "password"};
        Object[] values = {roomKey_, password_};

        String jsonStr = JsonEncode.getInstance().encodeCommandJson("enterGameRoom", keys, values);
        DebugHandler.log(getClass().getName(), jsonStr);

        //서버로 전송
        if (((MainActivity) context_).isLiveBinder)
            ((MainActivity) context_).socketService_.sendMessage(jsonStr);

        dismiss();
    }
}
