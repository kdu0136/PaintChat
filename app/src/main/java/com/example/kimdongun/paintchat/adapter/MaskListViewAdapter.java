package com.example.kimdongun.paintchat.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bumptech.glide.RequestManager;
import com.example.kimdongun.paintchat.R;

import java.util.ArrayList;

/**
 * Created by KimDongun on 2016-10-03.
 */

//마스크 리스트뷰 어댑터
public class MaskListViewAdapter extends BaseAdapter {
    private static String MASK_PATH = "http://211.110.229.53/mask_image/%s";
    public String[] defulatMask = {"no_mask.png", "spiderman.png", "ironman.png", "betman.png", "jigsaw.png"};
    private ArrayList<String> myListItem; //아이템 리스트
    private Context myContext = null;
    private LayoutInflater myInflater;
    private RequestManager requestManager;

    public MaskListViewAdapter(Context myContext, RequestManager requestManager){
        super();
        this.myContext = myContext;
        this.myListItem = new ArrayList<String>();
        //기본 마스크 추가
        for(int i = 0; i < defulatMask.length; i++)
            addItem(defulatMask[i]);

        this.requestManager = requestManager;
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
            view = myInflater.inflate(R.layout.item_mask_listview, parent, false);
            // Holder pattern을 위한 wrapper 초기화 (row를 base 클래스로 지정)
            viewHolder = new ViewHolder(view);
            // row에 viewWrapper object를 tag함 (나중에 row를 재활용 할때 필요)
            view.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder)convertView.getTag();
        }

        // getView() 호출시 인자로 전달된 position을 이용해 현재 사용중인 RowModel의 객체 model 얻기.
        final String imgStr = myListItem.get(index);

        //마스크 그림
        requestManager
                .load(String.format(MASK_PATH, imgStr))
                .placeholder(R.drawable.prepare_image)
                .into(viewHolder.getMask());

        return view;
    }

    /**********************************
     * addItem(Account item) - 리스트에 해당 아이템을 추가
     * item - 추가 할 아이템
     **********************************/
    public void addItem(String item){
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
        private ImageView imageView_mask; //마스크 그림

        ViewHolder(View base){
            this.base = base;
        }

        ImageView getMask() {
            if (imageView_mask == null) {
                imageView_mask = (ImageView) base.findViewById(R.id.imageView_mask);
            }
            return imageView_mask;
        }
    }

    public void setMyContext(Context contect){
        this.myContext = contect;
    }
}
