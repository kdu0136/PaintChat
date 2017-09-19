package com.example.kimdongun.paintchat.adapter;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.RequestManager;
import com.example.kimdongun.paintchat.CustomBitmapPool;
import com.example.kimdongun.paintchat.R;
import com.example.kimdongun.paintchat.item.NormalChatListViewItem;

import java.util.ArrayList;

import jp.wasabeef.glide.transformations.CropCircleTransformation;

/**
 * Created by KimDongun on 2016-10-03.
 */

//친구 목록 리스트뷰 어댑터
public class NormalChatListViewAdapter extends BaseAdapter {
    private ArrayList<NormalChatListViewItem> myListItem; //아이템 리스트
    private Context myContext = null;
    private LayoutInflater myInflater;
    private RequestManager requestManager;

    public NormalChatListViewAdapter(Context myContext, RequestManager requestManager){
        super();
        this.myContext = myContext;
        this.requestManager = requestManager;
        this.myListItem = new ArrayList<NormalChatListViewItem>();
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
        LinearLayout layout_other; //다른사람 채팅 레이아웃
        LinearLayout layout_mine; //자신 채팅 레이아웃
        TextView textView_msg; //상대방 채팅
        TextView textView_msg2; //본인 채팅
        ImageView imageView_file; //상대방 파일 이미지
        ImageView imageView_video; //상대방 비디오 화살표 이미지
        ImageView imageView_file2; //본인 파일 이미지
        ImageView imageView_video2; //본인 비디오 화살표 이미지
        TextView textView_chat_date; //채팅 날짜

        // "listview_item" Layout을 inflate하여 convertView 참조 획득.
        if(view == null){
            // 레이아웃을 inflate시켜 새로운 view 생성.
            myInflater = (LayoutInflater)myContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = myInflater.inflate(R.layout.item_normal_chat_listview, parent, false);
            // Holder pattern을 위한 wrapper 초기화 (row를 base 클래스로 지정)
            viewHolder = new ViewHolder(view);
            // row에 viewWrapper object를 tag함 (나중에 row를 재활용 할때 필요)
            view.setTag(viewHolder);
            layout_other = viewHolder.getLayoutOther(); //다른사람 채팅 레이아웃
            layout_mine = viewHolder.getLayoutMine(); //자신 채팅 레이아웃
            textView_msg = viewHolder.getMsg();
            textView_msg2 = viewHolder.getMsg2();
            imageView_file = viewHolder.getFile();
            imageView_video = viewHolder.getVideo();
            imageView_file2 = viewHolder.getFile2();
            imageView_video2 = viewHolder.getVideo2();
            textView_chat_date = viewHolder.getChatDate();
        }else{
            viewHolder = (ViewHolder)convertView.getTag();
            layout_other = viewHolder.getLayoutOther(); //다른사람 채팅 레이아웃
            layout_mine = viewHolder.getLayoutMine(); //자신 채팅 레이아웃
            textView_msg = viewHolder.getMsg();
            textView_msg2 = viewHolder.getMsg2();
            imageView_file = viewHolder.getFile();
            imageView_video = viewHolder.getVideo();
            imageView_file2 = viewHolder.getFile2();
            imageView_video2 = viewHolder.getVideo2();
            textView_chat_date = viewHolder.getChatDate();
        }

        // getView() 호출시 인자로 전달된 position을 이용해 현재 사용중인 RowModel의 객체 model 얻기.
        final NormalChatListViewItem item = myListItem.get(index);
        DisplayMetrics metrics = myContext.getResources().getDisplayMetrics();

        textView_chat_date.setText(item.date_); //채팅 날짜
        if(item.isFirstMsgDate){ //그 날 첫 메세지 된 경우
            textView_chat_date.setVisibility(View.VISIBLE);
        }else{ //날짜 변경 X 경우
            textView_chat_date.setVisibility(View.GONE);
        }

        if(!item.myMsg_){  //다른 사람의 채팅일 경우
            layout_mine.setVisibility(View.GONE);
            layout_other.setVisibility(View.VISIBLE);
            //프로필 사진
            requestManager
                    .load(item.account_.profileUrl_ + "Thumb.png")
                    .error(R.mipmap.ic_launcher)
                    .centerCrop()
                    .bitmapTransform(new CropCircleTransformation(new CustomBitmapPool()))
                    .into(viewHolder.getProfile());
            //닉네임
            viewHolder.getNick().setText(item.account_.nick_);
            if(item.type_.equals("chat")){  //채팅 일 경우
                textView_msg.setVisibility(View.VISIBLE);
                imageView_file.setVisibility(View.GONE);
                imageView_video.setVisibility(View.GONE);
                //채팅 메세지
                viewHolder.getMsg().setText(item.msg_);
            }else { //파일 일 경우
                textView_msg.setVisibility(View.GONE);
                imageView_file.setVisibility(View.VISIBLE);
                if(item.type_.equals("image")){ //이미지 파일 일 경우
                    imageView_video.setVisibility(View.GONE);
                }else{ //동영상 파일 일 경우
                    imageView_video.setVisibility(View.VISIBLE);
                }
                requestManager
                        .load(item.msg_ + "Thumb.png")
                        .override(metrics.widthPixels / 2, metrics.heightPixels/2)
                        .placeholder(R.drawable.prepare_image)
                        .error(R.mipmap.ic_launcher)
                        .into(imageView_file);
                viewHolder.getMsg().setText(item.msg_);
            }
            //채팅 시간
//            viewHolder.getDay().setText(item.date_);
            viewHolder.getDate().setText(item.time_);
            //채팅 읽은 숫자
            viewHolder.getNum().setText(String.valueOf(item.num_));
            if(item.num_ < 1){
                viewHolder.getNum().setVisibility(View.GONE);
            }else{
                viewHolder.getNum().setVisibility(View.VISIBLE);
            }
        }else{  //자신의 채팅일  경우
            layout_mine.setVisibility(View.VISIBLE);
            layout_other.setVisibility(View.GONE);
            if(item.type_.equals("chat")){ //채팅 일 경우
                textView_msg2.setVisibility(View.VISIBLE);
                imageView_file2.setVisibility(View.GONE);
                imageView_video2.setVisibility(View.GONE);

                //채팅 메세지
                viewHolder.getMsg2().setText(item.msg_);
            }else{ //파일 일 경우
                textView_msg2.setVisibility(View.GONE);
                imageView_file2.setVisibility(View.VISIBLE);
                if(item.type_.equals("image")){ //이미지 파일 일 경우
                    imageView_video2.setVisibility(View.GONE);
                }else{ //동영상 파일 일 경우
                    imageView_video2.setVisibility(View.VISIBLE);
                }

                requestManager
                        .load(item.msg_ + "Thumb.png")
                        .override(metrics.widthPixels / 2, metrics.heightPixels/2)
                        .placeholder(R.drawable.prepare_image)
                        .error(R.mipmap.ic_launcher)
                        .into(imageView_file2);
//                viewHolder.getMsg2().setText(item.msg_);
            }
            //채팅 시간
//            viewHolder.getDay2().setText(item.date_);
            viewHolder.getDate2().setText(item.time_);
            //채팅 읽은 숫자
            viewHolder.getNum2().setText(String.valueOf(item.num_));
            if(item.num_ < 1){
                viewHolder.getNum2().setVisibility(View.GONE);
            }else{
                viewHolder.getNum2().setVisibility(View.VISIBLE);
            }
        }


        return view;
    }

    /**********************************
     * getItem(long index) - 리스트에 해당 아이템을 가져옴
     * index - 가져 올 아이템 인덱스
     **********************************/
    public NormalChatListViewItem getItem(long index){
        for(int i = 0; i < myListItem.size(); i++){
            NormalChatListViewItem temp = myListItem.get(i);
            if(temp.dateLong_ == index){
                return temp;
            }
        }
        return null;
    }

    /**********************************
     * addItem(Account item) - 리스트에 해당 아이템을 추가
     * item - 추가 할 아이템
     **********************************/
    public void addItem(NormalChatListViewItem item){
        if(getCount() > 0) { //아이템이 한개 이상일 경우
            NormalChatListViewItem tempItem = myListItem.get(getCount() - 1); //날짜 비교하기 위해 이전 아이템 가져오기
            if(!tempItem.date_.equals(item.date_)) //메세지 날짜가 다른 경우
                item.isFirstMsgDate = true;
        }else{ //아이템이 처음 추가 됬을 떄
            item.isFirstMsgDate = true;
        }
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

    class ViewHolder {
        private View base;
        private ImageView imageView_profile; //유저 프로필
        private TextView textView_nick; //유저 닉네임
        private TextView textView_msg; //채팅 메세지
        private ImageView imageView_file; //파일 썸네일
        private ImageView imageView_video; //동영상 화살표
        private TextView textView_num; //채팅 읽음 숫자
//        private TextView textView_day; //채팅 날짜
        private TextView textView_date; //채팅 날짜
        private TextView textView_msg2; //자신의 채팅 메세지
        private ImageView imageView_file2; //자신의 파일 썸네일
        private ImageView imageView_video2; //자신의 동영상 화살표
//        private TextView textView_day2; //자신의 채팅 날짜
        private TextView textView_date2; //자신의 채팅 날짜
        private TextView textView_num2; //자신의 채팅 읽음 숫자

        private LinearLayout layout_other; //다른사람 채팅 레이아웃
        private LinearLayout layout_mine; //자신 채팅 레이아웃

        private TextView textView_chat_date; //채팅 날짜

        ViewHolder(View base) {
            this.base = base;
        }

        ImageView getProfile() {
            if (imageView_profile == null) {
                imageView_profile = (ImageView) base.findViewById(R.id.imageView_profile);
            }
            return imageView_profile;
        }

        TextView getNick() {
            if (textView_nick == null) {
                textView_nick = (TextView) base.findViewById(R.id.textView_nick);
            }
            return textView_nick;
        }

        TextView getMsg() {
            if (textView_msg == null) {
                textView_msg = (TextView) base.findViewById(R.id.textView_msg);
            }
            return textView_msg;
        }

        ImageView getFile() {
            if (imageView_file == null) {
                imageView_file = (ImageView) base.findViewById(R.id.imageView_file);
            }
            return imageView_file;
        }

        ImageView getVideo() {
            if (imageView_video == null) {
                imageView_video = (ImageView) base.findViewById(R.id.imageView_video);
            }
            return imageView_video;
        }

//        TextView getDay() {
//            if (textView_day == null) {
//                textView_day = (TextView) base.findViewById(R.id.textView_day);
//            }
//            return textView_day;
//        }

        TextView getDate() {
            if (textView_date == null) {
                textView_date = (TextView) base.findViewById(R.id.textView_date);
            }
            return textView_date;
        }

        TextView getNum() {
            if (textView_num == null) {
                textView_num = (TextView) base.findViewById(R.id.textView_num);
            }
            return textView_num;
        }

        TextView getMsg2() {
            if (textView_msg2 == null) {
                textView_msg2 = (TextView) base.findViewById(R.id.textView_msg2);
            }
            return textView_msg2;
        }

        ImageView getFile2() {
            if (imageView_file2 == null) {
                imageView_file2 = (ImageView) base.findViewById(R.id.imageView_file2);
            }
            return imageView_file2;
        }

        ImageView getVideo2() {
            if (imageView_video2 == null) {
                imageView_video2 = (ImageView) base.findViewById(R.id.imageView_video2);
            }
            return imageView_video2;
        }

//        TextView getDay2() {
//            if (textView_day2 == null) {
//                textView_day2 = (TextView) base.findViewById(R.id.textView_day2);
//            }
//            return textView_day2;
//        }

        TextView getDate2() {
            if (textView_date2 == null) {
                textView_date2 = (TextView) base.findViewById(R.id.textView_date2);
            }
            return textView_date2;
        }

        TextView getNum2() {
            if (textView_num2 == null) {
                textView_num2 = (TextView) base.findViewById(R.id.textView_num2);
            }
            return textView_num2;
        }

        LinearLayout getLayoutOther() {
            if (layout_other == null) {
                layout_other = (LinearLayout) base.findViewById(R.id.layout_other);
            }
            return layout_other;
        }

        LinearLayout getLayoutMine() {
            if (layout_mine == null) {
                layout_mine = (LinearLayout) base.findViewById(R.id.layout_mine);
            }
            return layout_mine;
        }

        TextView getChatDate(){
            if (textView_chat_date == null) {
                textView_chat_date = (TextView) base.findViewById(R.id.textView_chat_date);
            }
            return textView_chat_date;
        }
    }
    public void setMyContext(Context contect){
        this.myContext = contect;
    }
}
