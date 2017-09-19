package com.example.kimdongun.paintchat.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.RequestManager;
import com.example.kimdongun.paintchat.Account;
import com.example.kimdongun.paintchat.CustomBitmapPool;
import com.example.kimdongun.paintchat.R;

import java.util.ArrayList;

import jp.wasabeef.glide.transformations.CropCircleTransformation;

/**
 * Created by KimDongun on 2016-10-03.
 */

//친구 목록 리스트뷰 어댑터
public class SocialListViewAdapter extends BaseAdapter {
    public ArrayList<Account> myListItem; //친구 리스트
    private Context myContext = null;
    private LayoutInflater myInflater;
    private RequestManager requestManager;

    public SocialListViewAdapter(Context myContext, RequestManager requestManager){
        super();
        this.myContext = myContext;
        this.requestManager = requestManager;
        this.myListItem = new ArrayList<Account>();
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
            view = myInflater.inflate(R.layout.item_social_listview, parent, false);
            // Holder pattern을 위한 wrapper 초기화 (row를 base 클래스로 지정)
            viewHolder = new ViewHolder(view);
            // row에 viewWrapper object를 tag함 (나중에 row를 재활용 할때 필요)
            view.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder)convertView.getTag();
        }

        // getView() 호출시 인자로 전달된 position을 이용해 현재 사용중인 RowModel의 객체 model 얻기.
        final Account item = myListItem.get(index);

        //프로필 사진
        requestManager.load(item.profileUrl_ + "Thumb.png")
                .thumbnail(0.1f)
                .error(R.mipmap.ic_launcher)
                .centerCrop()
                .bitmapTransform(new CropCircleTransformation(new CustomBitmapPool()))
                .into(viewHolder.getProfile());
        //닉네임
        viewHolder.getNick().setText(item.nick_);

        return view;
    }

    /**********************************
     * addItem(Account item) - 리스트에 해당 아이템을 추가
     * item - 추가 할 아이템
     **********************************/
    public void addItem(Account item){
        myListItem.add(item);
    }

    /**********************************
     * getItem(Account item) - 리스트에 해당 아이템을 가져옴
     * item - 가져 올 아이템
     **********************************/
    public Account getItemById(String id){
        for(int i = 0; i < myListItem.size(); i++){
            Account item = myListItem.get(i);
            if(item.id_.equals(id)){
                return item;
            }
        }
        return null;
    }

    /**********************************
     * getItem(Account item) - 리스트에 해당 아이템을 가져옴
     * item - 가져 올 아이템
     **********************************/
    public Account getItemByNick(String nick){
        for(int i = 0; i < myListItem.size(); i++){
            Account item = myListItem.get(i);
            if(item.nick_.equals(nick)){
                return item;
            }
        }
        return null;
    }

    /**********************************
     * removeItem(Account item) - 리스트에 해당 아이템을 삭제 (오브젝트로)
     **********************************/
    public void removeItem(Account item){
        for(int i = 0; i < myListItem.size(); i++){
            Account temp = myListItem.get(i);
            if(temp.id_.equals(item.id_)){
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
        private ImageView imageView_profile; //유저 프로필
        private TextView textView_nick; //유저 닉네임

        ViewHolder(View base){
            this.base = base;
        }

        ImageView getProfile(){
            if (imageView_profile == null) {
                imageView_profile = (ImageView) base.findViewById(R.id.imageView_profile);
            }
            return imageView_profile;
        }

        TextView getNick() {
            if (textView_nick == null) {
                textView_nick = (TextView)base.findViewById(R.id.textView_nick);
            }
            return textView_nick;
        }
    }

    public void setMyContext(Context contect, RequestManager requestManager){
        this.myContext = contect;
        this.requestManager = requestManager;
    }
}
