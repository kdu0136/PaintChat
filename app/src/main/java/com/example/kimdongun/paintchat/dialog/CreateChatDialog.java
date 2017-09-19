package com.example.kimdongun.paintchat.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.bumptech.glide.Glide;
import com.example.kimdongun.paintchat.Account;
import com.example.kimdongun.paintchat.Client;
import com.example.kimdongun.paintchat.JsonEncode;
import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.activity.MainActivity;
import com.example.kimdongun.paintchat.adapter.InviteChatListViewAdapter;
import com.example.kimdongun.paintchat.adapter.SocialListViewAdapter;
import com.example.kimdongun.paintchat.item.InviteChatListViewItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by KimDongun on 2017-07-27.
 */

//생성 할 게임 방 설정을 하는 다이얼 로그
public class CreateChatDialog extends Dialog implements View.OnClickListener, AdapterView.OnItemClickListener{
    private Context context_; //다이얼로그 만들어질 때 context
    private Client client_; //클라이언트 정보

    private ListView listView; //초대 목록 리스트 뷰
    private InviteChatListViewAdapter adapter; //초대 목록 리스트뷰 어댑터
    private Button button_invite; //초대 버튼

    /**********************************
     * AddSocialDialog(Context context, Client client) - 각 변수들을 초기화
     * client - 친구 검색하는 유저의 정보
     **********************************/
    public CreateChatDialog(Context context, Client client) {
        super(context);
        this.context_ = context;
        this.client_ = client;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //setCanceledOnTouchOutside(false);
        setContentView(R.layout.dialog_invite_chat);

        //초기화
        listView = (ListView)findViewById(R.id.listView);
        button_invite = (Button)findViewById(R.id.button_invite);
        adapter = new InviteChatListViewAdapter(context_, Glide.with(context_));

        listView.setAdapter(adapter);

        //친구 목록 리스트를 이용하여 친구 초대 리스트 어탭터 초기화
        SocialListViewAdapter socialListViewAdapter = client_.socialListViewAdapter;
        for(int i = 0; i < socialListViewAdapter.getCount(); i++){
            Account socialAccount = (Account)socialListViewAdapter.getItem(i);
            InviteChatListViewItem inviteChatListViewItem = new InviteChatListViewItem(socialAccount);
            adapter.addItem(inviteChatListViewItem);
        }
        adapter.notifyDataSetChanged();

        //터치 이벤트
        listView.setOnItemClickListener(this);
        button_invite.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button_invite:
                createChatRoom();
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
        InviteChatListViewItem inviteChatListViewItem = (InviteChatListViewItem)adapter.getItem(index);
        inviteChatListViewItem.isChecked_ = !inviteChatListViewItem.isChecked_;
        adapter.notifyDataSetChanged();
    }

    /**********************************
     * createChatRoom() - 선택 유저와 채팅방 개설
     **********************************/
    public void createChatRoom(){
        ArrayList<String> listID = new ArrayList<String>();
        listID.add(client_.account_.id_);

        ArrayList<String> listNick = new ArrayList<String>();
        listNick.add(client_.account_.nick_);

        ArrayList<InviteChatListViewItem> checkedList = adapter.getCheckedItem();
        for(int i = 0; i < checkedList.size(); i++){
            listID.add(checkedList.get(i).account_.id_);
            listNick.add(checkedList.get(i).account_.nick_);
        }

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
            for(int i = 0; i < checkedList.size(); i++) {
                JSONObject idJson = new JSONObject();
                idJson.put("id", checkedList.get(i).account_.id_); //채팅방에 입장 할 유저 아이디
                jsonArr.put(idJson); //유저 아이디를 JsonArray에 추가
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //정보를 json으로 변환
        String jsonStr = JsonEncode.getInstance().encodeCommandJson("createChatRoom", jsonArr);
        //서버로 전송
        if(((MainActivity)context_).isLiveBinder)
            ((MainActivity)context_).socketService_.sendMessage(jsonStr);

        dismiss();
    }
}
