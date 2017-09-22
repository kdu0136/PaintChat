package com.example.kimdongun.paintchat;

import android.content.Context;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.example.kimdongun.paintchat.adapter.ChatRoomListViewAdapter;
import com.example.kimdongun.paintchat.adapter.GameRoomListViewAdapter;
import com.example.kimdongun.paintchat.adapter.SocialListViewAdapter;
import com.example.kimdongun.paintchat.item.ChatRoomListViewItem;
import com.example.kimdongun.paintchat.server.OnFinishDBListener;
import com.example.kimdongun.paintchat.server.PostDB;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by KimDongun on 2017-07-26.
 */

//계정 정보를 가지고 있는 클라이언트 객채
public class Client {
    private static Client client_; //클라이언트 전역 변수

    private Context context_; //현재 context 값
    public Account account_; //계졍 정보

    public GameRoomListViewAdapter gameRoomListViewAdapter; //게임 방 리스트 뷰 어댑터
    public ChatRoomListViewAdapter chatRoomListViewAdapter; //채팅 방 리스트 뷰 어댑터
    public SocialListViewAdapter socialListViewAdapter; //친구 리스트 뷰 어댑터
    public SocialListViewAdapter recommendSocialListViewAdapter; //추천 친구 리스트 뷰 어댑터

    /**********************************
     * Account() - 각 변수들을 초기화
     **********************************/
    public Client(Context context){
        this.context_ = context;

        gameRoomListViewAdapter = new GameRoomListViewAdapter(this.context_);
        chatRoomListViewAdapter = new ChatRoomListViewAdapter(this.context_, Glide.with(this.context_));
        socialListViewAdapter = new SocialListViewAdapter(this.context_, Glide.with(this.context_));
        recommendSocialListViewAdapter = new SocialListViewAdapter(this.context_, Glide.with(this.context_));
    }

    /**********************************
     * getInstance() - 클라이언트 매니저 객체를 생성하고 반환하는 함수
     **********************************/
    public static Client getInstance(Context context) {
        //account_ null값이면 초기화
        if(client_ == null) {
            client_ = new Client(context);
        }

        client_.context_ = context;
        client_.setContext();
        return client_;
    }

    /**********************************
     * loginClient(iString id, String profile, String email, String nick, String type) - 클라이언트 로그인 했을 때 작업
     **********************************/
    public void loginClient(String id, String profile, String email, String nick, String type, int confirm, int search, int push){
        this.account_ = new Account(id, profile, email, nick, type, confirm, search, push);
        DebugHandler.log("Client", "account_ Profile: " + account_.profileUrl_);
        loadSocialList();
    }

    /**********************************
     * logoutClient() - 클라이언트 로그아웃 했을 때 작업
     **********************************/
    public void logoutClient(){
        if(this.account_ == null) return;

        DebugHandler.log("Client", "LogoutClient");
        //SQLite 데이터 삭제
        SQLiteHandler handler = new SQLiteHandler(context_, "project_one", null, 1);
        handler.deleteAll("account_info");

        HashMap<String, String> postData = new HashMap<String, String>();
        postData.put("id", String.valueOf(this.account_.id_));

        PostDB postDB = new PostDB();
        postDB.putFileData("logout.php", postData, null, null, new OnFinishDBListener() {
            @Override
            public void onSuccess(String output) {
            }
        });

        //각 어탭터 초기화
        gameRoomListViewAdapter = new GameRoomListViewAdapter(this.context_);
        chatRoomListViewAdapter = new ChatRoomListViewAdapter(this.context_, Glide.with(this.context_));
        socialListViewAdapter = new SocialListViewAdapter(this.context_, Glide.with(this.context_));
        recommendSocialListViewAdapter = new SocialListViewAdapter(this.context_, Glide.with(this.context_));
    }

    /**********************************
     * loadSocialList() - 친구 정보를 서버로 부터 불러오와서 갱신하는 함수
     **********************************/
    public void loadSocialList(){
        HashMap<String, String> postData = new HashMap<String, String>();
        postData.put("id", this.account_.id_);

        PostDB postDB = new PostDB();
        postDB.putFileData("load_social.php", postData, null, null, new OnFinishDBListener() {
            @Override
            public void onSuccess(String output) {
                try {
                    socialListViewAdapter.removeAll();
                    recommendSocialListViewAdapter.removeAll();
                    DebugHandler.log(getClass().getName(), "output: " + output);
                    JSONObject reader = new JSONObject(output);
                    if(reader.get("social").equals(null)){ //친구 없는 경우

                    }else{ //친구 있는 경우
                        //json값 변환
                        JSONArray socialArray = reader.getJSONArray("social");
                        for (int i = 0; i < socialArray.length(); i++) {
                            JSONObject object = socialArray.getJSONObject(i);
                            String id = object.getString("social_id"); //계정 아이디
                            String nick = object.getString("social_nick"); //계정 닉네임
                            String profile = object.getString("social_profile"); //계정 프로필 주소
                            String type = object.getString("social_type"); //계정 유형

                            Account socialAccount = new Account(id, profile, "", nick, type, 1, 1, 1);
                            socialListViewAdapter.addItem(socialAccount);
                        }
                    }

                    if(reader.get("recommend").equals(null)){ //추천 친구 없는 경우

                    }else{ //추천 친구 있는 경우
                        //json값 변환
                        JSONArray recommendSocialArray = reader.getJSONArray("recommend");
                        for (int i = 0; i < recommendSocialArray.length(); i++) {
                            JSONObject object = recommendSocialArray.getJSONObject(i);
                            String id = object.getString("social_id"); //계정 아이디
                            String profile = object.getString("social_profile"); //계정 프로필 주소
                            String nick = object.getString("social_nick"); //계정 닉네임
                            String type = object.getString("social_type"); //계정 유형

                            Account recommendSocialAccount = new Account(id, profile, "", nick, type, 1, 1, 1);
                            recommendSocialListViewAdapter.addItem(recommendSocialAccount);
                        }
                    }
                    socialListViewAdapter.notifyDataSetChanged();
                    recommendSocialListViewAdapter.notifyDataSetChanged();
                    Log.d(getClass().getName(), "social size: " + socialListViewAdapter.getCount() +
                            "recommend size: " + recommendSocialListViewAdapter.getCount());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                loadChatRoomList();
            }
        });
    }


    /**********************************
     * loadChaRoomList() - 채팅 방 정보를 SQLite로 부터 불러오와서 갱신하는 함수
     **********************************/
    public void loadChatRoomList(){
        chatRoomListViewAdapter.removeAll();

        SQLiteHandler sqLiteHandler = new SQLiteHandler(context_, "project_one", null, 1);
        String[] whereNames = {"my_id"};
        Object[] whereValues = {this.account_.id_};
        ArrayList<ArrayList<Object>> accountArray = sqLiteHandler.select("chat_room", whereNames, whereValues, "and");

        ArrayList<ChatRoomListViewItem> chatRoomList = new ArrayList<>(); //시간 순서대로 저장 된 채팅방 리스트

        for(int i = 0; i < accountArray.size(); i++){
            String roomKey = (String)accountArray.get(i).get(1); //채팅방 키값
            String roomName = (String)accountArray.get(i).get(2); //채팅방 이름
            long user_num = Long.valueOf((String)accountArray.get(i).get(3)); //채팅방에 참여하고 있는 유저 수

            ArrayList<Object> roomListArray = sqLiteHandler.selectNoneReadChatMsgNum(roomKey);

            long msg_num = 0; //채팅방에 보일 읽지 않은 메세지 수
            String msg = ""; //채팅방에 보일 메세지
            long now = 0; //채팅방에 보일 시간
            if(roomListArray != null){
                msg_num = (long)roomListArray.get(0); //채팅방에 보일 읽지 않은 메세지 수
                msg = (String)roomListArray.get(1); //채팅방에 보일 메세지
                now = Long.valueOf((String)roomListArray.get(2)); //채팅방에 보일 시간
            }
            Date date = new Date(now);
            SimpleDateFormat sdfTime = new SimpleDateFormat("aa hh:mm");
            String strTime = sdfTime.format(date);

            String[] accountIdkArray = roomName.split(",");
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

            ChatRoomListViewItem chatRoomListViewItem = new ChatRoomListViewItem(roomKey, profileList, newRoomName, (int)user_num,
                    msg, strTime, (int)msg_num);
            chatRoomListViewItem.timeLong_ = now;

            chatRoomList.add(chatRoomListViewItem); //채팅 방 저장
        }

        Collections.sort(chatRoomList, new CompareTimeDesc());

        chatRoomListViewAdapter.addAll(chatRoomList);
        chatRoomListViewAdapter.notifyDataSetChanged();
    }

    //시간을 기준으로 내림차순 정렬
    static class CompareTimeDesc implements Comparator<ChatRoomListViewItem> {
        @Override
        public int compare(ChatRoomListViewItem o1, ChatRoomListViewItem o2) {
            // TODO Auto-generated method stub
            return o1.timeLong_ > o2.timeLong_ ? -1 : o1.timeLong_ < o2.timeLong_ ? 1:0;
        }
    }

    //getter
    public Context getContext() { return context_; }

    private void setContext(){
        gameRoomListViewAdapter.setMyContext(null);
        chatRoomListViewAdapter.setMyContext(null, null);
        socialListViewAdapter.setMyContext(null, null);
        recommendSocialListViewAdapter.setMyContext(null, null);

        gameRoomListViewAdapter.setMyContext(client_.context_);
        chatRoomListViewAdapter.setMyContext(client_.context_, Glide.with(client_.context_));
        socialListViewAdapter.setMyContext(client_.context_, Glide.with(client_.context_));
        recommendSocialListViewAdapter.setMyContext(client_.context_, Glide.with(client_.context_));
    }
}
