package com.example.kimdongun.paintchat.item;

import java.io.Serializable;

/**
 * Created by KimDongun on 2016-10-03.
 */

//채팅 리스트 아이템
public class GameChatListViewItem implements Serializable {
    public String msg_; //메세지
    public int msgColor_; //메세지 색상

    public GameChatListViewItem(String msg,int msgColor){
        this.msg_ = msg;
        this.msgColor_ = msgColor;
    }
}
