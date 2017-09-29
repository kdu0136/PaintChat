package com.example.kimdongun.paintchat;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

/**
 * Created by KimDongun on 2017-07-31.
 */

public class SQLiteHandler extends SQLiteOpenHelper {
    /***********************************************************************
     * SQLiteHandler(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) - SQLiteHandler 생성자로 관리할 DB 이름과 버전 정보를 받음
     ***********************************************************************/
    public SQLiteHandler(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        DebugHandler.log(getClass().getName(), "Use DataBase Name: " + getDatabaseName());
    }

    /***********************************************************************
     * onCreate(SQLiteDatabase db) - DB를 새로 생성할 때 호출되는 함수
     ***********************************************************************/
    @Override
    public void onCreate(SQLiteDatabase db) {
        //새로운 테이블 생성
        //로그인 한 계정 정보를 담고 있을 테이블
        db.execSQL("CREATE TABLE account_info (id TEXT, profile TEXT, email TEXT, nick TEXT, type TEXT, confirm INTEGER, search INTEGER, push INTEGER);");
        //계정의 채팅 메세지를 담고 있을 테이블 (본인 id, 채팅방 key, 보낸사람 id, 메세지 내용, 날짜, 읽은 사람 수, 읽은 채팅인지 여부, 메세지 유형)
        db.execSQL("CREATE TABLE chat_message (my_id INTEGER, room_key TEXT, id TEXT, msg TEXT, date INTEGER, read_num INTEGER, is_read INTEGER, type TEXT);");
        //계정의 채팅 방을 담고 있을 테이블 (본인 id, 채팅방 key, 방 이름, 사람 수)
        db.execSQL("CREATE TABLE chat_room (my_id TEXT, room_key TEXT, name TEXT, user_num INTEGER);");

        DebugHandler.log(getClass().getName(), "onCreateSQLiteHandler");
    }

    //SQLite버전이 변경될 때 호출되는 함수
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    /***********************************************************************
     * insert(String tableName, ArrayList<Object> tableValues) - DB에 값 insert
     * tableName - insert 할 table 이름
     * tableValues - 넣을 데이터
     ***********************************************************************/
    public void insert(String tableName, Object[] tableValues){
        // 읽고 쓰기가 가능하게 DB 열기
        SQLiteDatabase db = getWritableDatabase();

        StringBuffer query = new StringBuffer();
        query.append("insert into ");
        query.append(tableName);
        query.append(" values (");
        for(int i = 0; i < tableValues.length; i++){
            query.append("'" + tableValues[i] + "'");
            if(i+1 < tableValues.length){
                query.append(", ");
            }
        }
        query.append(")");
        DebugHandler.log(getClass().getName(), "Query: " + query.toString());

        db.execSQL(query.toString()); //쿼리문 실행
        db.close();
    }

    /***********************************************************************
     * select(String tableName) - DB에 해당 table값 모두 select
     * tableName - select 할 table 이름
     ***********************************************************************/
    public ArrayList<ArrayList<Object>> select(String tableName) {
        // 읽기가 가능하게 DB 열기
        SQLiteDatabase db = getReadableDatabase();
        StringBuffer query = new StringBuffer();
        query.append("SELECT * FROM ");
        query.append(tableName);
        DebugHandler.log(getClass().getName(), "Query: " + query.toString());

        ArrayList<ArrayList<Object>> selectData = new ArrayList<ArrayList<Object>>();

        // DB에 있는 데이터를 쉽게 처리하기 위해 Cursor를 사용하여 테이블에 있는 모든 데이터 출력
        Cursor cursor = db.rawQuery(query.toString(), null);
        while (cursor.moveToNext()) {
            ArrayList<Object> rowData = new ArrayList<Object>();
            for(int i = 0; i < cursor.getColumnCount(); i++){ //힌 얄에 있는 colum 데이터
                rowData.add(cursor.getString(i));
            }
            selectData.add(rowData);
        }

        for(int i = 0; i < selectData.size(); i++){
            String result = i + "번째 열: ";
            for(int j = 0; j < selectData.get(0).size(); j++){
                result += selectData.get(i).get(j) + " / ";
            }
            DebugHandler.log(getClass().getName(), "Select Data: " + result);
        }

        return selectData;
    }

    /***********************************************************************
     * select(String tableName, String[] whereNames, Object[] whereValues, String[] options) - DB에 값 select
     * tableName - select 할 table 이름
     * whereNames - select 할 table 찾는 행 이름
     * whereValues - select 할 table 찾는 행 값
     * option - select 에서 옵션 ex) or / and
     ***********************************************************************/
    public ArrayList<ArrayList<Object>> select(String tableName, String[] whereNames, Object[] whereValues, String option) {
        // 읽기가 가능하게 DB 열기
        SQLiteDatabase db = getReadableDatabase();
        StringBuffer query = new StringBuffer();
        query.append("SELECT * FROM ");
        query.append(tableName);
        query.append(" where ");
        for(int i = 0; i < whereNames.length; i++){
            query.append(whereNames[i] + "='" + whereValues[i] + "'");
            if(i+1 < whereValues.length){
                query.append(" " + option +" ");
            }
        }
        DebugHandler.log(getClass().getName(), "Query: " + query.toString());

        ArrayList<ArrayList<Object>> selectData = new ArrayList<ArrayList<Object>>();

        // DB에 있는 데이터를 쉽게 처리하기 위해 Cursor를 사용하여 테이블에 있는 모든 데이터 출력
        Cursor cursor = db.rawQuery(query.toString(), null);
        while (cursor.moveToNext()) {
            ArrayList<Object> rowData = new ArrayList<Object>();
            for(int i = 0; i < cursor.getColumnCount(); i++){ //힌 얄에 있는 colum 데이터
                rowData.add(cursor.getString(i));
            }
            selectData.add(rowData);
        }

        for(int i = 0; i < selectData.size(); i++){
            String result = i + "번째 열: ";
            for(int j = 0; j < selectData.get(0).size(); j++){
                result += selectData.get(i).get(j) + " / ";
            }
            DebugHandler.log(getClass().getName(), "SelectData: " +  result);
        }

        return selectData;
    }

    /***********************************************************************
     * update(String tableName, String[] columnNames, Object[] columnValues,
     String[] whereNames, Object[] whereValues, String[] options) - DB에 값 update
     * tableName - update 할 table 이름
     * columnNames - update 할 table 행 이름
     * columnValues - update 할 table 행 값
     * whereNames - update 할 table 찾는 행 이름
     * whereValues - update 할 table 찾는 행 값
     * option - where 에서 옵션 ex) or / and
     ***********************************************************************/
    public void update(String tableName, String[] columnNames, Object[] columnValues,
                       String[] whereNames, Object[] whereValues, String option) {
        SQLiteDatabase db = getWritableDatabase();
        // 입력한 항목과 일치하는 행의 정보 수정
        StringBuffer query = new StringBuffer();
        query.append("update ");
        query.append(tableName);
        query.append(" set ");
        for(int i = 0; i < columnNames.length; i++){
            query.append(columnNames[i].trim() + "='" + columnValues[i] + "'");
            if(i+1 < columnValues.length){
                query.append(", ");
            }
        }
        query.append(" where ");
        for(int i = 0; i < whereNames.length; i++){
            query.append(whereNames[i] + "='" + whereValues[i] + "'");
            if(i+1 < whereValues.length){
                query.append(" " + option +" ");
            }
        }
        DebugHandler.log(getClass().getName(), "Query: " + query.toString());
        db.execSQL(query.toString()); //쿼리문 실행
        db.close();
    }

    /***********************************************************************
     * delete(String tableName, String[] whereNames, Object[] whereValues, String[] options) - DB에 값 delete
     * tableName - delete 할 table 이름
     * whereNames - delete 할 table 찾는 행 이름
     * whereValues - delete 할 table 찾는 행 값
     * option - delete 에서 옵션 ex) or / and
     ***********************************************************************/
    public void delete(String tableName, String[] whereNames, Object[] whereValues, String option) {
        SQLiteDatabase db = getWritableDatabase();
        // 입력한 항목과 일치하는 행의 정보 수정
        StringBuffer query = new StringBuffer();
        query.append("delete from ");
        query.append(tableName);
        query.append(" where ");
        for(int i = 0; i < whereNames.length; i++){
            query.append(whereNames[i] + "='" + whereValues[i] + "'");
            if(i+1 < whereValues.length){
                query.append(" " + option +" ");
            }
        }
        DebugHandler.log(getClass().getName(), "Query: " + query.toString());
        db.execSQL(query.toString()); //쿼리문 실행
        db.close();
    }

    /***********************************************************************
     * deleteAll(String tableName) - DB에 값 모두 delete
     * tableName - delete 할 table 이름
     ***********************************************************************/
    public void deleteAll(String tableName) {
        SQLiteDatabase db = getWritableDatabase();
        // 입력한 항목과 일치하는 행의 정보 수정
        StringBuffer query = new StringBuffer();
        query.append("delete from ");
        query.append(tableName);
        DebugHandler.log(getClass().getName(), "Query: " + query.toString());
        db.execSQL(query.toString()); //쿼리문 실행
        db.close();
    }

    public ArrayList<Object> selectNoneReadChatMsgNum(String roomKey){
        // 읽기가 가능하게 DB 열기
        SQLiteDatabase db = getReadableDatabase();
        StringBuffer query = new StringBuffer();
        query.append("SELECT * FROM chat_message where room_key='");
        query.append(roomKey);
        query.append("'");
        DebugHandler.log(getClass().getName(), "Query: " + query.toString());

        ArrayList<ArrayList<Object>> selectData = new ArrayList<ArrayList<Object>>();

        // DB에 있는 데이터를 쉽게 처리하기 위해 Cursor를 사용하여 테이블에 있는 모든 데이터 출력
        Cursor cursor = db.rawQuery(query.toString(), null);
        while (cursor.moveToNext()) {
            ArrayList<Object> rowData = new ArrayList<Object>();
            for(int i = 0; i < cursor.getColumnCount(); i++){ //힌 얄에 있는 colum 데이터
                rowData.add(cursor.getString(i));
            }
            selectData.add(rowData);
        }

        if(selectData.size() > 0){
            ArrayList<Object> returnArray = new ArrayList<Object>();

            long numMsg = 0; //읽지 않은 메세지 갯수
            for(int i = 0; i < selectData.size(); i++){
                if(Long.valueOf((String)selectData.get(i).get(6)) == 0) //읽지 않은 메세지
                    numMsg++;
            }

            returnArray.add(numMsg); //메세지 갯수
            returnArray.add(selectData.get(selectData.size() - 1).get(3)); //마지막 메세지
            returnArray.add(selectData.get(selectData.size() - 1).get(4)); //마지막 시간
            returnArray.add(selectData.get(selectData.size() - 1).get(7)); //마지막 메세지 유형

            DebugHandler.log(getClass().getName(), "Num None Read Message: " + numMsg);
            DebugHandler.log(getClass().getName(), "Last Message: " + selectData.get(selectData.size() - 1).get(3));
            DebugHandler.log(getClass().getName(), "Last Message TIme: " + selectData.get(selectData.size() - 1).get(4));

            return returnArray;
        }
        return null;
    }
}

