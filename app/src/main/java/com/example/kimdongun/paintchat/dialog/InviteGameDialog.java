package com.example.kimdongun.paintchat.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.kimdongun.paintchat.Account;
import com.example.kimdongun.paintchat.Client;
import com.example.kimdongun.paintchat.JsonEncode;
import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.activity.GameRoomActivity;
import com.example.kimdongun.paintchat.adapter.InviteChatListViewAdapter;
import com.example.kimdongun.paintchat.adapter.SocialListViewAdapter;
import com.example.kimdongun.paintchat.item.InviteChatListViewItem;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by KimDongun on 2017-07-27.
 */

//생성 할 게임 방 설정을 하는 다이얼 로그
public class InviteGameDialog extends Dialog implements View.OnClickListener, AdapterView.OnItemClickListener{
    private Context context_; //다이얼로그 만들어질 때 context
    private Client client_; //클라이언트 정보

    private ListView listView; //초대 목록 리스트 뷰
    private InviteChatListViewAdapter adapter; //초대 목록 리스트뷰 어댑터
    private Button button_invite; //초대 버튼

    private String gameRoomKey_; //방 키값

    /**********************************
     * AddSocialDialog(Context context, Client client) - 각 변수들을 초기화
     * client - 친구 검색하는 유저의 정보
     **********************************/
    public InviteGameDialog(Context context, Client client, String roomKey) {
        super(context);
        this.context_ = context;
        this.client_ = client;
        this.gameRoomKey_ = roomKey;
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
                inviteGameRoom();
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
     * inviteGameRoom() - 선택 유저에게 게임 방 초대 메세지 전송
     **********************************/
    public void inviteGameRoom(){
        ArrayList<InviteChatListViewItem> checkedList = adapter.getCheckedItem();
        for(int index = 0; index < checkedList.size(); index++) {
            ArrayList<String> listID = new ArrayList<String>();
            listID.add(client_.account_.id_);
            listID.add(checkedList.get(index).account_.id_); //아이디 추가

            //정렬
            Collections.sort(listID);

            //방 키값 생성
            String roomKey = "";
            for(int i = 0; i < listID.size(); i++){
                roomKey += listID.get(i);
                if(i < listID.size() - 1){
                    roomKey += ",";
                }
            }
            roomKey.trim();

            //정보를 json으로 변환
            String[] keys = {"roomKey", "id", "nick", "msg", "type"};
            Object[] values = {roomKey, client_.account_.id_, client_.account_.nick_, gameRoomKey_, "invite"};
            String jsonStr = JsonEncode.getInstance().encodeCommandJson("normalChat", keys, values);
            //서버로 전송
            if(((GameRoomActivity)context_).isLiveBinder)
                ((GameRoomActivity)context_).socketService_.sendMessage(jsonStr);
        }
        Toast.makeText(context_, "초대 메세지를 보냈습니다.", Toast.LENGTH_SHORT).show();

        dismiss();
    }
}
