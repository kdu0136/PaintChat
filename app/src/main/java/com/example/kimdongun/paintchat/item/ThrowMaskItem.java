package com.example.kimdongun.paintchat.item;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by KimDongun on 2017-09-07.
 */

public class ThrowMaskItem {
    public String maskName; //마스크 이름
    public LatLng latLng; //마스크 위도,경도

    public ThrowMaskItem(String maskName, double lat, double lng){
        this.maskName = maskName;
        latLng = new LatLng(lat, lng);
    }
}
