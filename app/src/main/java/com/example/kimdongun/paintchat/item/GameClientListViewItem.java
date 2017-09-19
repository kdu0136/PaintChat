package com.example.kimdongun.paintchat.item;

import java.io.Serializable;

/**
 * Created by KimDongun on 2016-10-03.
 */

//유저 리스트 아이템
public class GameClientListViewItem implements Serializable {
    public String id_; //계정 아이디
    public String nick_; //계정 닉네임
    public boolean isReady_; //레디 상태인지
    public boolean isHost_; //방장인가?

    public GameClientListViewItem(String id, String nick, boolean isReady, boolean isHost_){
        this.id_ = id;
        this.nick_ = nick;
        this.isHost_ = isHost_;
        this.isReady_ = isReady;
    }
}
