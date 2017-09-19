package com.example.kimdongun.paintchat.service;

/**
 * Created by KimDongun on 2017-08-11.
 */

//모든 엑티비티에서 공통으로 수행되어야 하는 명령들
public interface SocketServiceExecutor {
    void receiveNormalChat(Object jsonObj); //받은 커맨드가 채팅일 경우 행동
    void receiveReadChat(Object jsonObj); //받은 커맨드가 채팅 읽음 처리 행동
    void receiveExitServerResult(); //받은 커맨드가 서버와 접속 종료 결과 처리 행동
}
