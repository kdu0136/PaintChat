package com.example.kimdongun.paintchat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by KimDongun on 2017-07-28.
 */

public class JsonEncode {
    public static JsonEncode jsonEncode_;

    /**********************************
     * JsonControl() - 각 값 초기화
     **********************************/
    private JsonEncode(){
    }

    /**********************************
     * getInstance() - 객체를 생성하고 반환하는 함수
     **********************************/
    public static JsonEncode getInstance(){
        if(jsonEncode_ == null)
            jsonEncode_ = new JsonEncode();

        return jsonEncode_;
    }

    /**********************************
     * encodeCommandJson(String cmd, String[] keys, Object[] values) - 받은 값을 json 형태로 변환
     * cmd - 명령 커맨드
     * keys - 명령 디테일 키값
     * keys - 명령 디테일 값
     **********************************/
    public String encodeCommandJson(String cmd, String[] keys, Object[] values){
        JSONObject json = new JSONObject();
        try {
            JSONObject requestJson = new JSONObject();
            for(int i = 0; i < keys.length; i++){
                requestJson.put(keys[i], values[i]);
            }
            //각 JsonObject를 최종 Json에 put
            json.put("cmd", cmd);
            json.put("request", requestJson);
            DebugHandler.log(getClass().getName(), "EncodeJson: " + json.toString());

            return json.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**********************************
     * encodeCommandJson(String cmd, JSONArray jsonArray) - 받은 값을 json 형태로 변환
     * cmd - 명령 커맨드
     * jsonArray - 명령 디테일 Json값
     **********************************/
    public String encodeCommandJson(String cmd, JSONArray jsonArray){
        JSONObject json = new JSONObject();
        try {
            json.put("cmd", cmd);
            json.put("request", jsonArray);
            DebugHandler.log(getClass().getName(), "EncodeJson: " + json.toString());

            return json.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
