package com.example.kimdongun.paintchat.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.RequestManager;
import com.example.kimdongun.paintchat.CustomBitmapPool;
import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.item.InviteChatListViewItem;

import java.util.ArrayList;

import jp.wasabeef.glide.transformations.CropCircleTransformation;

/**
 * Created by KimDongun on 2016-10-03.
 */

//채팅에 친구 초대 리스트뷰 어댑터
public class InviteChatListViewAdapter extends BaseAdapter {
    public ArrayList<InviteChatListViewItem> myListItem; //아이템 리스트
    private Context myContext = null;
    private LayoutInflater myInflater;
    private RequestManager requestManager;

    public InviteChatListViewAdapter(Context myContext, RequestManager requestManager){
        super();
        this.myContext = myContext;
        this.requestManager = requestManager;
        this.myListItem = new ArrayList<InviteChatListViewItem>();
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
        CheckBox checkBox; //선택 체크박스

        // "listview_item" Layout을 inflate하여 convertView 참조 획득.
        if(view == null){
            // 레이아웃을 inflate시켜 새로운 view 생성.
            myInflater = (LayoutInflater)myContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = myInflater.inflate(R.layout.item_invite_chat_listview, parent, false);
            // Holder pattern을 위한 wrapper 초기화 (row를 base 클래스로 지정)
            viewHolder = new ViewHolder(view);
            // row에 viewWrapper object를 tag함 (나중에 row를 재활용 할때 필요)
            view.setTag(viewHolder);
            checkBox = viewHolder.getCheckBox();

            CheckBox.OnCheckedChangeListener checkedChangeListener = new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    Integer index = (Integer)buttonView.getTag();
                    InviteChatListViewItem item = myListItem.get(index);

                    item.isChecked_ = isChecked;
                }
            };
            checkBox.setOnCheckedChangeListener(checkedChangeListener);
        }else{
            viewHolder = (ViewHolder)convertView.getTag();
            checkBox = viewHolder.getCheckBox();
        }

        // getView() 호출시 인자로 전달된 position을 이용해 현재 사용중인 RowModel의 객체 model 얻기.
        final InviteChatListViewItem item = myListItem.get(index);

        //프로필 사진
        requestManager
                .load(item.account_.profileUrl_ + "Thumb.png")
                .thumbnail(0.1f)
                .error(R.mipmap.ic_launcher)
                .centerCrop()
                .bitmapTransform(new CropCircleTransformation(new CustomBitmapPool()))
                .into(viewHolder.getProfile());
//        viewHolder.getProfile().setImageDrawable(item.account_.profile_);
        //닉네임
        viewHolder.getNick().setText(item.account_.nick_);
        //체크 박스
        checkBox.setTag(new Integer(index));
        checkBox.setChecked(item.isChecked_);

        return view;
    }

    /**********************************
     * addItem(InviteChatListViewItem item) - 리스트에 해당 아이템을 추가
     * item - 추가 할 아이템
     **********************************/
    public void addItem(InviteChatListViewItem item){
        myListItem.add(item);
    }

    /**********************************
     * removeItem(Account item) - 리스트에 해당 아이템을 삭제 (오브젝트로)
     * index - 삭제할 아이템 인덱스 번호
     **********************************/
    public void removeItem(InviteChatListViewItem item){
        for(int i = 0; i < myListItem.size(); i++){
            InviteChatListViewItem temp = myListItem.get(i);
            if(temp.account_.id_.equals(item.account_.id_)){
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

    /**********************************
     * getCheckedItem() - 체크 된 아이템을 가져오기
     **********************************/
    public ArrayList<InviteChatListViewItem> getCheckedItem(){
        ArrayList<InviteChatListViewItem> arrayList = new ArrayList<InviteChatListViewItem>();
        for(int i = 0; i < myListItem.size(); i++){
            InviteChatListViewItem item = myListItem.get(i);
            if(item.isChecked_)
                arrayList.add(item);
        }

        return arrayList;
    }

    class ViewHolder{
        private View base;
        private ImageView imageView_profile; //유저 프로필
        private TextView textView_nick; //유저 닉네임
        private CheckBox checkbox_select; //선택 체크 박스

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

        CheckBox getCheckBox(){
            if(checkbox_select == null){
                checkbox_select = (CheckBox)base.findViewById(R.id.checkbox_select);
            }
            return checkbox_select;
        }
    }

    public void setMyContext(Context contect){
        this.myContext = contect;
    }
}
