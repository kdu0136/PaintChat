package com.example.kimdongun.paintchat.item;

import java.io.Serializable;

/**
 * Created by KimDongun on 2016-10-03.
 */

//친구 추가 리스트 아이템
public class GameRoomListViewItem implements Serializable {
    public String key_; //방 고유 키값
    public String name_; //게임 방 제목
    public boolean isLock_; //잠금 방인가
    public int num_; //현재 인원
    public int maxNum_; //최대 인원
    public boolean isStart_; //시작한 방인가

    public GameRoomListViewItem(String key, String name, boolean isLock, int num, int maxNum, boolean isStart){
        this.key_ = key;
        this.name_ = name;
        this.isLock_ = isLock;
        this.num_ = num;
        this.maxNum_ = maxNum;
        this.isStart_ = isStart;
    }
}
