package com.example.kimdongun.paintchat;

/**
 * Created by KimDongun on 2017-07-28.
 */

public interface HandlerMessageDecode {
    void doReceiveAction(String request); //받은 json형태의 메세지를 풀어주고 메세지에 맞는 행동을 함
}
