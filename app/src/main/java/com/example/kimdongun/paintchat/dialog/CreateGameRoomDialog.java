package com.example.kimdongun.paintchat.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
public class CreateGameRoomDialog extends Dialog implements View.OnClickListener{
    private Context context_; //다이얼로그 만들어질 때 context
    private String roomName_; //방 제목
    private boolean isLock_; //비공개 방인가?
    private String password_; //방 비밀번호
    private int maxNum_; //방 최대 인원 (최소 2 최대 8)

    private EditText editText_name; //방 제목
    private CheckBox checkBox_lock; //비밀방 설정
    private LinearLayout layout_password; //비밀번호 설정 레이아웃
    private EditText editText_password; //비밀번호 설정
    private Spinner spinner_num; //방 인원 스피너
    private Button button_create; //방 만들기 버튼

    private Client client_; //클라이언트 정보

    /**********************************
     * CreateGameRoomDialog(Context context, String name) - 각 변수들을 초기화
     * account - 방을 만드는 유저의 정보
     **********************************/
    public CreateGameRoomDialog(Context context, Client client) {
        super(context);
        this.context_ = context;
        this.client_ = client;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //setCanceledOnTouchOutside(false);
        setContentView(R.layout.dialog_create_game_room);

        //초기화
        editText_name = (EditText)findViewById(R.id.editText_name);
        checkBox_lock = (CheckBox)findViewById(R.id.checkBox_lock);
        layout_password = (LinearLayout)findViewById(R.id.layout_password);
        editText_password = (EditText)findViewById(R.id.editText_password);
        spinner_num = (Spinner)findViewById(R.id.spinner_num);
        button_create = (Button)findViewById(R.id.button_create);

        editText_name.setHint(client_.account_.nick_ + "의 게임 방입니다.");
        //비밀방 체크 이벤트
        checkBox_lock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){ //체크 된 경우 -> 비밀방 설정 -> 비밀번호 입력 나와야됨
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)layout_password.getLayoutParams();
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    layout_password.setLayoutParams(params);
                }else{ //체크 해제한 경우 -> 비밀방 X -> 비밀번호 입력 사라짐
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)layout_password.getLayoutParams();
                    params.height = 0;
                    layout_password.setLayoutParams(params);
                    editText_password.setText(null); //입력값 삭제
                }
                isLock_ = isChecked;
            }
        });
        //인원 설정 이벤트
        maxNum_ = 2; //기본 2명
        spinner_num.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                maxNum_ = position + 2; //position 0 => 2명
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        //터치 이벤트
        button_create.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button_create: //방 만들기 버튼
                if(editText_name.getText().toString().length() < 1){ //방 제목 입력 안한 경우
                    roomName_ = editText_name.getHint().toString();
                }else{ //입력 한 경우
                    roomName_ = editText_name.getText().toString();
                }
                if(isLock_){ //비밀방
                    password_ = editText_password.getText().toString();
                    if(password_.length() < 1){ //비밀번호 입력 안한 경우
                        Toast.makeText(context_, "비밀번호를 입력하세요", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }else{
                    password_ = "null";
                }
                createGameRoom();
                break;
        }
    }

    /**********************************
     * createGameRoom() - 방을 만드는 명령을 내림
     **********************************/
    private void createGameRoom(){
        long now = System.currentTimeMillis();
        String key = now + client_.account_.id_;
        key = key.trim();

        String[] keys = {"key", "name", "isLock", "password", "maxNum"};
        Object[] values = {key ,roomName_, isLock_, password_, maxNum_};

        String jsonStr = JsonEncode.getInstance().encodeCommandJson("createGameRoom", keys, values);
        DebugHandler.log(getClass().getName(), jsonStr);

        //서버로 전송
        if(((MainActivity)context_).isLiveBinder)
            ((MainActivity)context_).socketService_.sendMessage(jsonStr);

        dismiss();
    }
}
