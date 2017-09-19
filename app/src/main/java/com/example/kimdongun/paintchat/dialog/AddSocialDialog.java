package com.example.kimdongun.paintchat.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.kimdongun.paintchat.Account;
import com.example.kimdongun.paintchat.Client;
import com.example.kimdongun.paintchat.CustomBitmapPool;
import com.example.kimdongun.paintchat.DebugHandler;
import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.server.OnFinishDBListener;
import com.example.kimdongun.paintchat.server.PostDB;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;

import jp.wasabeef.glide.transformations.CropCircleTransformation;

/**
 * Created by KimDongun on 2017-07-27.
 */

//생성 할 게임 방 설정을 하는 다이얼 로그
public class AddSocialDialog extends Dialog implements View.OnClickListener{
    private TextView textView_find; //찾기 버튼
    private LinearLayout layout_default; //검색 이전 보여줄 레이아웃
    private LinearLayout layout_no; //검색 실패 보여줄 레이아웃
    private LinearLayout layout_social; //검색 성공 보여줄 레이아웃
    private TextView textView_my_id; //내 계정 아이디
    private TextView textView_no_id; //못 찾은 아이디
    private ImageView imageView_profile; //찾은 프로필 사진
    private TextView textView_social_id; //찾은 아이디
    private Button button_add; //친구 추가 버튼 (친구 추가 안 된 경우)
    private Button button_chat; //채팅 버튼 (친구 추가 된 경우)
    private SearchView searchView; //검색 창
    private String searchText; //검색 텍스트

    private Context context_; //다이얼로그 만들어질 때 context
    private Client client_; //클라이언트 정보

    private Account socialAccount_ = null; //검색 한 계정 정보

    /**********************************
     * AddSocialDialog(Context context, Client client) - 각 변수들을 초기화
     * client - 친구 검색하는 유저의 정보
     **********************************/
    public AddSocialDialog(Context context,Client client) {
        super(context);
        this.context_ = context;
        this.client_ = client;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //setCanceledOnTouchOutside(false);
        setContentView(R.layout.dialog_add_social);

        //초기화
        textView_find = (TextView) findViewById(R.id.textView_find);
        layout_default = (LinearLayout) findViewById(R.id.layout_default);
        layout_no = (LinearLayout) findViewById(R.id.layout_no);
        layout_social = (LinearLayout) findViewById(R.id.layout_social);
        textView_my_id = (TextView) findViewById(R.id.textView_my_id);
        textView_no_id = (TextView) findViewById(R.id.textView_no_id);
        imageView_profile = (ImageView) findViewById(R.id.imageView_profile);
        textView_social_id = (TextView) findViewById(R.id.textView_social_id);
        button_add = (Button) findViewById(R.id.button_add);
        button_chat = (Button) findViewById(R.id.button_chat);
        searchView = (SearchView)findViewById(R.id.searchView);
        searchText = "";

        textView_my_id.setText(client_.account_.id_);


        //검색 이벤트
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchSocial();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchText = newText;
                if(newText.length() > 0){ //길이가 1이상이면 찾기 활성화
                    textView_find.setEnabled(true);
                }else{
                    textView_find.setEnabled(false);
                }
                return false;
            }
        });

        //터치 이벤트
        textView_find.setOnClickListener(this);
        button_add.setOnClickListener(this);
        button_chat.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.textView_find: //찾기 버튼
                searchSocial();
                break;

            case R.id.button_add: //친구 추가 버튼
                addSocial();
                break;

            case R.id.button_chat: //1:1 채팅 버튼
                break;
        }
    }

    /**********************************
     * searchSocial(Context context, Client client) - 입력한 텍스트로 계정 검색
     **********************************/
    public void searchSocial(){
        if(client_.account_.id_.equals(searchText)) { //본인 아이디 검색
            Toast.makeText(context_, "본인의 아이디 입니다.", Toast.LENGTH_SHORT).show();
            layout_default.setVisibility(View.VISIBLE); //검색 이전 보여줄 레이아웃
            layout_no.setVisibility(View.INVISIBLE); //검색 실패 보여줄 레이아웃
            layout_social.setVisibility(View.INVISIBLE); //검색 성공 보여줄 레이아웃
        }else {
            HashMap<String, String> postData = new HashMap<String, String>();
            postData.put("id", client_.account_.id_);
            postData.put("search_id", searchText);

            PostDB postDB = new PostDB();
            postDB.putFileData("search_social.php", postData, null, null, new OnFinishDBListener() {
                @Override
                public void onSuccess(String output) {
                    try {
                        DebugHandler.log(getClass().getName(), "output: " + output);
                        if (output.equals("null")) { //검색 결과 없음
                            layout_default.setVisibility(View.INVISIBLE); //검색 이전 보여줄 레이아웃
                            layout_no.setVisibility(View.VISIBLE); //검색 실패 보여줄 레이아웃
                            layout_social.setVisibility(View.INVISIBLE); //검색 성공 보여줄 레이아웃
                            textView_no_id.setText("'" + searchText + "'");
                        } else {
                            layout_default.setVisibility(View.INVISIBLE); //검색 이전 보여줄 레이아웃
                            layout_no.setVisibility(View.INVISIBLE); //검색 실패 보여줄 레이아웃
                            layout_social.setVisibility(View.VISIBLE); //검색 성공 보여줄 레이아웃
                            JSONObject reader = new JSONObject(output);
                            JSONArray socialArray = reader.getJSONArray("social");
                            //json값 변환
                            for (int i = 0; i < socialArray.length(); i++) {
                                JSONObject object = socialArray.getJSONObject(i);
                                String id = object.getString("id"); //계정 아이디
                                String profile = object.getString("profile"); //계정 프로필
                                String nick = object.getString("nick"); //계정 닉네임
                                String type = object.getString("type"); //계정 유형

                                socialAccount_ = new Account(id, profile, "", nick, type, 1, 1, 1);
                            }
                            String relation = reader.getString("relation");

                            Glide.with(context_)
                                    .load(socialAccount_.profileUrl_ + "Thumb.png")
                                    .thumbnail(0.1f)
                                    .centerCrop()
                                    .placeholder(R.drawable.prepare_image)
                                    .error(R.mipmap.ic_launcher)
                                    .centerCrop()
                                    .crossFade()
                                    .bitmapTransform(new CropCircleTransformation(new CustomBitmapPool()))
                                    .into(imageView_profile);
                            
                            textView_social_id.setText(socialAccount_.id_);
                            if (relation.equals("none")) { //아무 관계 아님 -> 친구 추가 버튼 활성화
                                button_add.setVisibility(View.VISIBLE);
                                button_chat.setVisibility(View.INVISIBLE);
                            } else { //친구 관계 -> 1:1 채팅 버튼 활성화
                                button_add.setVisibility(View.INVISIBLE);
                                button_chat.setVisibility(View.VISIBLE);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    /**********************************
     * addSocial(Context context, Client client) - 검색한 계정 친구 추가
     **********************************/
    public void addSocial(){
        DebugHandler.log(getClass().getName(), "addSocial: { " + " id: "+ client_.account_.id_+ "}");
        DebugHandler.log(getClass().getName(), "addSocial: { " + " search_id: "+ socialAccount_.id_+ "}");

        HashMap<String, String> postData = new HashMap<String, String>();
        postData.put("id", client_.account_.id_);
        postData.put("social_id", socialAccount_.id_);

        PostDB postDB = new PostDB();
        postDB.putFileData("add_social.php", postData, null, null, new OnFinishDBListener() {
            @Override
            public void onSuccess(String output) {
                if (output.equals("success")) { //친구 추가 성공 -> 1:1 채팅 버튼 활성화
                    Toast.makeText(context_, socialAccount_.id_ + "님을 친구로 등록 했습니다.", Toast.LENGTH_SHORT).show();
                    button_add.setVisibility(View.INVISIBLE);
                    button_chat.setVisibility(View.VISIBLE);

                    //친구 목록에 계정 추가
                    client_.socialListViewAdapter.addItem(socialAccount_);
                    client_.socialListViewAdapter.notifyDataSetChanged();
                    client_.recommendSocialListViewAdapter.removeItem(socialAccount_);
                    client_.recommendSocialListViewAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(context_, "친구 등록에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
