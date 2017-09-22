package com.example.kimdongun.paintchat.item;

import android.widget.ImageView;

/**
 * Created by KimDongun on 2017-09-07.
 */

public class MaskItem {
    public String maskName; //마스크 이름
    public String maskOldID; //마스크 원래 주인
    public String maskMemo; //마스크 메모
    public String maskDate; //마스크 습득 날짜
    public float azimuth; //마스크 방위각
    public ImageView imageView; //마스크 이미지 뷰
    public int floor; //마스트가 위치하고 있는 층 수

    public MaskItem(String maskName, String maskOldID,String maskMemo,String maskDate,ImageView imageView){
        this.maskName = maskName;
        this.maskOldID = maskOldID;
        this.maskMemo = maskMemo;
        this.maskDate = maskDate;
        this.imageView = imageView;
        this.azimuth = 0;
        this.floor = 0;
    }

    public void deleteMask(){
        this.maskName = null;
        this.maskOldID = null;
        this.maskMemo = null;
        this.maskDate = null;
        this.imageView = null;
    }
}
