package com.example.kimdongun.paintchat.item;

import com.example.kimdongun.paintchat.Account;

import java.io.Serializable;

/**
 * Created by KimDongun on 2016-10-03.
 */

//채팅에 친구 초대 리스트 아이템
public class InviteChatListViewItem implements Serializable {
    public Account account_; //유저 정보
    public boolean isChecked_; //체크 유무

    public InviteChatListViewItem(Account account){
        this.account_ = account;
        this.isChecked_ = false;
    }
}
