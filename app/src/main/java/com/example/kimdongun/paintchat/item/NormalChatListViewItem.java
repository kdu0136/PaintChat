package com.example.kimdongun.paintchat.item;

import com.example.kimdongun.paintchat.Account;

import java.io.Serializable;

/**
 * Created by KimDongun on 2016-10-03.
 */

//채팅 리스트 아이템
public class NormalChatListViewItem implements Serializable {
    public Account account_; //유저 정보
    public String msg_; //채팅 메세지
    public String date_; //채팅 날짜
    public String time_; //채팅 시간
    public long dateLong_; //채팅 시간 (미리세컨까지)
    public int num_; //채팅 읽은 숫자
    public boolean myMsg_; //채팅이 자신의 채팅인가? (왼쪽 오른쪽 정렬 목적)
    public String type_; //타입  ex)chat / file
    public boolean isFirstMsgDate = false; //그 날 첫 메세지 인가? 날짜 띄우기 용

    public NormalChatListViewItem(Account account,
                                  String msg, String date, String time, long dateLong, int num, boolean myMsg, String type){
        this.account_ = account;
        this.msg_ = msg;
        this.date_ = date;
        this.time_ = time;
        this.dateLong_ = dateLong;
        this.num_ = num;
        this.myMsg_ = myMsg;
        this.type_ = type;
    }
}
