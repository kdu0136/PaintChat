package com.example.kimdongun.paintchat.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.item.GameClientListViewItem;

import java.util.ArrayList;

/**
 * Created by KimDongun on 2016-10-03.
 */

//친구 목록 리스트뷰 어댑터
public class GameClientListViewAdapter extends BaseAdapter {
    public ArrayList<GameClientListViewItem> myListItem; //아이템 리스트
    private Context myContext = null;
    private LayoutInflater myInflater;

    public GameClientListViewAdapter(Context myContext){
        super();
        this.myContext = myContext;
        this.myListItem = new ArrayList<GameClientListViewItem>();
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
            view = myInflater.inflate(R.layout.item_game_client_listview, parent, false);
            // Holder pattern을 위한 wrapper 초기화 (row를 base 클래스로 지정)
            viewHolder = new ViewHolder(view);
            // row에 viewWrapper object를 tag함 (나중에 row를 재활용 할때 필요)
            view.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder)convertView.getTag();
        }

        // getView() 호출시 인자로 전달된 position을 이용해 현재 사용중인 RowModel의 객체 model 얻기.
        final GameClientListViewItem item = myListItem.get(index);

        //배경
        if(item.isHost_)
            viewHolder.setBack(myContext.getResources().getDrawable(R.drawable.game_host_shape));
        else
            viewHolder.setBack(myContext.getResources().getDrawable(R.drawable.game_client_shape));

        //Ready
        if(item.isReady_)
            viewHolder.visibleR(View.VISIBLE);
        else
            viewHolder.visibleR(View.INVISIBLE);

        //닉네임
        viewHolder.getNick().setText(item.nick_);

        return view;
    }

    /**********************************
     * addItem(Account item) - 리스트에 해당 아이템을 추가
     * item - 추가 할 아이템
     **********************************/
    public void addItem(GameClientListViewItem item){
        myListItem.add(item);
    }

    public GameClientListViewItem getItem(String id){
        for(int i = 0; i < getCount(); i++){
            if(myListItem.get(i).id_.equals(id)){
                return myListItem.get(i);
            }
        }
        return null;
    }

    /**********************************
     * removeItem(String id) - 리스트에 해당 아이템을 삭제 (인덱스로)
     * id - 삭제할 아이템 아이디
     **********************************/
    public void removeItem(String id){
        for(int i = 0; i < getCount(); i++){
            if(myListItem.get(i).id_.equals(id)){
                myListItem.remove(i);
                return;
            }
        }
    }

    /**********************************
     * removeAll() - 리스트 비우기
     **********************************/
    public void removeAll(){ myListItem.clear(); }

    class ViewHolder{
        private View base;
        private LinearLayout layout_back; //배경
        private TextView textView_ready; //레디
        private TextView textView_nick; //닉네임

        ViewHolder(View base){
            this.base = base;
        }

        void setBack(Drawable back) {
            if (layout_back == null) {
                layout_back = (LinearLayout) base.findViewById(R.id.layout_back);
            }
            layout_back.setBackground(back);
        }

        void visibleR(int visible) {
            if (textView_ready == null) {
                textView_ready = (TextView)base.findViewById(R.id.textView_ready);
            }
            textView_ready.setVisibility(visible);
        }

        TextView getNick() {
            if (textView_nick == null) {
                textView_nick = (TextView)base.findViewById(R.id.textView_nick);
            }
            return textView_nick;
        }
    }

    public void setMyContext(Context contect){
        this.myContext = contect;
    }
}
