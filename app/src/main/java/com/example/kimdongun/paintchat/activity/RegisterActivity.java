package com.example.kimdongun.paintchat.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.network.Network;
import com.example.kimdongun.paintchat.server.OnFinishDBListener;
import com.example.kimdongun.paintchat.server.PostDB;

import java.util.HashMap;

//회원가입 화면
public class RegisterActivity extends AppCompatActivity implements View.OnClickListener{
    private AutoCompleteTextView editText_id; //입력 id
    private EditText editText_password; //입력 password
    private AutoCompleteTextView editText_email; //입력 email
    private AutoCompleteTextView editText_nick; //입력 nick
    private Button button_id_check; //id 중복 체크 버튼
    private Button button_email_check; //이메일 중복 체크 버튼
    private Button button_nick_check; //닉네임 중복 체크 버튼
    private Button button_confirm; //회원가입 버튼

    private boolean confirm_id; //아이디 중복 체크 완료 유무
    private boolean confirm_email; //이메일 중복 체크 완료 유무
    private boolean confirm_nick; //닉네임 중복 체크 완료 유무


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //초기화
        editText_id = (AutoCompleteTextView)findViewById(R.id.editText_id);
        editText_password = (EditText)findViewById(R.id.editText_password);
        editText_email = (AutoCompleteTextView)findViewById(R.id.editText_email);
        editText_nick = (AutoCompleteTextView)findViewById(R.id.editText_nick);
        button_id_check = (Button)findViewById(R.id.button_id_check);
        button_email_check = (Button)findViewById(R.id.button_email_check);
        button_nick_check = (Button)findViewById(R.id.button_nick_check);
        button_confirm = (Button)findViewById(R.id.button_confirm);
        confirm_id = false;
        confirm_email = false;
        confirm_nick = false;

        //터치 이벤트
        button_id_check.setOnClickListener(this);
        button_email_check.setOnClickListener(this);
        button_nick_check.setOnClickListener(this);
        button_confirm.setOnClickListener(this);

        //텍스트 체인지 이벤트
        editText_id.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                confirm_id = false;
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        editText_email.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                confirm_email = false;
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        editText_nick.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                confirm_nick = false;
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.button_id_check: //아이디 중복 체크
                String id = editText_id.getText().toString();
                checkDuplicateData(id, "id", "아이디");
                id = null;
                break;

            case R.id.button_email_check: //이메일 중복 체크
                String email = editText_email.getText().toString();
                checkDuplicateData(email, "email", "이메일");
                email = null;
                break;

            case R.id.button_nick_check: //닉네임 중복 체크
                String nick = editText_nick.getText().toString();
                checkDuplicateData(nick, "nick", "닉네임");
                nick = null;
                break;

            case R.id.button_confirm: //회원 가입 버튼
                registerAccount();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder alert_confirm = new AlertDialog.Builder(RegisterActivity.this);
        alert_confirm.setMessage("회원가입을 취소 하시겠습니까?").setCancelable(false).setPositiveButton("확인",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
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

    /**********************************************************************************************************************
     * checkDuplicateData(final String data, final String dataType, final String dataTypeKR) - 회원가입 입력 값 중복 체크
     * data - 입력 데이터
     * dataType - 데이터 타입 ex)id, email, nick
     * dataTypeKR - 토스트 메세지에 띄울 데이터 타입
     **********************************************************************************************************************/
    private void checkDuplicateData(final String data, final String dataType, final String dataTypeKR){
        if(data.length() < 1) { //데이터 입력 안 했을 경우 메세지
            Toast.makeText(getApplicationContext(), dataTypeKR + "을(를) 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }
        //네트워크 연결 체크
        String netState = Network.getWhatKindOfNetwork(this);
        if(netState.equals(Network.NONE_STATE)){ //연결 안 되있는 경우
            Network.connectNetwork(this);
        }else { //연결 되있는 경우
            HashMap<String, String> postData = new HashMap<String, String>();
            postData.put("check_data_type", dataType);
            postData.put("check_data", data);

            PostDB postDB = new PostDB();
            postDB.putFileData("join_account_duplicate_check.php", postData, null, null, new OnFinishDBListener() {
                @Override
                public void onSuccess(String output) {
                    if(output.equals("null")){ //사용 가능한 데이터일 경우
                        Toast.makeText(getApplicationContext(), data + " 은(는) 사용 가능한 " + dataTypeKR + " 입니다.",Toast.LENGTH_SHORT).show();
                        if(dataType.equals("id")) //아이디 체크 완료
                            confirm_id = true;
                        else if(dataType.equals("email")) //이메일 체크 완료
                            confirm_email = true;
                        else if(dataType.equals("nick")) //닉네임 체크 완료
                            confirm_nick = true;
                    }else{ //사용 불가능한 데이터일 ㅕㄱㅇ우
                        Toast.makeText(getApplicationContext(), data + " 은(는) 이미 존재하는 " + dataTypeKR + " 입니다.",Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    /**********************************************************************************************************************
     * checkDuplicateData(final String data, final String dataType, final String dataTypeKR) - 회원가입 입력 값 중복 체크
     * data - 입력 데이터
     * dataType - 데이터 타입 ex)id, email, nick
     * dataTypeKR - 토스트 메세지에 띄울 데이터 타입
     **********************************************************************************************************************/
    private void registerAccount(){
        String id = editText_id.getText().toString();
        String password = editText_password.getText().toString();
        String email = editText_email.getText().toString();
        String nick = editText_nick.getText().toString();

        //입력 값이 비어 있는 경우
        if(id.length() < 1 || password.length() < 1 || email.length() < 1 ||nick.length() < 1) {
            Toast.makeText(this, "비어있는 회원 정보가 있습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        //중복 체크를 하지 않은 경우
        if(!confirm_id){
            Toast.makeText(this, "아이디 중복체크가 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }else if(!confirm_email){
            Toast.makeText(this, "이메일 중복체크가 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }else if(!confirm_nick){
            Toast.makeText(this, "닉네임 중복체크가 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        //네트워크 연결 체크
        String netState = Network.getWhatKindOfNetwork(this);
        if(netState.equals(Network.NONE_STATE)){ //연결 안 되있는 경우
            Network.connectNetwork(this);
        }else { //연결 되있는 경우
            HashMap<String, String> postData = new HashMap<String, String>();
            postData.put("id", id);
            postData.put("password", password);
            postData.put("email", email);
            postData.put("nick", nick);

            PostDB postDB = new PostDB();
            postDB.putFileData("register_account.php", postData, null, null, new OnFinishDBListener() {
                @Override
                public void onSuccess(String output) {
                    Toast.makeText(getApplicationContext(), "회원가입을 완료 하였습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }
    }
}
