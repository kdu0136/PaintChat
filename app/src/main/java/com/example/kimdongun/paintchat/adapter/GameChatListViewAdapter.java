package com.example.kimdongun.paintchat.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.item.GameChatListViewItem;

import java.util.ArrayList;

/**
 * Created by KimDongun on 2016-10-03.
 */

//친구 목록 리스트뷰 어댑터
public class GameChatListViewAdapter extends BaseAdapter {
    public ArrayList<GameChatListViewItem> myListItem; //아이템 리스트
    private Context myContext = null;
    private LayoutInflater myInflater;

    public GameChatListViewAdapter(Context myContext){
        super();
        this.myContext = myContext;
        this.myListItem = new ArrayList<GameChatListViewItem>();
    }
    @Override
    public int getCount() {
        return myListItem.size();
    }

    @Override
    public Object getItem(int index) {
        return myListItem.get(index);
    }

    @Override
    public long getItemId(int index) {
        return index;
    }

    @Override
    public View getView(int index, View convertView, ViewGroup parent) {
        View view = convertView;
        final ViewHolder viewHolder;

        // "listview_item" Layout을 inflate하여 convertView 참조 획득.
        if(view == null){
            // 레이아웃을 inflate시켜 새로운 view 생성.
            myInflater = (LayoutInflater)myContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = myInflater.inflate(R.layout.item_game_chat_listview, parent, false);
            // Holder pattern을 위한 wrapper 초기화 (row를 base 클래스로 지정)
            viewHolder = new ViewHolder(view);
            // row에 viewWrapper object를 tag함 (나중에 row를 재활용 할때 필요)
            view.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder)convertView.getTag();
        }

        // getView() 호출시 인자로 전달된 position을 이용해 현재 사용중인 RowModel의 객체 model 얻기.
        final GameChatListViewItem item = myListItem.get(index);

        //메세지
        viewHolder.getMsg().setText(item.msg_);
        //메세지 색생
        viewHolder.getMsg().setTextColor(item.msgColor_);

        return view;
    }

    /**********************************
     * addItem(Account item) - 리스트에 해당 아이템을 추가
     * item - 추가 할 아이템
     **********************************/
    public void addItem(GameChatListViewItem item){
        myListItem.add(item);
    }


    /**********************************
     * removeItem(GameRoomListViewItem item) - 리스트에 해당 아이템을 삭제 (인덱스로)
     * index - 삭제할 아이템 인덱스 번호
     **********************************/
    public void removeItem(int index){ myListItem.remove(index); }

    /**********************************
     * removeAll() - 리스트 비우기
     **********************************/
    public void removeAll(){ myListItem.clear(); }

    class ViewHolder{
        private View base;
        private TextView textView_msg; //메세지

        ViewHolder(View base){
            this.base = base;
        }

        TextView getMsg() {
            if (textView_msg == null) {
                textView_msg = (TextView)base.findViewById(R.id.textView_msg);
            }
            return textView_msg;
        }
    }

    public void setMyContext(Context contect){
        this.myContext = contect;
    }
}
