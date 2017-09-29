package com.example.kimdongun.paintchat.item;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by KimDongun on 2016-10-03.
 */

//채팅 목록 리스트 아이템
public class ChatRoomListViewItem implements Serializable {
    public String roomKey_; //방 키값
    public ArrayList<String> accountProfileUrl_; //유저들 프로필 Url
    public String accountNick_; //유저들 닉네임
    public int userNum_; //참여 유저 수
    public String msg_; //채팅 메세지
    public String time_; //채팅 시간
    public int msgNum_; //채팅 읽은 숫자
    public long timeLong_; //채팅 시간
    public String type_; //메세지 타입

    public ChatRoomListViewItem(String roomKey, ArrayList<String> accountProfileUrl, String accountNick, int userNum,
                                String msg, String time, int msgNum){
        this.roomKey_ = roomKey;
        this.accountProfileUrl_ = accountProfileUrl;
        this.accountNick_ = accountNick;
        this.userNum_ = userNum;
        this.msg_ = msg;
        this.time_ = time;
        this.msgNum_ = msgNum;
        this.type_ = "chat";
    }
}
