package com.example.kimdongun.paintchat.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.item.GameRoomListViewItem;

import java.util.ArrayList;

/**
 * Created by KimDongun on 2016-10-03.
 */

//게임방 리스트뷰 어댑터
public class GameRoomListViewAdapter extends BaseAdapter {
    public ArrayList<GameRoomListViewItem> myListItem; //아이템 리스트
    private Context myContext = null;
    private LayoutInflater myInflater;

    public GameRoomListViewAdapter(Context myContext){
        super();
        this.myContext = myContext;
        this.myListItem = new ArrayList<GameRoomListViewItem>();
    }
    @Override
    public int getCount() {
        return myListItem.size();
    }

    @Override
    public Object getItem(int key) {
        return myListItem.get(key);
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
            view = myInflater.inflate(R.layout.item_game_room_listview, parent, false);
            // Holder pattern을 위한 wrapper 초기화 (row를 base 클래스로 지정)
            viewHolder = new ViewHolder(view);
            // row에 viewWrapper object를 tag함 (나중에 row를 재활용 할때 필요)
            view.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder)convertView.getTag();
        }

        // getView() 호출시 인자로 전달된 position을 이용해 현재 사용중인 RowModel의 객체 model 얻기.
        final GameRoomListViewItem item = myListItem.get(index);

        // convertView(row)내부의 TextView의 text를 객체가 저장하고 있는 String으로  설정.
        //제목
        viewHolder.getName().setText(item.name_);
        //잠금상태
        if(item.isLock_){ //비밀 방
            viewHolder.getLock().setVisibility(View.VISIBLE);
        }else{ //공개 방
            viewHolder.getLock().setVisibility(View.INVISIBLE);
        }
        //현재 인원
        viewHolder.getNum().setText(String.valueOf(item.num_));
        //최대 인원
        viewHolder.getMaxNum().setText(String.valueOf(item.maxNum_));
        //상태
        if(item.isStart_){ //시작 한 방
            viewHolder.getStatus().setText("진\n행");
            viewHolder.getStatus().setBackground(myContext.getResources().getDrawable(R.drawable.room_state_shape2));
        }else{ //대기 방
            viewHolder.getStatus().setText("대\n기");
            viewHolder.getStatus().setBackground(myContext.getResources().getDrawable(R.drawable.room_state_shape));
        }

        return view;
    }

    /**********************************
     * addItem(GameRoomListViewItem item) - 리스트에 해당 아이템을 추가
     * item - 추가 할 아이템
     **********************************/
    public void addItem(GameRoomListViewItem item){
        //GameRoomListViewItem item_ = new GameRoomListViewItem(item.no_, item.name_, item.isLock_, item.num_, item.maxNum_, item.isStart_);
        myListItem.add(item);
    }

    /**********************************
     * getItem(GameRoomListViewItem item) - 리스트에 해당 아이템을 가져옴
     * item - 가져 올 아이템
     **********************************/
    public GameRoomListViewItem getItem(GameRoomListViewItem item){
        for(int i = 0; i < myListItem.size(); i++){
            GameRoomListViewItem temp = myListItem.get(i);
            if(temp.key_ == item.key_){
                return temp;
            }
        }
        return null;
    }

    /**********************************
     * removeItem(GameRoomListViewItem item) - 리스트에 해당 아이템을 삭제 (오브젝트 비교로)
     **********************************/
    public void removeItem(GameRoomListViewItem item){
        for(int i = 0; i < myListItem.size(); i++){
            GameRoomListViewItem temp = myListItem.get(i);
            if(temp.key_ == item.key_){
                myListItem.remove(i);
                return;
            }
        }
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
        private TextView textView_name; //방 제목
        private ImageView imageView_lock; //방 잠금
        private TextView textView_num; //방 현재 인원
        private TextView textView_max_num; //방 최대 인원
        private TextView textView_status; //방 상태

        ViewHolder(View base){
            this.base = base;
        }

        TextView getName() {
            if (textView_name == null) {
                textView_name = (TextView)base.findViewById(R.id.textView_name);
            }
            return textView_name;
        }

        ImageView getLock() {
            if (imageView_lock == null) {
                imageView_lock = (ImageView)base.findViewById(R.id.imageView_lock);
            }
            return imageView_lock;
        }

        TextView getNum() {
            if (textView_num == null) {
                textView_num = (TextView)base.findViewById(R.id.textView_num);
            }
            return textView_num;
        }

        TextView getMaxNum() {
            if (textView_max_num == null) {
                textView_max_num = (TextView)base.findViewById(R.id.textView_max_num);
            }
            return textView_max_num;
        }

        TextView getStatus() {
            if (textView_status == null) {
                textView_status = (TextView)base.findViewById(R.id.textView_status);
            }
            return textView_status;
        }
    }

    public void setMyContext(Context contect){
        this.myContext = contect;
    }
}
