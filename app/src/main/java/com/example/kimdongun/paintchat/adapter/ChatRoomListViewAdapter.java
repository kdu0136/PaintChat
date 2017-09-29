package com.example.kimdongun.paintchat.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.item.ChatRoomListViewItem;
import com.stfalcon.multiimageview.MultiImageView;

import java.util.ArrayList;

/**
 * Created by KimDongun on 2016-10-03.
 */

//채팅 목록 리스트뷰 어댑터
public class ChatRoomListViewAdapter extends BaseAdapter {
    public ArrayList<ChatRoomListViewItem> myListItem; //아이템 리스트
    private Context myContext = null;
    private LayoutInflater myInflater;
    private RequestManager requestManager;

    public ChatRoomListViewAdapter(Context myContext, RequestManager requestManager){
        super();
        this.myContext = myContext;
        this.requestManager = requestManager;
        this.myListItem = new ArrayList<ChatRoomListViewItem>();
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
        final MultiImageView multiImageView; //채팅방 프로필 이미지 뷰

        // "listview_item" Layout을 inflate하여 convertView 참조 획득.
        if(view == null){
            // 레이아웃을 inflate시켜 새로운 view 생성.
            myInflater = (LayoutInflater)myContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = myInflater.inflate(R.layout.item_chat_room_listview, parent, false);
            // Holder pattern을 위한 wrapper 초기화 (row를 base 클래스로 지정)
            viewHolder = new ViewHolder(view);
            // row에 viewWrapper object를 tag함 (나중에 row를 재활용 할때 필요)
            view.setTag(viewHolder);
            multiImageView = viewHolder.getProfile();
        }else{
            viewHolder = (ViewHolder)convertView.getTag();
            multiImageView = viewHolder.getProfile();
        }

        // getView() 호출시 인자로 전달된 position을 이용해 현재 사용중인 RowModel의 객체 model 얻기.
        final ChatRoomListViewItem item = myListItem.get(index);

        //프로필 사진
//        multiImageView.setTag(new Integer(index));
        multiImageView.setShape(MultiImageView.Shape.CIRCLE);
        multiImageView.clear();
        for(int i = 0; i < item.accountProfileUrl_.size(); i++){
            requestManager.load(item.accountProfileUrl_.get(i) + "Thumb.png")
                    .asBitmap()
                    .error(R.mipmap.ic_launcher)
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                            multiImageView.addImage(resource);
                        }
                    });
            if(i == 3) break;
        }

        //닉네임
        viewHolder.getNick().setText(item.accountNick_);
        //채팅 참여 유저 수
        if(item.userNum_ > 2) { //채팅방에 유저가 3명 이상일 때
            viewHolder.getUserNum().setVisibility(View.VISIBLE);
            viewHolder.getUserNum().setText(String.valueOf(item.userNum_));
        }else{ //채팅방에 유저가 3명 미만일 때 (본인 포함)
            viewHolder.getUserNum().setVisibility(View.INVISIBLE);
        }
        //채팅 메세지
        if(item.type_.equals("chat")){ //채팅 메세지 일 경우
            viewHolder.getMsg().setText(item.msg_);
        }else if(item.type_.equals("image")){ //이미지 파일 일 경우
            viewHolder.getMsg().setText("사진");
        }else if(item.type_.equals("video")){ //비디오 파일 일 경우
            viewHolder.getMsg().setText("동영상");
        }else if(item.type_.equals("invite")){ //게임 초대 일 경우
            viewHolder.getMsg().setText("초대 메세지");
        }
//        Uri uri = Uri.parse(item.msg_);
//        if (uri.getScheme() == null || uri.getScheme().isEmpty()) {
//            // safe text
//            viewHolder.getMsg().setText(item.msg_);
//        } else {
//            // has url
//            viewHolder.getMsg().setText("파일");
//        }
        //채팅 시간
        viewHolder.getDate().setText(item.time_);
        //읽지 않은 메세지 숫자
        if(item.msgNum_ > 0){ //읽지 않은 메세지가 1개 이상일 경우
            viewHolder.getMsgNum().setVisibility(View.VISIBLE);
            viewHolder.getMsgNum().setText(String.valueOf(item.msgNum_));
        }else{ //읽지 않은 메세지가 없을 경우
            viewHolder.getMsgNum().setVisibility(View.INVISIBLE);
        }

        return view;
    }

    /**********************************
     * getItem(String key) - 리스트에 해당 아이템을 가져옴
     * key - 가져 올 방 키값
     **********************************/
    public ChatRoomListViewItem getItem(String key){
        for(int i = 0; i < myListItem.size(); i++){
            ChatRoomListViewItem temp = myListItem.get(i);
            if(temp.roomKey_.equals(key)){
                return temp;
            }
        }
        return null;
    }

    /**********************************
     * addAll(ArrayList<ChatRoomListViewItem> listItem) - 리스트에 해당 아이템을 모두 추가
     **********************************/
    public void addAll(ArrayList<ChatRoomListViewItem> listItem){
        myListItem.addAll(listItem);
    }


    /**********************************
     * addItem(ChatRoomListViewItem item) - 리스트에 해당 아이템을 추가
     * item - 추가 할 아이템
     **********************************/
    public void addItem(ChatRoomListViewItem item){
        myListItem.add(item);
    }

    /**********************************
     * addItem(ChatRoomListViewItem item) - 리스트에 해당 아이템을 추가 (맨 위에)
     * item - 추가 할 아이템
     **********************************/
    public void addTopItem(ChatRoomListViewItem item){
        myListItem.add(0, item);
    }


    /**********************************
     * removeItem(int index) - 리스트에 해당 아이템을 삭제 (인덱스로)
     * index - 삭제할 아이템 인덱스 번호
     **********************************/
    public void removeItem(int index){ myListItem.remove(index); }

    /**********************************
     * removeItem(String roomKey) - 리스트에 해당 아이템을 삭제 (키값으로)
     * roomKey - 삭제할 방 키값값
     *********************************/
    public void removeItem(String roomKey){
        for(int i = 0; i < myListItem.size(); i++){
            ChatRoomListViewItem item = myListItem.get(i);
            if(item.roomKey_.equals(roomKey)){
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
        private MultiImageView imageView_profile; //유저들 프로필
        private TextView textView_nick; //유저들 닉네임
        private TextView textView_user_num; //유저들 수
        private TextView textView_msg; //채팅 메세지
        private TextView textView_msg_num; //안 읽은 채팅 메세지 수
        private TextView textView_date; //채팅 날짜

        ViewHolder(View base){
            this.base = base;
        }

        MultiImageView getProfile(){
            if (imageView_profile == null) {
                imageView_profile = (MultiImageView) base.findViewById(R.id.imageView_profile);
            }
            return imageView_profile;
        }

        TextView getNick(){
            if (textView_nick == null) {
                textView_nick = (TextView) base.findViewById(R.id.textView_nick);
            }
            return textView_nick;
        }

        TextView getUserNum(){
            if (textView_user_num == null) {
                textView_user_num = (TextView) base.findViewById(R.id.textView_user_num);
            }
            return textView_user_num;
        }

        TextView getMsg(){
            if (textView_msg == null) {
                textView_msg = (TextView) base.findViewById(R.id.textView_msg);
            }
            return textView_msg;
        }

        TextView getMsgNum(){
            if (textView_msg_num == null) {
                textView_msg_num = (TextView) base.findViewById(R.id.textView_msg_num);
            }
            return textView_msg_num;
        }

        TextView getDate(){
            if (textView_date == null) {
                textView_date = (TextView) base.findViewById(R.id.textView_date);
            }
            return textView_date;
        }
    }

    public void setMyContext(Context context, RequestManager requestManager){
        this.myContext = context;
        this.requestManager = requestManager;
    }
}
