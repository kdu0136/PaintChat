package com.example.kimdongun.paintchat;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by KimDongun on 2017-07-26.
 */

//계정 정보
public class Account implements Serializable {
    public String id_; //유저 아이디
    public String profileUrl_; //유저 프로필 url
    public String email_; //유저 이메일
    public String nick_; //유저 닉네임
    public String type_; //계정 종류 ex)google, facebook, local(자체 회원)
    public int confirm_; //인증 계정 유무
    public int search_; //아이디 검색 허용 유무
    public int push_; //푸시 알림 허용 유무

    /**********************************
     * Account(String id, String email,String nick, String type, int confirm) - 각 변수들을 초기화
     **********************************/
    public Account(String id, String profileUrl, String email, String nick,String type, int confirm, int search, int push) {
        this.id_ = id;
        this.profileUrl_ = profileUrl;
        this.email_ = email;
        this.nick_ = nick;
        this.type_ = type;
        this.confirm_ = confirm;
        this.search_ = search;
        this.push_ = push;
    }

    /**********************************
     * accountJSON() - 계정 정보를 Json값으로 변환
     **********************************/
    public String accountToJSON_() {
        JSONObject json = new JSONObject();
        try {
            JSONObject accountJson = new JSONObject();
            accountJson.put("id", this.id_);
            accountJson.put("email", this.email_);
            accountJson.put("nick", this.nick_);
            accountJson.put("type", this.type_);

            json.put("account", accountJson);

            Log.d("jsonTest", json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            return "null";
        }finally {
            return json.toString();
        }
    }
}
